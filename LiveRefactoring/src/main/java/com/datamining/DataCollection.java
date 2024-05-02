package com.datamining;

import com.analysis.metrics.ClassMetrics;
import com.analysis.metrics.MethodMetrics;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.datamining.Utils.*;
import static com.mongodb.client.model.Accumulators.first;
import static com.mongodb.client.model.Accumulators.push;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static java.util.Arrays.asList;

public class DataCollection extends AnAction {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "smartshark_2_2";
    private static final String COLLECTION_NAME = "refactoring";
    private MongoClient mongoClient;
    private Project project;

    private static final String repositoryPaths = "C:\\Users\\dluis\\Documents\\repoClones";
    private final Map<String, ArrayList<Document>> repoRefactoringMap = new ConcurrentHashMap<>();

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        this.project = anActionEvent.getProject();

        try {
            this.mongoClient = MongoClients.create(CONNECTION_STRING);
        } catch (Exception e) {
            throw new RuntimeException("Error connecting to the database", e);
        }

        long start = System.currentTimeMillis();

        HashSet<Document> refactoringData = getRefactoringData();

        long end = System.currentTimeMillis();

        System.out.println("Data extracted: " + refactoringData.size());

        try {
            extractMetrics(refactoringData);
        } catch (IOException | SQLException | GitAPIException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        long elapsed = end - start;
        long elapsedMinutes = (elapsed / 1000) / 60;
        long elapsedSeconds = (elapsed / 1000) % 60;
        System.out.println("Time for mongo search: " + elapsedMinutes + "m " + elapsedSeconds + "s");

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
                limit(100),
                lookup("commit", "commit_id", "_id", "commit"),
                unwind("$commit"),
                lookup("file_action", "commit._id", "commit_id", "file_actions"),
                unwind("$file_actions"),
                lookup("file", "file_actions.file_id", "_id", "file"),
                unwind("$file"),
                lookup("vcs_system", "commit.vcs_system_id", "_id", "vcs_system"),
                unwind("$vcs_system"),
                match(ne("vcs_system.url", "https://github.com/apache/wss4j.git")), //don't have the data for this repo
                group(
                        "$_id",
                        first("type", "$type"),
                        first("description", "$description"),
                        first("revision_hash", "$commit.revision_hash"),
                        first("parent_revision_hash", "$file_actions.parent_revision_hash"),
                        push("files", "$file.path"),
                        first("repo", "$vcs_system.url")
                )
        );

        return collection.aggregate(pipeline).into(new HashSet<>());
    }

    private void extractMetrics(HashSet<Document> documents) throws IOException, SQLException, GitAPIException, ExecutionException, InterruptedException {

        long start = System.currentTimeMillis();

        for (Document document : documents) {
            Database.countMetrics();

            System.out.println(document);
            String repo = document.getString("repo");

            RefactoringInfo refactoringInfo = getRefactoringInfo(document);

            //If I can't find the file path, the info will be null
            if (refactoringInfo == null){
                continue;
            }

            //Just in case
            if (refactoringInfo.getBeforeFile() == null || refactoringInfo.getAfterFile() == null){
                continue;
            }

            Pair<ClassMetrics, MethodMetrics> beforeMetrics = getMethodMetricsFromFile(refactoringInfo.getBeforeFile(),
                    refactoringInfo.getMethodName(), refactoringInfo.getClassName());

            Pair<ClassMetrics, MethodMetrics> afterMetrics = getMethodMetricsFromFile(refactoringInfo.getAfterFile(),
                    refactoringInfo.getMethodName(), refactoringInfo.getClassName());

            //Sometimes methods/arguments change names, so I can't get the correct data
            if(beforeMetrics.getFirst() == null || beforeMetrics.getSecond() == null || afterMetrics.getSecond() == null || afterMetrics.getFirst() == null) {
                continue;
            }

            Database.saveMetrics(null, beforeMetrics.getSecond(), afterMetrics.getSecond());
        }

        long end = System.currentTimeMillis();
        long elapsed = end - start;
        long elapsedMinutes = (elapsed / 1000) / 60;
        long elapsedSeconds = (elapsed / 1000) % 60;
        System.out.println("Time for extract metrics: " + elapsedMinutes + "m " + elapsedSeconds + "s");
    }


    private RefactoringInfo getRefactoringInfo(Document document) throws IOException, GitAPIException {
        RefactoringInfo info = new RefactoringInfo();

        info.set_id(document.getObjectId("_id"));

        String description = document.getString("description");

        info.setMethodName(getMethodName(description));

        Pair<String, String> classInfo = getClassName(description);
        info.setFullClass(classInfo.getFirst());
        info.setClassName(classInfo.getSecond());

        String repoName = document.getString("repo").split("github.com/")[1].split(".git")[0];
        Git git = Git.open(new File(repositoryPaths + "/" + repoName));

        String filePath = findFilePath(git, repoName, info, document);

        if (filePath == null) {
            git.close();
            return null;
        }

        filePath = repositoryPaths + "/" + repoName + "/" + filePath;

        if (info.getBeforeFile() == null){
            info.setBeforeFile(getFileFromCommit(git, filePath, repoName, document.getString("parent_revision_hash")));
        }

        info.setAfterFile(getFileFromCommit(git, filePath, repoName, document.getString("revision_hash")));

        git.close();

        return info;
    }

    private String findFilePath(Git git, String repoName, RefactoringInfo info, Document document) throws GitAPIException {
        for (String filePath : document.getList("files", String.class)) {
            if (filePath.contains(info.getFullClass().replace(".", "/") + ".java")) {
                return filePath;
            }
        }

        git.checkout().setName(document.getString("parent_revision_hash")).call();

        for (String filePath : document.getList("files", String.class)) {
            String fullFilePath = repositoryPaths + "/" + repoName + "/" + filePath;
            PsiJavaFile file = Utils.loadFile(fullFilePath, repoName, this.project);
            if(file == null){
                continue;
            }

            for (PsiClass _class : file.getClasses()){
                if (_class.getName().equals(info.getClassName())){
                    info.setBeforeFile(file);
                    return filePath;
                }
            }
        }

        return null;
    }

    private PsiJavaFile getFileFromCommit(Git git, String filePath, String repoName, String commitHash) {
        try {
            git.checkout().setName(commitHash).call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }

        return Utils.loadFile(filePath, repoName, this.project);
    }
}
