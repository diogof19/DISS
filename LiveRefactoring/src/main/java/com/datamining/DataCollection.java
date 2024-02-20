package com.datamining;

import com.analysis.metrics.FileMetrics;
import com.analysis.metrics.MethodMetrics;
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

import java.io.File;
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

        extractMetrics(refactoringData);

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

    private void extractMetrics(HashSet<Document> documents) {
        for (Document document : documents) {
            System.out.println(document);

            //TODO: Test when I have the final database
            //FileInfo fileInfo = getFileInfo(document);

            PsiJavaFile file =
                    loadFile("C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\test_files\\AbstractGraphTest.java");
            FileInfo fileInfo = new FileInfo("streamLanguageTagsCaseInsensitive",
                    "AbstractGraphTest", "org.apache.commons.rdf.api.AbstractGraphTest",
                    "C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\test_files\\AbstractGraphTest.java",
                    file);

            FileMetrics fileMetrics;
            try {
                fileMetrics = new FileMetrics(fileInfo.getFile());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            MethodMetrics methodMetrics = fileMetrics.methodMetrics.stream()
                    .filter(m -> m.methodName.equals(fileInfo.getMethodName()) && m.className.equals(fileInfo.getClassName()))
                    .findFirst()
                    .orElse(null);

            break;
        }
    }

    //TODO: Test when I have the final database
    //Maybe add comparison between old method and new method to extract the old and new size
    private FileInfo getFileInfo(Document document) {
        FileInfo info = new FileInfo();

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

        for (String filePath : document.getList("files", String.class)) {
            if (filePath.contains(fullClass.replace(".", "/"))) {
                info.setFilePath(filePath);
                info.setFile(loadFile(filePath));
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
                    info.setFile(file);
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
