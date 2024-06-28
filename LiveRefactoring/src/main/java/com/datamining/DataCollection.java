package com.datamining;

import com.analysis.Candidates;
import com.analysis.candidates.ExtractClassCandidate;
import com.analysis.candidates.ExtractMethodCandidate;
import com.analysis.metrics.ClassMetrics;
import com.analysis.metrics.FileMetrics;
import com.analysis.metrics.MethodMetrics;
import com.analysis.refactorings.ExtractClass;
import com.analysis.refactorings.ExtractMethod;
import com.core.Pair;
import com.core.Refactorings;
import com.core.Severity;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.utils.importantValues.SelectedRefactorings;
import com.utils.importantValues.Values;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.jgit.api.Git;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import static com.datamining.Utils.*;
import static com.mongodb.client.model.Accumulators.first;
import static com.mongodb.client.model.Accumulators.push;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.nin;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static java.util.Arrays.asList;

public class DataCollection extends AnAction {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "smartshark_2_2";
    private static final String COLLECTION_NAME = "refactoring";
    private static final Refactorings REFACTORING_TYPE = Refactorings.ExtractClass;
    private MongoClient mongoClient;
    private Project project;

    private static final String repositoryPaths = "C:\\Users\\dluis\\Documents\\repoClones";

    private static final int MAX_THREADS = 1;
    private final Map<String, ArrayList<Document>> repoRefactoringMap = new HashMap<>();
    private final Map<String, ReentrantLock> repoLocks = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final ReentrantLock logLock = new ReentrantLock();
    private int liveRefMetricsCount = 0;

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
    public HashSet<Document> getRefactoringData() {
        MongoCollection<Document> collection = this.mongoClient.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);

        List<String> urlsToExclude = Arrays.asList(
                "https://github.com/apache/wss4j.git"
        );

        Bson combinedFilter = match(nin("vcs_system.url", urlsToExclude));

