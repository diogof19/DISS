package com.datamining;

import com.core.Pair;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;

import java.io.File;

public class Utils {
    /**
     * Extracts the method name from the refactoring description
     * @param description refactoring description from RefactoringMiner or refDiff
     * @return method name
     */
    public static String getMethodName(String description) {
        return description.split("\\(\\)")[0].replace("public ", "")
                .replace("private ", "").replace("protected ", "")
                .trim().replace("static ", "").trim();
    }

    /**
     * Extracts the full and short class names from the refactoring description
     * @param description refactoring description from RefactoringMiner or refDiff
     * @return pair with full class name and short class name
     */
    public static Pair<String, String> getClassName(String description){
        String fullClass = description.split("from")[1].split("in class")[1].trim();
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
        VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(new File(filePath));
        if(vf != null){
            System.out.println("Virtual file found");
            return (PsiJavaFile) com.intellij.psi.util.PsiUtilBase.getPsiFile(project, vf);
        } else {
            System.out.println("Virtual file not found");
        }

        return null;
    }
}
