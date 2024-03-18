package com.datamining;

import com.core.Pair;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.jetbrains.annotations.NotNull;
import org.refactoringminer.api.*;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.io.*;
import java.util.*;

//TODO: Give credits to RefactoringMiner
//      https://github.com/tsantalis/RefactoringMiner?tab=readme-ov-file#how-to-cite-refactoringminer

//TODO: Maybe I need to save the branch and commit the repo currently is, so I can return to it after the extraction
public class RepositoryMetricsExtraction extends AnAction {
//public class RepositoryMetricsExtraction {
    private static final String EXTRACTED_METRICS_FILE_PATH = "tmp/repo_extracted_metrics.csv";
    private String repositoryPath;
    private String branch;
    private Project project;
    private Map<String, Set<String>> changedFiles = new HashMap<>();
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        RMEDialogWrapper dialogWrapper = new RMEDialogWrapper();
        dialogWrapper.show();
        
        if(dialogWrapper.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            this.repositoryPath = dialogWrapper.getRepositoryPath();
            this.branch = dialogWrapper.getBranch();
            
            this.project = anActionEvent.getProject();

            System.out.println("Extracting metrics");

            try {
                extractMetrics();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Extracts the metrics from all the Extract Method refactorings in the specific branch of the repository and saves them to a file
     * @throws Exception If there is an error with the git API, the repository or the file
     */
    private void extractMetrics() throws Exception {
        File tmpFolder = new File("tmp");
        if(!tmpFolder.exists()) {
            tmpFolder.mkdir();
        }

        cloneRepo(tmpFolder);

        Set<RefactoringInfo> refactoringInfos = getRefactorings();

        System.out.println("Refactorings extracted: " + refactoringInfos.size());

        File metricsFile = new File(EXTRACTED_METRICS_FILE_PATH);
        if(!metricsFile.exists()) {
            metricsFile.createNewFile();
        }

        System.out.println("Metrics file path: " + metricsFile.getAbsolutePath());

        FileWriter writer = new FileWriter(metricsFile.getAbsolutePath(), false);
        BufferedWriter bufferedWriter = new BufferedWriter(writer);

        //Write the header of the file
        writer.write(
                "numberLinesOfCodeBef," + "numberCommentsBef," + "numberBlankLinesBef," + "totalLinesBef," +
                    "numParametersBef," + "numStatementsBef," + "halsteadLengthBef," + "halsteadVocabularyBef," +
                    "halsteadVolumeBef," + "halsteadDifficultyBef," + "halsteadEffortBef," + "halsteadLevelBef," +
                    "halsteadTimeBef," + "halsteadBugsDeliveredBef," + "halsteadMaintainabilityBef," +
                    "cyclomaticComplexityBef," + "cognitiveComplexityBef," + "lackOfCohesionInMethodBef\n"
        );

        for (RefactoringInfo refactoringInfo : refactoringInfos) {
            String filePath = getFilePath(refactoringInfo);

            if(filePath == null) {
                System.out.println("File not found for refactoring: " + refactoringInfo.getMethodName() + " at commit " + refactoringInfo.getCommitId());
                continue;
            }

            refactoringInfo.setFilePath(filePath);
            saveMetrics(refactoringInfo, bufferedWriter);
        }

        bufferedWriter.close();

        System.out.println("Metrics extracted");

        deleteClonedRepo();

        System.out.println("Cloned repo deleted");
    }

    /**
     * Clone the repository to a temporary folder
     * @param file The temporary folder
     * @throws IOException If there is an error with the repository
     * @throws GitAPIException If there is an error with the git API
     */
    private void cloneRepo(File file) throws IOException, GitAPIException {
        //Check if there is already a cloned repo and delete if there is
        File clonedRepo = new File("tmp/repo");
        if(clonedRepo.exists()) {
            FileUtils.deleteDirectory(clonedRepo);
        }

        //Clone the repository so the original one is not affected
        Git.cloneRepository()
                .setURI("file://" + this.repositoryPath)
                .setDirectory(new File("tmp/repo"))
                .setBranch(this.branch)
                .call();

        this.repositoryPath = file.getAbsolutePath() + "/repo";

        System.out.println("Finished cloning the repository");
    }

    /**
     * Delete the cloned repository
     * @throws IOException If there is an error deleting the repository
     */
    private void deleteClonedRepo() throws IOException {
        File clonedRepo = new File("tmp/repo");
        if(clonedRepo.exists()) {
            try {
                FileUtils.deleteDirectory(clonedRepo);
            } catch (IOException e) {
                System.out.println("Path: " + clonedRepo.getAbsolutePath());
                System.out.println("Error deleting the cloned repository: " + e.getMessage());
            }
        }
    }

    /**
     * Get the refactorings in the repository, on the previously specified branch, using RefactoringMiner
     * @return A set with the refactorings
     * @throws Exception If there is an error with the git API or the repository
     */
    private Set<RefactoringInfo> getRefactorings() throws Exception {
        GitService gitService = new GitServiceImpl();
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

        Repository repo = gitService.openRepository(this.repositoryPath);

        Set<String> commits = getCommits();

        Set<RefactoringInfo> refactoringInfos = new HashSet<>();

        for(String commit : commits) {
            try {
                List<RefactoringInfo> temp = refactoringsAtCommit(miner, repo, commit);
                refactoringInfos.addAll(temp);

                //Set the files changes in the commit, so I can find the correct file path later
                if(!temp.isEmpty()){
                    Set<String> filesChanged = getFilesChanged(commit);
                    changedFiles.put(commit, filesChanged);
                }
            } catch (Exception e) {
                //I have to use an old implementation of RefactoringMiner to be compatible with the Plugin, so there are unresolved issues
                //Currently ignoring all the commits that provoke an error
            }
        }

        repo.close();

        return refactoringInfos;
    }

    /**
     * Get the refactorings at a specific commit using RefactoringMiner
     * @param miner The RefactoringMiner object
     * @param repo The repository
     * @param commitId The commit id
     * @return A list with the refactorings at the commit
     */
    private List<RefactoringInfo> refactoringsAtCommit(GitHistoryRefactoringMiner miner, Repository repo, String commitId) {
        List<RefactoringInfo> refactoringInfos = new ArrayList<>();

        //For some reason, this method receives 2 strings, but only the second one is sued as the commit id (I tested it)
        miner.detectAtCommit(repo, null, commitId, new RefactoringHandler() {
            @Override
            public void handle(String commitId, List<Refactoring> refactorings) {
                for (Refactoring ref : refactorings) {

                    if(ref.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
                        //System.out.println(ref);

                        RefactoringInfo refInfo = getRefactoringInfoFromRefactoring(ref);

                        refInfo.setCommitId(commitId);

                        refactoringInfos.add(refInfo);
                    }
                }
            }
        });

        return refactoringInfos;
    }

    /**
     * Create the RefactoringInfo from the Refactoring object obtained from RefactoringMiner
     * @param ref The refactoring object
     * @return The RefactoringInfo object
     */
    private RefactoringInfo getRefactoringInfoFromRefactoring(Refactoring ref) {
        RefactoringInfo refInfo = new RefactoringInfo();

        refInfo.setMethodName(Utils.getMethodName(ref.toString()));

        Pair<String, String> classNames = Utils.getClassName(ref.toString());
        refInfo.setFullClass(classNames.getFirst());
        refInfo.setClassName(classNames.getSecond());

        return refInfo;
    }

    /**
     * Get all the commits in the repository for the branch specified in the class attribute
     * @return A set with all the commit ids
     * @throws GitAPIException If there is an error with the git API
     * @throws IOException If there is an error with the repository
     */
    private Set<String> getCommits() throws GitAPIException, IOException {
        Repository repository = Git.open(new File(this.repositoryPath)).getRepository();

        Set<String> commits = new HashSet<>();
        try (Git git = new Git(repository)) {
            Iterable<RevCommit> allCommits = git.log().add(repository.resolve(this.branch)).call();

            allCommits.forEach(commit -> commits.add(commit.getName()));
        }

        repository.close();
        return commits;
    }

    /**
     * Save the metrics of the file before the refactoring
     * @param refInfo The refactoring info
     * @param bufferedWriter The writer of file where the metrics are to be saved
     * @throws IOException If there is an error writing to the file
     * @throws GitAPIException If there is an error with the git API
     */
    private void saveMetrics(RefactoringInfo refInfo, BufferedWriter bufferedWriter) throws IOException, GitAPIException {
        Git git = Git.open(new File(this.repositoryPath));

        Repository repo = git.getRepository();

        ObjectId commitObjectId = repo.resolve(refInfo.getCommitId());

        RevWalk revWalk = new RevWalk(repo);
        RevCommit commit = revWalk.parseCommit(commitObjectId);

        revWalk.markStart(commit);
        revWalk.sort(RevSort.COMMIT_TIME_DESC);
        RevCommit previousCommit = revWalk.next();
        while (previousCommit != null) {
            if (!previousCommit.getName().equals(refInfo.getCommitId())) {
                break;
            }
            previousCommit = revWalk.next();
        }

        //Checkout to the commit before the refactoring
        git.checkout().setName(previousCommit.getName()).call();

        revWalk.close();
        repo.close();
        git.close();

        String temp = refInfo.getFilePath().replace(".java", "").replace("\\", ".");
        String filePath = this.repositoryPath + "\\" + temp + ".java";

        PsiJavaFile psiFile = Utils.loadFile(filePath, this.project);

        refInfo.setBeforeFile(psiFile);

        Utils.saveMetricsToFile(bufferedWriter, refInfo, true);
    }

    /**
     * Get the file path of the class that was refactored
     * @param refactoringInfo The refactoring info
     * @return The file path
     */
    private String getFilePath(RefactoringInfo refactoringInfo){
        Set<String> filesChanged = changedFiles.get(refactoringInfo.getCommitId());

        if (filesChanged.size() == 1){
            return filesChanged.iterator().next();
        }

        String classFilePath = "/" + refactoringInfo.getFullClass().replace(".", "/") + ".java";
        for(String file : filesChanged){
            if(file.contains(classFilePath)){
                return file;
            }
        }

        //If file has multiple classes
        for(String file : filesChanged){
            PsiJavaFile psiFile = Utils.loadFile(file, this.project);
            assert psiFile != null;
            for (PsiClass _class : psiFile.getClasses()){
                if (_class.getName().equals(refactoringInfo.getClassName())){
                    return file;
                }
            }
        }

        return null;
    }

    /**
     * Finds all the changed files in a commit (including merges)
     * @param commitId The commit id
     * @return A set with the names of the files changed in the commit (it's a set, due to merges)
     */
    private Set<String> getFilesChanged(String commitId) {
        Set<String> filesChanged = new HashSet<>();
        try {
            //Example: git log -m -1 --name-only --pretty="format:" 38983ec445339ed03efe389c53cc0bb6861f30f2
            ProcessBuilder processBuilder = new ProcessBuilder("git", "log", "-m", "-1", "--name-only", "--pretty=\"format:\"", commitId);
            processBuilder.redirectErrorStream(true);
            processBuilder.directory(new File(this.repositoryPath));
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                //Ignore the empty line that exists when dealing with merges
                if(!line.isEmpty() && !line.contains(" "))
                    filesChanged.add(line);
            }

            //System.out.println("Files changed in commit " + commitId + ": " + filesChanged.size());

            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return filesChanged;
    }
}
