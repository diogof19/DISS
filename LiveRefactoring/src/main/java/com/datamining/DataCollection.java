package com.datamining;

import com.core.Pair;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;

import static com.datamining.Utils.*;
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

    private String repositoryPath;

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
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Gets all the instances where an Extract Method refactoring was performed
     * @return A set of documents containing the refactoring data
     */
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

    private void extractMetrics(HashSet<Document> documents) throws IOException, SQLException {
        for (Document document : documents) {
            System.out.println(document);

            //TODO: Test when I have the final database
            //RefactoringInfo refactoringInfo = getRefactoringInfo(document);

            PsiJavaFile file =
                    loadFile("C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\test_files\\AbstractGraphTest.java",
                            this.project);
            RefactoringInfo refactoringInfo = new RefactoringInfo(null, "streamLanguageTagsCaseInsensitive",
                    "AbstractGraphTest", "org.apache.commons.rdf.api.AbstractGraphTest",
                    "C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\test_files\\AbstractGraphTest.java",
                    file, file, null, null);

            //TODO: Replace the 2nd argument with the correct refactoringInfo
            Database.saveMetrics(refactoringInfo, refactoringInfo);

            break;
        }
    }


    //TODO: Test when I have the final database
    //Maybe add comparison between old method and new method to extract the old and new size
    private RefactoringInfo getRefactoringInfo(Document document) throws IOException, GitAPIException {
        RefactoringInfo info = new RefactoringInfo();

        info.set_id(document.getObjectId("_id"));

        String description = document.getString("description");

        info.setMethodName(getMethodName(description));

        Pair<String, String> classInfo = getClassName(description);
        info.setFullClass(classInfo.getFirst());
        info.setClassName(classInfo.getSecond());

        Git git = Git.open(new File(this.repositoryPath));

        String filePath = findFilePath(git, info, document);

        if (filePath == null) {
            git.close();
            throw new RuntimeException("File not found: " + description);
        }

        if (info.getBeforeFile() == null)
            info.setBeforeFile(getFileFromCommit(git, filePath, document.getString("parent_revision_hash")));

        info.setAfterFile(getFileFromCommit(git, filePath, document.getString("revision_hash")));

        git.close();

        return info;
    }

    private String findFilePath(Git git, RefactoringInfo info, Document document) throws IOException, GitAPIException {
        //TODO: Check if there's the issue with the .java and the class name (Ex: MovableTest and Movable)
        for (String filePath : document.getList("files", String.class)) {
            if (filePath.contains(info.getFullClass().replace(".", "/"))) {
                return filePath;
            }
        }

        git.checkout().setName(document.getString("parent_revision_hash")).call();

        for (String filePath : document.getList("files", String.class)) {
            PsiJavaFile file = Utils.loadFile(filePath, this.project);
            assert file != null;
            for (PsiClass _class : file.getClasses()){
                if (_class.getName().equals(info.getClassName())){
                    info.setBeforeFile(file);
                    return filePath;
                }
            }
        }

        git.close();

        return null;
    }

    private PsiJavaFile getFileFromCommit(Git git, String filePath, String commitHash) throws GitAPIException {
        git.checkout().setName(commitHash).call();

        PsiJavaFile file = Utils.loadFile(filePath, this.project);

        return file;
    }
}
