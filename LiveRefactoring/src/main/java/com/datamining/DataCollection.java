package com.datamining;

import com.analysis.metrics.ClassMetrics;
import com.analysis.metrics.MethodMetrics;
import com.core.Pair;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

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

    private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();;
    private final Map<String, ArrayList<Document>> repoRefactoringMap = new HashMap<>();
    private final Map<String, ReentrantLock> repoLocks = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final ReentrantLock logLock = new ReentrantLock();

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

        documentsToMap(refactoringData);

        for (String repo : repoRefactoringMap.keySet()) {
            repoLocks.put(repo, new ReentrantLock());
            System.out.println(repo + ": " + repoRefactoringMap.get(repo).size());
        }

        extractMetrics();

        long end = System.currentTimeMillis();
        long elapsed = end - start;
        long elapsedMinutes = (elapsed / 1000) / 60;
        long elapsedSeconds = (elapsed / 1000) % 60;
        System.out.println("Elapsed Time: " + elapsedMinutes + "m " + elapsedSeconds + "s");
        logProgress("Elapsed Time: " + elapsedMinutes + "m " + elapsedSeconds + "s\n");
        Database.countMetrics();
    }

    /**
     * Gets all the instances where an Extract Method refactoring was performed
     * @return A set of documents containing the refactoring data
     */
    private HashSet<Document> getRefactoringData() {
        MongoCollection<Document> collection = this.mongoClient.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);

        List<Bson> pipeline = asList(
                match(eq("type", "extract_method")),
                skip(0),
                limit(20000),
                project(fields(include("_id", "commit_id", "type", "description"))),
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

    private void documentsToMap(HashSet<Document> documents) {
        repoRefactoringMap.clear();

        for (Document document : documents) {
            String repo = document.getString("repo");

            if (!repoRefactoringMap.containsKey(repo)) {
                repoRefactoringMap.put(repo, new ArrayList<>());
            }

            repoRefactoringMap.get(repo).add(document);
        }
    }

    private void extractMetrics() {
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);

        Database.countMetrics();

        try {
            Queue<String> repos = new LinkedList<>(repoRefactoringMap.keySet());

            while (!repos.isEmpty()){
                List<Callable<Void>> tasks = new ArrayList<>();
                for (int i = 0; i < MAX_THREADS; i++) {
                    String repo = repos.poll();
                    if (repo == null){
                        break;
                    }

                    tasks.add(() -> {
                        try {
                            extractMetrics(repoRefactoringMap.get(repo), repo);
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                        return null;
                    });
                }

                executor.invokeAll(tasks);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }

        Database.countMetrics();
    }

    private void extractMetrics(ArrayList<Document> documents, String repo) throws IOException {
        repoLocks.get(repo).lock();
        System.out.println("(" + Thread.currentThread().getId() + ") Extracting metrics for " + repo);
        long start = System.currentTimeMillis();

        int errors = 0;
        int equalMetrics = 0;
        int size = documents.size();
        for (int i = 0; i < size; i++) {
            Document document = documents.get(i);
            System.out.println("(" + Thread.currentThread().getId() + ") " + repo + " - " + (i + 1) + "/" + size);

            RefactoringInfo refactoringInfo = getRefactoringInfo(document);

            //If I can't find the file path, the info will be null
            if (refactoringInfo == null){
                errors++;
                continue;
            }

            //Just in case
            if (refactoringInfo.getBeforeFile() == null || refactoringInfo.getAfterFile() == null){
                errors++;
                continue;
            }

            Pair<ClassMetrics, MethodMetrics> beforeMetrics = getMethodMetricsFromFile(refactoringInfo.getBeforeFile(),
                    refactoringInfo.getMethodName(), refactoringInfo.getClassName());

            if(beforeMetrics == null || beforeMetrics.getFirst() == null || beforeMetrics.getSecond() == null){
                errors++;
                continue;
            }

            Pair<ClassMetrics, MethodMetrics> afterMetrics = getMethodMetricsFromFile(refactoringInfo.getAfterFile(),
                    refactoringInfo.getMethodName(), refactoringInfo.getClassName());

            if(afterMetrics == null || afterMetrics.getFirst() == null || afterMetrics.getSecond() == null){
                errors++;
                continue;
            }

            if(beforeMetrics.getSecond().equals(afterMetrics.getSecond())){
                equalMetrics++;
            }

            saveMetricsSafe(beforeMetrics.getSecond(), afterMetrics.getSecond());
        }

        long end = System.currentTimeMillis();
        long elapsed = end - start;
        long elapsedMinutes = (elapsed / 1000) / 60;
        long elapsedSeconds = (elapsed / 1000) % 60;
        System.out.println("Elapsed Time for " + repo + ": " + elapsedMinutes + "m " + elapsedSeconds + "s");
        System.out.println("Equal Metrics: " + equalMetrics + "/" + size);
        System.out.println("Errors: " + errors + "/" + size);

        logProgress("Elapsed Time for " + repo + ": " + elapsedMinutes + "m " + elapsedSeconds + "s\n");
        logProgress("Equal Metrics: " + equalMetrics + "/" + size + "\n");
        logProgress("Errors: " + errors + "/" + size + "\n\n");
        repoLocks.get(repo).unlock();
    }


    private RefactoringInfo getRefactoringInfo(Document document) throws IOException {
        RefactoringInfo info = new RefactoringInfo();

        info.set_id(document.getObjectId("_id"));

        String description = document.getString("description");

        info.setMethodName(getMethodName(description));

        Pair<String, String> classInfo = getClassName(description);
        info.setFullClass(classInfo.getFirst());
        info.setClassName(classInfo.getSecond());

        String repoName = document.getString("repo").split("github.com/")[1].split(".git")[0];
        Git git = Git.open(new File(repositoryPaths + "/" + repoName));

        String filePath;
        try {
            filePath = findFilePath(git, repoName, info, document);
        } catch (GitAPIException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

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

            Boolean found = ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
                for (PsiClass _class : file.getClasses()){
                    if (_class.getName().equals(info.getClassName())){
                        info.setBeforeFile(file);
                        return true;
                    }
                }
                return false;
            });

            if (found)
                return filePath;
        }

        return null;
    }

    private PsiJavaFile getFileFromCommit(Git git, String filePath, String repoName, String commitHash) {
        try {
            git.checkout().setName(commitHash).call();
        } catch (GitAPIException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return loadFile(filePath, repoName, this.project);
    }

    private void saveMetricsSafe(MethodMetrics beforeMetrics, MethodMetrics afterMetrics) {
        lock.lock();
        try {
            Database.saveMetrics(null, beforeMetrics, afterMetrics);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private void logProgress(String message){
        logLock.lock();
        String path = "C:\\Users\\dluis\\Documents\\log.txt";
        try {
            Files.write(Paths.get(path), message.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logLock.unlock();
    }
}
