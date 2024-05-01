package com.datamining;

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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.datamining.Utils.*;
import static com.mongodb.client.model.Accumulators.first;
import static com.mongodb.client.model.Accumulators.push;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static java.util.Arrays.asList;

public class DataCollection extends AnAction {
//public class DataCollection {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "smartshark_2_2";
    private static final String COLLECTION_NAME = "refactoring";
    private MongoClient mongoClient;
    private Project project;

    private static final String repositoryPaths = "C:\\Users\\dluis\\Documents\\repoClones";

//    public static void main(String[] args) {
//        DataCollection dataCollection = new DataCollection();
//
//        dataCollection.mongoClient = MongoClients.create(CONNECTION_STRING);
//
//        HashSet<Document> refactoringData = dataCollection.getRefactoringData();
//        System.out.println("Data extracted: " + refactoringData.size());
//
//        try {
//            dataCollection.extractMetrics(refactoringData);
//        } catch (IOException | SQLException | GitAPIException e) {
//            throw new RuntimeException(e);
//        }
//
//    }

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
        } catch (IOException | SQLException | GitAPIException | ExecutionException | InterruptedException e) {
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
                limit(100),
                lookup("commit", "commit_id", "_id", "commit"),
                unwind("$commit"),
                lookup("file_action", "commit._id", "commit_id", "file_actions"),
                unwind("$file_actions"),
                lookup("file", "file_actions.file_id", "_id", "file"),
                unwind("$file"),
                lookup("vcs_system", "commit.vcs_system_id", "_id", "vcs_system"),
                unwind("$vcs_system"),
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
        //ExecutorService executor = Executors.newCachedThreadPool();
        //Map<String, CompletableFuture<Void>> repoTaskMap = new ConcurrentHashMap<>();

        int count = 0;
        for (Document document : documents) {
            Database.countMetrics();

            System.out.println(document);
            //String repo = document.getString("repo");

            RefactoringInfo refactoringInfo = getRefactoringInfo(document);

//            Pair<ClassMetrics, MethodMetrics> beforeMetrics = getMethodMetricsFromFile(refactoringInfo.getBeforeFile(),
//                    refactoringInfo.getMethodName(), refactoringInfo.getClassName());
//
//            Pair<ClassMetrics, MethodMetrics> afterMetrics = getMethodMetricsFromFile(refactoringInfo.getAfterFile(),
//                    refactoringInfo.getMethodName(), refactoringInfo.getClassName());
//
//            if (beforeMetrics.getSecond().equals(afterMetrics.getSecond())) {
//                System.out.println("Metrics are equal");
//
//            } else {
//                count++;
//                System.out.println("Before\n: " + beforeMetrics.getSecond());
//                System.out.println("After\n: " + afterMetrics.getSecond());
//                //System.out.println("Equal: " + beforeMetrics.getSecond().equals(afterMetrics.getSecond()));
//            }
//
//            Database.saveMetrics(null, beforeMetrics.getSecond(), afterMetrics.getSecond());
//
//            Database.countMetrics();

            //break;
        }

        System.out.println("Count: " + count);
    }


    private RefactoringInfo getRefactoringInfo(Document document) throws IOException, GitAPIException, ExecutionException, InterruptedException {
        RefactoringInfo info = new RefactoringInfo();

        info.set_id(document.getObjectId("_id"));

        String description = document.getString("description");

        info.setMethodName(getMethodName(description));

        Pair<String, String> classInfo = getClassName(description);
        info.setFullClass(classInfo.getFirst());
        info.setClassName(classInfo.getSecond());
        System.out.println("Class: " + info.getFullClass() + " - " + info.getClassName());
        System.out.println("Method: " + info.getMethodName());

        String repoName = document.getString("repo").split("github.com/")[1].split(".git")[0];
        Git git = Git.open(new File(repositoryPaths + "/" + repoName));

        String filePath = findFilePath(git, info, document);

        if (filePath == null) {
            git.close();
            throw new RuntimeException("File not found: " + description);
        }

        filePath = repositoryPaths + "/" + repoName + "/" + filePath;
        System.out.println("File path: " + filePath);

//        String finalFilePath = filePath;
//        if (info.getBeforeFile() == null){
//            CompletableFuture<PsiJavaFile> futureBefore = CompletableFuture.supplyAsync(() ->
//                    ApplicationManager.getApplication().runReadAction((Computable<PsiJavaFile>) () ->
//                            (PsiJavaFile) getFileFromCommit(git, finalFilePath, document.getString("parent_revision_hash"))));
//            CompletableFuture<PsiJavaFile> futureAfter = futureBefore.thenCompose(fileBefore ->
//                    CompletableFuture.supplyAsync(() ->
//                            ApplicationManager.getApplication().runReadAction((Computable<PsiJavaFile>) () ->
//                                    (PsiJavaFile) getFileFromCommit(git, finalFilePath, document.getString("revision_hash")))));
//
//            info.setBeforeFile(futureBefore.get());
//            info.setAfterFile(futureAfter.get());
//
//            System.out.println("Same: " + info.getBeforeFile().equals(info.getAfterFile()));
//        } else {
//            CompletableFuture<PsiJavaFile> futureAfter = CompletableFuture.supplyAsync(() ->
//                    ApplicationManager.getApplication().runReadAction((Computable<PsiJavaFile>) () ->
//                            (PsiJavaFile) getFileFromCommit(git, finalFilePath, document.getString("revision_hash"))));
//
//            info.setAfterFile(futureAfter.get());
//        }

        PsiJavaFile beforeFile;
        if (info.getBeforeFile() == null){
            beforeFile = getFileFromCommit(git, filePath, document.getString("parent_revision_hash"));
        }
        else {
            beforeFile = info.getBeforeFile();
        }

        MethodMetrics beforeMetrics = getMethodMetricsFromFile(beforeFile, info.getMethodName(), info.getClassName()).getSecond();
        info.setBeforeMetrics(beforeMetrics);

        PsiJavaFile afterFile = getFileFromCommit(git, filePath, document.getString("revision_hash"));
        MethodMetrics afterMetrics = getMethodMetricsFromFile(afterFile, info.getMethodName(), info.getClassName()).getSecond();
        info.setAfterMetrics(afterMetrics);

        git.close();

        return info;
    }

    private String findFilePath(Git git, RefactoringInfo info, Document document) throws GitAPIException {
        for (String filePath : document.getList("files", String.class)) {
            if (filePath.contains(info.getFullClass().replace(".", "/") + ".java")) {
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

        return null;
    }

    private PsiJavaFile getFileFromCommit(Git git, String filePath, String commitHash) {
        try {
            git.checkout().setName(commitHash).call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }

        System.out.println("\ngetFileFromCommit - " + commitHash + "\n");

        //VirtualFile virtualFile = VfsUtil.findFileByIoFile(new File(repositoryPaths), false);
        //VfsUtil.markDirtyAndRefresh(false, true, true, virtualFile);
        PsiJavaFile file = Utils.loadFile(filePath, this.project);

        if(file == null){
            throw new RuntimeException("File not found: " + filePath);
        }

        return file;
    }
}
