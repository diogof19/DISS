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
        System.out.println("Hello World");

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

        List<Bson> pipeline = asList(
                //(or(eq("type", "extract_method"), eq("type", "extract_class"), eq("type", "extract_variable"))),
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
                "numberLinesOfCode," +
                "numberComments," +
                "numberBlankLines," +
                "totalLines," +
                "numParameters," +
                "numStatements," +
                "halsteadLength," +
                "halsteadVocabulary," +
                "halsteadVolume," +
                "halsteadDifficulty," +
                "halsteadEffort," +
                "halsteadLevel," +
                "halsteadTime," +
                "halsteadBugsDelivered," +
                "halsteadMaintainability," +
                "cyclomaticComplexity," +
                "cognitiveComplexity," +
                "lackOfCohesionInMethod\n"
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
                    file, null);

            /*
               ML model:
                -> methodMetrics from BeforeFile

               methodMetrics from AfterFile:
                -> numberOfLinesOfCode could be used to refine one of the thresholds
                -> can be used for later analysis

               classMetrics from BeforeFile & AfterFile:
                -> can be used for later analysis and comparison reasons
             */

            Pair<ClassMetrics, MethodMetrics> beforeMetrics = getMethodMetricsFromFile(refactoringInfo.getBeforeFile(),
                    refactoringInfo.getMethodName(), refactoringInfo.getClassName());

//            Pair<ClassMetrics, MethodMetrics> afterMetrics = getMethodMetricsFromFile(refactoringInfo.getAfterFile(),
//                    refactoringInfo.getMethodName(), refactoringInfo.getClassName());
            
            //write the metrics to the file
            
            MethodMetrics beforeMethodMetrics = beforeMetrics.getSecond();

            int totalLines = beforeMethodMetrics.numberLinesOfCode + beforeMethodMetrics.numberComments +
                    beforeMethodMetrics.numberBlankLines;

            bufferedWriter.write(
                    "\"" + refactoringInfo.get_id() + "\"," +
                    beforeMethodMetrics.numberLinesOfCode + "," +
                    beforeMethodMetrics.numberComments + "," +
                    beforeMethodMetrics.numberBlankLines + "," +
                    totalLines + "," +
                    beforeMethodMetrics.numParameters + "," +
                    beforeMethodMetrics.numberOfStatements + "," +
                    beforeMethodMetrics.halsteadLength + "," +
                    beforeMethodMetrics.halsteadVocabulary + "," +
                    beforeMethodMetrics.halsteadVolume + "," +
                    beforeMethodMetrics.halsteadDifficulty + "," +
                    beforeMethodMetrics.halsteadEffort + "," +
                    beforeMethodMetrics.halsteadLevel + "," +
                    beforeMethodMetrics.halsteadTime + "," +
                    beforeMethodMetrics.halsteadBugsDelivered + "," +
                    beforeMethodMetrics.halsteadMaintainability + "," +
                    beforeMethodMetrics.complexityOfMethod + "," +
                    beforeMethodMetrics.cognitiveComplexity + "," +
                    beforeMethodMetrics.lackOfCohesionInMethod + "\n"
            );

            break;
        }

        
        bufferedWriter.close();
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

        String methodName = description.split("\\(\\)")[0].replace("public ", "")
                .replace("private ", "").replace("protected ", "")
                .trim().replace("static ", "").trim();
        info.setMethodName(methodName);

        String fullClass = description.split("from")[1].split("in class")[1].trim();
        info.setFullClass(fullClass);

        String[] parts = fullClass.split("\\.");
        final String className = parts[parts.length - 1];
        info.setClassName(className);

        //TODO: Add git search using the revision hash for the parent commit and the commit
        //Then search for the file in the parent commit and the commit

        for (String filePath : document.getList("files", String.class)) {
            if (filePath.contains(fullClass.replace(".", "/"))) {
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
                if (_class.getName().equals(className)){
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
