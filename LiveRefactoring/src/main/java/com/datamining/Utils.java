package com.datamining;

import com.analysis.metrics.ClassMetrics;
import com.analysis.metrics.FileMetrics;
import com.analysis.metrics.MethodMetrics;
import com.core.Pair;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiUtilBase;
import com.utils.importantValues.Values;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

enum RefType {
    EXTRACT_METHOD,
    EXTRACT_CLASS
}

public class Utils {
    public static final String FILE_COPIES_DIR = Values.dataFolder;
    public static int counter = 0;

    /**
     * Extracts the method name from the refactoring description
     * @param description refactoring description from RefactoringMiner or refDiff
     * @return method name
     */
    public static String getMethodName(String description) {
        //protected checkLatest(dd DependencyDescriptor, newModuleFound ResolvedModuleRevision, data ResolveData) : ResolvedModuleRevision
        return description.split("extracted from")[1].split("in class")[0].trim().split("\\)")[0]
                .replace("public ", "").replace("private ", "")
                .replace("protected ", "").replace("static ", "").trim().concat(")");
    }

    /**
     * Extracts the full and short class names from the refactoring description
     * @param description refactoring description from RefactoringMiner or refDiff
     * @return pair with full class name and short class name
     */
    public static Pair<String, String> getClassName(String description){
        String fullClass = description.split("in class")[1].trim();
        String[] parts = fullClass.split("\\.");
        final String className = parts[parts.length - 1];
        return new Pair<>(fullClass, className);
    }

    public static Pair<String, String> getOldClass(String description){
        //Extract Class	org.apache.ivy.core.cache.CacheManager from class org.apache.ivy.Ivy

        String oldClass = description.split("from class")[1].trim();
        String[] oldClassParts = oldClass.split("\\.");
        String oldClassName = oldClassParts[oldClassParts.length - 1];

        return new Pair<>(oldClass, oldClassName);
    }

    /**
     * Loads a PsiJavaFile from a file path
     * @param filePath file path
     * @param project project
     * @return PsiJavaFile
     */
    public static PsiJavaFile loadFile(String filePath, String folder, Project project) {
        File tmpDir = new File(FILE_COPIES_DIR + folder + "/");
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }

        String newFilePath = FILE_COPIES_DIR + folder + "/" + counter + ".java";
        counter++;
        try {
            Files.copy(Path.of(filePath), Path.of(newFilePath));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(newFilePath);

        if(vf != null){
            return ApplicationManager.getApplication().runReadAction(
                    (Computable<PsiJavaFile>) () -> (PsiJavaFile) PsiUtilBase.getPsiFile(project, vf)
            );
        }

        return null;
    }

    /**
     * Extracts the method metrics from a PsiJavaFile
     * @param file PsiJavaFile
     * @param method method name and arguments
     * @param className class name
     * @return method metrics
     */
    public static MethodMetrics getMethodMetricsFromFile(PsiJavaFile file, String method, String className) {
        return ApplicationManager.getApplication().runReadAction((Computable<MethodMetrics>) () -> {
            try {
                ClassMetrics classMetrics = getClassMetricsFromFile(file, className);
                if (classMetrics == null) {
                    return null;
                }

                String methodName = method.split("\\(")[0];
                ArrayList<String> parameters = new ArrayList<>();

                if (!method.contains("()")) { // if the method has parameters
                    String[] parts = method.split("\\(")[1].split("\\)")[0].split(",");
                    for(String p : parts) {
                        parameters.add(p.trim());
                    }
                }

                MethodMetrics methodMetrics = classMetrics.methodMetrics.stream()
                        .filter(m -> m.methodName.equals(methodName))
                        .filter(m -> m.method.getParameterList().getParameters().length == parameters.size())
                        .filter(m -> {
                            for (int i = 0; i < parameters.size(); i++) {
                                String name = m.method.getParameterList().getParameters()[i].getName();
                                if (!name.equals(parameters.get(i).split(" ")[1]) && !name.equals(parameters.get(i).split(" ")[0])) {
                                    return false;
                                }
                            }
                            return true;
                        })
                        .findFirst()
                        .orElse(null);

                return methodMetrics;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public static ClassMetrics getClassMetricsFromFile(PsiJavaFile file, String className) {
        return ApplicationManager.getApplication().runReadAction((Computable<ClassMetrics>) () -> {
            try {
                FileMetrics fileMetrics =  new FileMetrics(file);

                ClassMetrics classMetrics = fileMetrics.classMetrics.stream()
                        .filter(c -> c.className.equals(className))
                        .findFirst()
                        .orElse(null);

                return classMetrics;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Creates a balloon type notification
     * @param project project
     * @param title title of the notification
     * @param content content of the notification
     * @param type type of the notification
     */
    public static void popup(Project project, String title, String content, NotificationType type){
        //For some reason, I need to define a groupID before creating the notification
        String groupID = "LiveRefactor";
        Notification notification = new Notification(
                groupID,
                title,
                content,
                type
        );

        notification.notify(project);
    }
}
