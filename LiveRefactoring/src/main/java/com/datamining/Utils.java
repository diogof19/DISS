package com.datamining;

import com.analysis.metrics.ClassMetrics;
import com.analysis.metrics.FileMetrics;
import com.analysis.metrics.MethodMetrics;
import com.core.Pair;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;

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
//        return description.split("\\(\\)")[0].replace("public ", "")
//                .replace("private ", "").replace("protected ", "")
//                .trim().replace("static ", "").trim();
        return description.split("\\(")[0].replace("public ", "")
        .replace("private ", "").replace("protected ", "")
        .trim().replace("static ", "").replace("Extract Method", "").trim();
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

        ClassMetrics classMetrics = fileMetrics.classMetrics.stream()
                .filter(c -> c.className.equals(className))
                .findFirst()
                .orElse(null);

        assert classMetrics != null;
        MethodMetrics methodMetrics = classMetrics.methodMetrics.stream()
                .filter(m -> m.methodName.equals(methodName))
                .findFirst()
                .orElse(null);

        return new Pair<>(classMetrics, methodMetrics);

    }

    /**
     * Saves the metrics to a file (can be used to save before and after changes in the same row)
     * @param writer file writer
     * @param refactoringInfo refactoring info
     * @param isBefore true if the metrics are from the before file (or only file), false otherwise
     * @throws IOException
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

        if (isBefore){
            //So that it works when it's only one file and not before and after per row
            if (refactoringInfo.get_id() != null)
                writer.write(
                        "\"" + refactoringInfo.get_id() + "\","
                );
        }

        writer.write(
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

        if (!isBefore){
            writer.write("\n");
        }

    }
}
