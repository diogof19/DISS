package com.datamining;

import com.analysis.metrics.ClassMetrics;
import com.analysis.metrics.FileMetrics;
import com.analysis.metrics.MethodMetrics;
import com.core.Pair;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiStatement;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

public class Utils {
    /**
     * Extracts the method name from the refactoring description
     * @param description refactoring description from RefactoringMiner or refDiff
     * @return method name
     */
    public static String getMethodName(String description) {
        //Example description from RefactoringMiner:
        //Extract Method	private nextPosEmpty(Action ACTION) : boolean extracted from public doAction(Action ACTION) : void in class com.lpoo_2021_g61.Controller.CanvasController
//        return description.split("\\(\\)")[0].replace("public ", "")
//                .replace("private ", "").replace("protected ", "")
//                .replace("static ", "").trim();

        return description.split("extracted from")[1].split("in class")[0].trim().split("\\(")[0]
                .replace("public ", "").replace("private ", "")
                .replace("protected ", "").replace("static ", "").trim();
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

    /**
     * Loads a PsiJavaFile from a file path
     * @param filePath file path
     * @param project project
     * @return PsiJavaFile
     */
    public static PsiJavaFile loadFile(String filePath, Project project) {
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(filePath));
        if(vf != null){
            return (PsiJavaFile) com.intellij.psi.util.PsiUtilBase.getPsiFile(project, vf);
        } else {
            System.out.println("Virtual file not found");
        }

        return null;
    }

    /**
     * Extracts the class and method metrics from a PsiJavaFile
     * @param file PsiJavaFile
     * @param methodName method name
     * @param className class name
     * @return pair with class and method metrics
     */
    public static Pair<ClassMetrics, MethodMetrics> getMethodMetricsFromFile(PsiJavaFile file, String methodName, String className) {
        FileMetrics fileMetrics;
        try {
            fileMetrics = new FileMetrics(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("Class: " + className + " - " + methodName);

        ClassMetrics classMetrics = fileMetrics.classMetrics.stream()
                .filter(c -> c.className.equals(className))
                .findFirst()
                .orElse(null);

        assert classMetrics != null;

        MethodMetrics methodMetrics = classMetrics.methodMetrics.stream()
                .filter(m -> m.methodName.equals(methodName))
                .findFirst()
                .orElse(null);

        if(methodMetrics == null){
            System.out.println("Method not found: " + methodName);
            System.out.println("Class: " + className);
            System.out.println("Methods from class metrics: ");
            for(MethodMetrics m : classMetrics.methodMetrics){
                System.out.println(m.methodName);
                for (PsiStatement statement : m.method.getBody().getStatements()) {
                    System.out.println(statement.getText());
                }
            }
            throw new RuntimeException("Method not found: " + methodName);
        }
        else {
            System.out.println("Method: " + methodName);
            for (PsiStatement statement : file.getClasses()[0].findMethodsByName(methodName, false)[0].getBody().getStatements()) {
                System.out.println(statement.getText());
            }
        }

        return new Pair<>(classMetrics, methodMetrics);

    }

    //TODO: delete this after testing
    /**
     * Saves the metrics to a file (can be used to save before and after changes in the same row)
     * @param writer file writer
     * @param refactoringInfo refactoring info
     * @param isBefore true if the metrics are from the before file (or only file), false otherwise
     */
    public static void saveMetricsToFile(BufferedWriter writer, RefactoringInfo refactoringInfo, boolean isBefore) throws IOException {
        Pair<ClassMetrics, MethodMetrics> metrics;
        if(isBefore){
            metrics = getMethodMetricsFromFile(refactoringInfo.getBeforeFile(), refactoringInfo.getMethodName(),
                    refactoringInfo.getClassName());
        }
        else{
            metrics = getMethodMetricsFromFile(refactoringInfo.getAfterFile(), refactoringInfo.getMethodName(),
                    refactoringInfo.getClassName());
        }

        MethodMetrics methodMetrics = metrics.getSecond();

        int totalLines = methodMetrics.numberLinesOfCode + methodMetrics.numberComments +
                methodMetrics.numberBlankLines;

        writer.write(
                refactoringInfo.getAuthor().toString() + "," +
                    methodMetrics.numberLinesOfCode + "," +
                    methodMetrics.numberComments + "," +
                    methodMetrics.numberBlankLines + "," +
                    totalLines + "," +
                    methodMetrics.numParameters + "," +
                    methodMetrics.numberOfStatements + "," +
                    methodMetrics.halsteadLength + "," +
                    methodMetrics.halsteadVocabulary + "," +
                    methodMetrics.halsteadVolume + "," +
                    methodMetrics.halsteadDifficulty + "," +
                    methodMetrics.halsteadEffort + "," +
                    methodMetrics.halsteadLevel + "," +
                    methodMetrics.halsteadTime + "," +
                    methodMetrics.halsteadBugsDelivered + "," +
                    methodMetrics.halsteadMaintainability + "," +
                    methodMetrics.complexityOfMethod + "," +
                    methodMetrics.cognitiveComplexity + "," +
                    methodMetrics.lackOfCohesionInMethod
        );

        if (!isBefore || refactoringInfo.get_id() == null){
            writer.write("\n");
        }

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
        Notification error = new Notification(
                groupID,
                title,
                content,
                type
        );

        error.notify(project);
    }
}
