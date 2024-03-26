package com.datamining;

import com.analysis.metrics.ClassMetrics;
import com.analysis.metrics.FileMetrics;
import com.analysis.metrics.MethodMetrics;
import com.core.Pair;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;

import java.io.*;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

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

        return description.split("from")[1].split("in class")[0].trim().split("\\(")[0]
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
     * Writes the headers for the metrics file
     * @param writer file writer
     * @param beforeAndAfter true if the file will contain before and after metrics
     * @throws IOException if an I/O error occurs
     */
    public static void writeMetricsFileHeader(BufferedWriter writer, boolean beforeAndAfter) throws IOException {
        writer.write(
                "author," + "numberLinesOfCodeBef," + "numberCommentsBef," + "numberBlankLinesBef," +
                        "totalLinesBef," + "numParametersBef," + "numStatementsBef," + "halsteadLengthBef," +
                        "halsteadVocabularyBef," + "halsteadVolumeBef," + "halsteadDifficultyBef," +
                        "halsteadEffortBef," + "halsteadLevelBef," + "halsteadTimeBef," + "halsteadBugsDeliveredBef,"
                        + "halsteadMaintainabilityBef," + "cyclomaticComplexityBef," + "cognitiveComplexityBef," +
                        "lackOfCohesionInMethodBef"
        );

        if(beforeAndAfter){
            writer.write("numberLinesOfCodeAft," + "numberCommentsAft," + "numberBlankLinesAft," + "totalLinesAft,"
                    + "numParametersAft," + "numStatementsAft," + "halsteadLengthAft," + "halsteadVocabularyAft," +
                    "halsteadVolumeAft," + "halsteadDifficultyAft," + "halsteadEffortAft," + "halsteadLevelAft," +
                    "halsteadTimeAft," + "halsteadBugsDeliveredAft," + "halsteadMaintainabilityAft," +
                    "cyclomaticComplexityAft," + "cognitiveComplexityAft," + "lackOfCohesionInMethodAft");
        }

        writer.write("\n");
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

        writer.write(
                refactoringInfo.getAuthor() + "," +
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
     * Extracts a file from the jar file to the tmp folder
     * @param fileName the name of the file to be extracted
     * @throws IOException if there is a problem extracting the file
     * @return the path of the extracted file
     */
    public static String extractFile(String fileName) throws IOException {
        File file = new File("tmp");
        if(!file.exists()){
            file.mkdir();
        }

        URL url = PredictionModel.class.getResource("/" + fileName);

        InputStream inputStream = url.openStream();
        OutputStream outputStream = new FileOutputStream("tmp/" + fileName);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        inputStream.close();
        outputStream.close();

        return Paths.get("tmp/" + fileName).toAbsolutePath().toString();
    }

    public static Set<AuthorInfo> getAuthors() throws IOException, ClassNotFoundException {
        File tmpFolder = new File("tmp");
        if(!tmpFolder.exists()) {
            tmpFolder.mkdir();
        }

        File authorsFile = new File("tmp/authors.txt");
        if(!authorsFile.exists()){
            authorsFile.createNewFile();
            return new HashSet<>();
        }

        return AuthorInfo.readAuthorInfoSet(authorsFile.getAbsolutePath());
    }
}
