package com.datamining;

import com.analysis.metrics.ClassMetrics;
import com.analysis.metrics.FileMetrics;
import com.analysis.metrics.MethodMetrics;
import com.core.Pair;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import static com.datamining.Utils.getClassName;
import static com.datamining.Utils.getMethodName;
import static com.mongodb.client.model.Accumulators.first;
import static com.mongodb.client.model.Accumulators.push;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static java.util.Arrays.asList;

public class DataCollection extends AnAction {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "smartshark_2_2";
    private static final String COLLECTION_NAME = "refactoring";

    private static final String EXTRACTED_METRICS_FILE_PATH =
            "C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\LiveRefactoring\\src\\main\\java\\com\\datamining\\data\\extracted_metrics.csv";

    private MongoClient mongoClient;
    private Project project;

    public DataCollection() {

    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        this.project = anActionEvent.getProject();

        try {
            this.mongoClient = MongoClients.create(CONNECTION_STRING);
        } catch (Exception e) {
            throw new RuntimeException("Error connecting to the database", e);
        }

        HashSet<Document> refactoringData = getRefactoringData();

        System.out.println("Data extracted: " + refactoringData.size());

        try {
            extractMetrics(refactoringData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private HashSet<Document> getRefactoringData() {
        MongoCollection<Document> collection = this.mongoClient.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);

        //TODO: Remove limit
        List<Bson> pipeline = asList(
                match(eq("type", "extract_method")),
                project(fields(include("_id", "commit_id", "type", "description"))),
                limit(5),
                lookup("commit", "commit_id", "_id", "commit"),
                unwind("$commit"),
                lookup("file_action", "commit._id", "commit_id", "file_actions"),
                unwind("$file_actions"),
                lookup("file", "file_actions.file_id", "_id", "file"),
                unwind("$file"),
                group("$_id",
                        first("type", "$type"),
                        first("description", "$description"),
                        first("revision_hash", "$commit.revision_hash"),
                        first("parent_revision_hash", "$file_actions.parent_revision_hash"),
                        push("files", "$file.path")
                )
        );

        return collection.aggregate(pipeline).into(new HashSet<>());
    }

    private void extractMetrics(HashSet<Document> documents) throws IOException {
        File metricsFile = new File(EXTRACTED_METRICS_FILE_PATH);
        FileWriter writer = new FileWriter(metricsFile, false);
        BufferedWriter bufferedWriter = new BufferedWriter(writer);

        bufferedWriter.write(
                "id," +
                "numberLinesOfCodeBef," +
                "numberCommentsBef," +
                "numberBlankLinesBef," +
                "totalLinesBef," +
                "numParametersBef," +
                "numStatementsBef," +
                "halsteadLengthBef," +
                "halsteadVocabularyBef," +
                "halsteadVolumeBef," +
                "halsteadDifficultyBef," +
                "halsteadEffortBef," +
                "halsteadLevelBef," +
                "halsteadTimeBef," +
                "halsteadBugsDeliveredBef," +
                "halsteadMaintainabilityBef," +
                "cyclomaticComplexityBef," +
                "cognitiveComplexityBef," +
                "lackOfCohesionInMethodBef," +
                "numberLinesOfCodeAft," +
                "numberCommentsAft," +
                "numberBlankLinesAft," +
                "totalLinesAft," +
                "numParametersAft," +
                "numStatementsAft," +
                "halsteadLengthAft," +
                "halsteadVocabularyAft," +
                "halsteadVolumeAft," +
                "halsteadDifficultyAft," +
                "halsteadEffortAft," +
                "halsteadLevelAft," +
                "halsteadTimeAft," +
                "halsteadBugsDeliveredAft," +
                "halsteadMaintainabilityAft," +
                "cyclomaticComplexityAft," +
                "cognitiveComplexityAft," +
                "lackOfCohesionInMethodAft\n"
        );

        for (Document document : documents) {
            System.out.println(document);

            //TODO: Test when I have the final database
            //RefactoringInfo refactoringInfo = getRefactoringInfo(document);

            PsiJavaFile file =
                    loadFile("C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\test_files\\AbstractGraphTest.java");
            RefactoringInfo refactoringInfo = new RefactoringInfo(null, "streamLanguageTagsCaseInsensitive",
                    "AbstractGraphTest", "org.apache.commons.rdf.api.AbstractGraphTest",
                    "C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\test_files\\AbstractGraphTest.java",
                    file, file, null);

            saveMetricsToFile(bufferedWriter, refactoringInfo, true);
            saveMetricsToFile(bufferedWriter, refactoringInfo, false);

            break;
        }

        
        bufferedWriter.close();
    }

    void saveMetricsToFile(BufferedWriter writer, RefactoringInfo refactoringInfo, boolean isBefore) throws IOException {
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

    private Pair<ClassMetrics, MethodMetrics> getMethodMetricsFromFile(PsiJavaFile file, String methodName, String className) {
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


    //TODO: Test when I have the final database
    //Maybe add comparison between old method and new method to extract the old and new size
    private RefactoringInfo getRefactoringInfo(Document document) {
        RefactoringInfo info = new RefactoringInfo();

        info.set_id(document.getObjectId("_id"));

        String description = document.getString("description");

        info.setMethodName(getMethodName(description));

        Pair<String, String> classInfo = getClassName(description);
        info.setFullClass(classInfo.getFirst());
        info.setClassName(classInfo.getSecond());

        //TODO: Add git search using the revision hash for the parent commit and the commit
        //Then search for the file in the parent commit and the commit

        for (String filePath : document.getList("files", String.class)) {
            if (filePath.contains(info.getFullClass().replace(".", "/"))) {
                info.setFilePath(filePath);
                info.setBeforeFile(loadFile(filePath));
                return info;
            }
        }

        //If file has multiple classes
        for (String filePath : document.getList("files", String.class)) {
            PsiJavaFile file = loadFile(filePath);
            assert file != null;
            for (PsiClass _class : file.getClasses()){
                if (_class.getName().equals(info.getClassName())){
                    info.setFilePath(filePath);
                    info.setBeforeFile(file);
                    return info;
                }
            }
        }


        System.out.println("File not found");

        return null;
    }

    //TODO: Update when I have the final database
    private PsiJavaFile loadFile(String filePath) {
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