        String refactoringType = "";
        if (REFACTORING_TYPE.equals(Refactorings.ExtractMethod)){
            refactoringType = "extract_method";
        } else if (REFACTORING_TYPE.equals(Refactorings.ExtractClass)){
            refactoringType = "extract_class";
        }
        List<Bson> pipeline = asList(
                match(eq("type", refactoringType)),
                //limit(500),
                project(fields(include("_id", "commit_id", "type", "description"))),
                lookup("commit", "commit_id", "_id", "commit"),
                unwind("$commit"),
                lookup("vcs_system", "commit.vcs_system_id", "_id", "vcs_system"),
                unwind("$vcs_system"),
                combinedFilter,
                lookup("file_action", "commit._id", "commit_id", "file_actions"),
                unwind("$file_actions"),
                lookup("file", "file_actions.file_id", "_id", "file"),
                unwind("$file"),
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

    /**
     * Converts the documents to a map where the key is the repository and the value is a list of documents
     * @param documents The documents to convert
     */
    public void documentsToMap(HashSet<Document> documents) {
        repoRefactoringMap.clear();

        for (Document document : documents) {
            String repo = document.getString("repo");

            if (!repoRefactoringMap.containsKey(repo)) {
                repoRefactoringMap.put(repo, new ArrayList<>());
            }

            repoRefactoringMap.get(repo).add(document);
        }
    }

    /**
     * Extracts the metrics from the documents
     */
    private void extractMetrics() {
//        ThreadPoolExecutor pool = new ThreadPoolExecutor(
//                MAX_THREADS,
//                MAX_THREADS,
//                0L,
//                java.util.concurrent.TimeUnit.MILLISECONDS,
//                new java.util.concurrent.LinkedBlockingQueue<>());
//        ExecutorCompletionService<Void> completionService = new ExecutorCompletionService<>(pool);
//
//        Database.countMetrics();
//
//        try {
//            ArrayList<String> reposList = new ArrayList<>(repoRefactoringMap.keySet());
//            reposList.sort(Comparator.comparingInt(o -> repoRefactoringMap.get(o).size()));
//
//            for (int i = reposList.size()-1; i >= 0; i--) {
//                String repo = reposList.get(i);
//
//                completionService.submit(() -> {
//                    try {
//                        extractMetrics(repoRefactoringMap.get(repo), repo);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    } finally {
//                        repoRefactoringMap.remove(repo);
//                    }
//                    return null;
//                });
//            }
//
//            for (int i = 0; i < reposList.size(); i++) {
//                try {
//                    completionService.take().get();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            pool.shutdown();
//        }

        Database.countMetrics();

        ArrayList<String> reposList = new ArrayList<>(repoRefactoringMap.keySet());
        reposList.sort(Comparator.comparingInt(o -> repoRefactoringMap.get(o).size()));

        for (int i = reposList.size()-1; i >= 0; i--) {
            String repo = reposList.get(i);

            try {
                extractMetrics(repoRefactoringMap.get(repo), repo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //delete fileCopies folder
        String fileCopiesFolder = Values.dataFolder + "fileCopies/";
        File file = new File(fileCopiesFolder);
        try {
            FileUtils.deleteDirectory(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Database.countMetrics();
    }

    /**
     * Extracts the metrics from the documents
     * @param documents The documents to extract the metrics from
     * @param repo The repository the documents are from
     */
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

            if(REFACTORING_TYPE.equals(Refactorings.ExtractMethod)){
                MethodMetrics beforeMetrics = getMethodMetricsFromFile(refactoringInfo.getBeforeFile(),
                        refactoringInfo.getMethodName(), refactoringInfo.getClassName());

                if(beforeMetrics == null){
                    errors++;
                    continue;
                }

                MethodMetrics afterMetrics = getMethodMetricsFromFile(refactoringInfo.getAfterFile(),
                        refactoringInfo.getMethodName(), refactoringInfo.getClassName());

                if(afterMetrics == null){
                    errors++;
                    continue;
                }

                if(beforeMetrics.equals(afterMetrics)){
                    equalMetrics++;
                }

                Integer id = saveMethodMetricsSafe(beforeMetrics, afterMetrics);

                //runPluginAndSaveResult(false, refactoringInfo.getBeforeFile(), beforeMetrics.method, refactoringInfo.getMethodName(), refactoringInfo.getClassName(), id, beforeMetrics.equals(afterMetrics));

            } else if (REFACTORING_TYPE.equals(Refactorings.ExtractClass)){
                ClassMetrics beforeMetrics = getClassMetricsFromFile(refactoringInfo.getBeforeFile(), refactoringInfo.getClassName());

                if(beforeMetrics == null){
                    errors++;
                    continue;
                }

                ClassMetrics afterMetrics = getClassMetricsFromFile(refactoringInfo.getAfterFile(), refactoringInfo.getClassName());

                if(afterMetrics == null){
                    errors++;
                    continue;
                }

                if(beforeMetrics.equals(afterMetrics)){
                    equalMetrics++;
                }

                Integer id = saveClassMetricsSafe(beforeMetrics, afterMetrics);

                //runPluginAndSaveResult(refactoringInfo.getBeforeFile(), beforeMetrics.targetClass, id, refactoringInfo.getClassName());
            }
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

    /**
     * Gets the refactoring info from the document
     * @param document The document to get the info from
     * @return The refactoring info
     * @throws IOException If there is an error getting the refactoring info
     */
    private RefactoringInfo getRefactoringInfo(Document document) throws IOException {
        RefactoringInfo info = new RefactoringInfo();

        info.set_id(document.getObjectId("_id"));

        String description = document.getString("description");

        if(REFACTORING_TYPE.equals(Refactorings.ExtractMethod)){
            info.setMethodName(getMethodName(description));

            Pair<String, String> classInfo = getClassName(description);
            info.setFullClass(classInfo.getFirst());
            info.setClassName(classInfo.getSecond());
        } else if (REFACTORING_TYPE.equals(Refactorings.ExtractClass)) {
            Pair<String, String> classInfo = getOldClass(description);
            info.setFullClass(classInfo.getFirst());
            info.setClassName(classInfo.getSecond());
        }


        String repoName = document.getString("repo").split("github.com/")[1].split(".git")[0];
        Git git = Git.open(new File(repositoryPaths + "/" + repoName));

        String filePath;
        filePath = findFilePath(git, repoName, info, document);

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

    /**
     * Finds the file path of the refactored file
     * @param git The git object
     * @param repoName The name of the repository
     * @param info The refactoring info
     * @param document The document
     * @return The file path
     */
    private String findFilePath(Git git, String repoName, RefactoringInfo info, Document document) {
        for (String filePath : document.getList("files", String.class)) {
            if (filePath.contains(info.getFullClass().replace(".", "/") + ".java")) {
                return filePath;
            }
        }

        try {
            git.checkout().setName(document.getString("parent_revision_hash")).call();
        } catch (Exception e) {
            e.printStackTrace();
            return null; //Found a commit that doesn't exist
        }

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

    /**
     * Gets the file from the specific commit
     * @param git The git object
     * @param filePath The file path
     * @param repoName The name of the repository
     * @param commitHash The commit hash
     * @return The file
     */
    private PsiJavaFile getFileFromCommit(Git git, String filePath, String repoName, String commitHash) {
        try {
            git.checkout().setName(commitHash).call();
        } catch (Exception e) {
            e.printStackTrace();
            return null; //Found a commit that doesn't exist
        }

        return loadFile(filePath, repoName, this.project);
    }

    /**
     * Saves the method metrics to the database
     * @param beforeMetrics The metrics before the refactoring
     * @param afterMetrics The metrics after the refactoring
     * @return The id of the saved metrics
     */
    private Integer saveMethodMetricsSafe(MethodMetrics beforeMetrics, MethodMetrics afterMetrics) {
        lock.lock();

        Integer id = Database.saveMethodMetrics(null, beforeMetrics, afterMetrics);

        lock.unlock();

        return id;
    }

    /**
     * Saves the class metrics to the database
     * @param beforeMetrics The metrics before the refactoring
     * @param afterMetrics The metrics after the refactoring
     * @return The id of the saved metrics
     */
    private Integer saveClassMetricsSafe(ClassMetrics beforeMetrics, ClassMetrics afterMetrics) {
        lock.lock();

        Integer id = Database.saveClassMetrics(null, beforeMetrics, afterMetrics);

        lock.unlock();

        return id;
    }

    /**
     * Logs progress to a file
     * @param message The message to log
     */
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


    /* MOCK PLUGIN */

        /* COMMON */

    /**
     * Initialises the values needed to run the plugin
     * @param file The file to run the plugin on
     * @param refactoring The type of refactoring to run
     */
    private void initialiseScriptValues(PsiJavaFile file, Refactorings refactoring) {
        SelectedRefactorings.selectedRefactoring = refactoring;
        SelectedRefactorings.selectedRefactorings = new ArrayList<>();
        SelectedRefactorings.selectedRefactorings.add(refactoring);
        Values.editor = FileEditorManager.getInstance(this.project).openTextEditor(new OpenFileDescriptor(this.project, file.getVirtualFile()), true);
        Values.isActive = true;
    }

    /**
     * Gets the candidates for the refactoring
     * @param isMethod If true, the refactoring is Extract Method, otherwise it is Extract Class
     * @param object The candidate object (ExtractMethodCandidate or ExtractClassCandidate)
     * @return The candidates
     */
    private ArrayList<Severity> getCandidates(boolean isMethod, Object object) {
        Candidates candidatesClass = new Candidates();
        Values.candidates = candidatesClass.getSeverities();

        if(Values.candidates.isEmpty()){
            return null;
        }

        ArrayList<Severity> candidates = new ArrayList<>();
        for (Severity severity : Values.candidates){
            if(isMethod) {
                PsiMethod method = (PsiMethod) object;
                ExtractMethodCandidate candidate = (ExtractMethodCandidate) severity.candidate;
                if (sameMethod(method, candidate.method)) {
                    candidates.add(severity);
                }
            } else {
                PsiClass _class = (PsiClass) object;
                ExtractClassCandidate candidate = (ExtractClassCandidate) severity.candidate;
                if (sameClass(_class, candidate.targetClass)) {
                    candidates.add(severity);
                }
            }
        }

        if(candidates.isEmpty()){
            return null;
        }

        candidates.sort(Comparator.comparingDouble(o -> o.severity));
        return candidates;
    }

    /**
     * Runs a task with a timeout
     * @param task The task to run
     * @param timeout The timeout in seconds
     * @return If the task timed out
     */
    private boolean runWithTimeout(Callable<Void> task, int timeout) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Void> future = executor.submit(task);
        boolean timeoutReached = false;

        try {
            future.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true); // Interrupt the task
            timeoutReached = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(timeout, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdown();
            }
        }

        return timeoutReached;
    }

        /* EXTRACT METHOD */

    /**
     * Runs the plugin for Extract Method to test and (possibly) saves the resulting metrics
     * @param mock If true, the plugin will not perform the refactoring
     * @param file The file to run the plugin on
     * @param method The method to run the plugin on
     * @param methodName The name of the method
     * @param className The name of the class
     * @param id The id of the method metrics on the database to be able to link it to the new metrics
     * @param same
     */
    private void runPluginAndSaveResult(boolean mock, PsiJavaFile file, PsiMethod method, String methodName, String className, Integer id, boolean same) {
        initialiseScriptValues(file, Refactorings.ExtractMethod);

        boolean timeout = initialiseCandidates(file, method);
        if (timeout) {
            System.out.println("Timeout on initialise candidates");
            return;
        }

        ArrayList<Severity> candidates = getCandidates(true, method);
        if (candidates == null) {
            if (!mock)
                Database.saveAfterLiveRefMetrics(null, id, same);
            else
                Database.saveAfterNewLiveRefMetrics(false, same);
            return;
        }

        if (!mock) {
            //Actually running the plugin and performing the refactoring
            Severity severity = candidates.get(candidates.size() - 1);
            ExtractMethodCandidate extractMethodCandidate = (ExtractMethodCandidate) severity.candidate;
            try {
                extractAndSave(extractMethodCandidate, severity, file, methodName, className, id, same);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            //Not performing the refactoring, but saving the fact that the plugin would do it
            Database.saveAfterNewLiveRefMetrics(true, same);
            liveRefMetricsCount++;
            System.out.println("Saved after life ref metrics (" + liveRefMetricsCount + ")");
        }
    }

    /**
     * Runs the plugin to get the candidates for Extract Method
     * @param file The file to run the plugin on
     * @param method The method to run the plugin on
     * @return If the plugin timed out
     */
    private boolean initialiseCandidates(PsiJavaFile file, PsiMethod method) {
        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
            try {
                Values.before = new FileMetrics(file);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Values.currentFile = Values.before;
            Values.tempMethod = method;

            ExtractMethod extractMethod = new ExtractMethod(Values.editor, file);

            Callable<Void> task = () -> {
                ApplicationManager.getApplication().runReadAction(extractMethod::run);
                return null;
            };

            return runWithTimeout(task, 30);
        });
    }

    /**
     * Performs the actual Extract Method refactoring and saves the resulting metrics
     * @param extractMethodCandidate The candidate to perform the refactoring
     * @param severity The severity of the candidate
     * @param file The file to perform the refactoring on
     * @param methodName The name of the method
     * @param className The name of the class
     * @param id The id of the method metrics on the database to be able to link it to the new metrics
     * @param same
     */
    private void extractAndSave(ExtractMethodCandidate extractMethodCandidate, Severity severity, PsiJavaFile file, String methodName, String className, int id, boolean same) {
        simulateDialog(extractMethodCandidate);

        ApplicationManager.getApplication().runReadAction(() -> {
            ExtractMethod extractMethod = new ExtractMethod(Values.editor);
            extractMethod.extractMethod(extractMethodCandidate, severity.severity, severity.indexColorGutter);
        });

        MethodMetrics methodMetrics = getMethodMetricsFromFile(file, methodName, className);

        if (methodMetrics == null)
            return;

        Database.saveAfterLiveRefMetrics(methodMetrics, id, same);
        liveRefMetricsCount++;
        System.out.println("Saved after life ref metrics (" + liveRefMetricsCount + ")");
    }

    /**
     * Simulates the use of the dialog to perform the refactoring
     * @param candidate The candidate to perform the refactoring
     */
    private void simulateDialog(ExtractMethodCandidate candidate) {
        Thread clickButtonThread = new Thread(() -> {
            int count = 0;
            JDialog dialog;

            String title = "Extract Method applied to " + candidate.method.getName();
            while ((dialog = getDialogOpen(title)) == null && count < 20){
                try {
                    Thread.sleep(600);
                    count++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if(dialog != null) {
                clickRefactorButton(dialog);
            } else {
                System.out.println("Dialog not found");
            }
        });
        clickButtonThread.start();
    }

    /**
     * Clicks the refactor button on the dialog
     * @param dialog The dialog to click the button on
     */
    private void clickRefactorButton(JDialog dialog) {
        Component[] components = dialog.getComponents();
        for(Component component : components){
            try {
                for(int i = 0; i < 7; i++) {
                    SwingUtilities.invokeAndWait(() -> {
                        component.dispatchEvent(new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED));
                        component.dispatchEvent(new KeyEvent(component, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED));
                    });
                }
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Finds an open Dialog
     * @param title The title of the dialog
     * @return The dialog
     */
    private JDialog getDialogOpen(String title) {
        for(Window window: Window.getWindows()){
            if(window instanceof JDialog){
                JDialog dialog = (JDialog) window;
                if(dialog.getTitle().equals(title)){
                    return dialog;
                }
            }
        }
        return null;
    }

        /* EXTRACT CLASS */

    /**
     * Runs the plugin for Extract Class to test and saves if it would perform the refactoring or not
     * @param file The file to run the plugin on
     * @param _class The class to run the plugin on
     * @param id The id of the class metrics on the database to be able to link it to the new metrics
     * @param className The name of the class
     */
    private void runPluginAndSaveResult(PsiJavaFile file, PsiClass _class, Integer id, String className) {
        initialiseScriptValues(file, Refactorings.ExtractClass);

        boolean timeout = initialiseCandidates(file, _class);
        if (timeout) {
            System.out.println("Timeout on initialise candidates");
            return;
        }

        ArrayList<Severity> candidates = getCandidates(false, _class);
        if (candidates == null) {
            Database.saveClassMetricsLiveRefSaraYesNo(false);
            return;
        }

        //Not performing the refactoring, but saving the fact that the plugin would do it
        Database.saveClassMetricsLiveRefSaraYesNo(true);
        liveRefMetricsCount++;
        System.out.println("Saved after life ref metrics (" + liveRefMetricsCount + ")");
    }

    /**
     * Runs the plugin to get the candidates for Extract Method
     * @param file The file to run the plugin on
     * @param _class The class to run the plugin on
     * @return If the plugin timed out
     */
    private boolean initialiseCandidates(PsiJavaFile file, PsiClass _class) {
        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
            try {
                Values.before = new FileMetrics(file);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Values.currentFile = Values.before;
            Values.tempClass = _class;

            ExtractClass extractClass = new ExtractClass(file, Values.editor);

            Callable<Void> task = () -> {
                ApplicationManager.getApplication().runReadAction(extractClass::run);
                return null;
            };

            return runWithTimeout(task, 30);
        });
    }
}