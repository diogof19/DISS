package com.datamining;

import com.core.Pair;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.refactoringminer.api.*;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.io.*;
import java.util.*;

//TODO: Give credits to RefactoringMiner
//      https://github.com/tsantalis/RefactoringMiner?tab=readme-ov-file#how-to-cite-refactoringminer
//public class RepositoryMetricsExtraction extends AnAction {
public class RepositoryMetricsExtraction {
    private static final String REPOSITORY_PATH = "C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\lpoo-2021-g61";
    private static final String EXTRACTED_METRICS_FILE_PATH = "src/main/java/com/datamining/data/repo_extracted_metrics.csv";

    private static final String BRANCH_NAME = "main";
    private Project project;

    private Map<String, List<String>> changedFiles = new HashMap<>();
//    @Override
//    public void actionPerformed(AnActionEvent anActionEvent) {
//        this.project = anActionEvent.getProject();
//
//        System.out.println("Extracting metrics");
//
//        new RepositoryMetricsExtraction();
//    }
    public static void main(String[] args) {
        System.out.println("Extracting metrics");
        new RepositoryMetricsExtraction();
    }

    public RepositoryMetricsExtraction() {
        try {
            extractMetrics();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void extractMetrics() throws Exception {
        GitService gitService = new GitServiceImpl();
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

        Repository repo = gitService.openRepository(REPOSITORY_PATH);

        Set<String> commits = getCommits();

        List<RefactoringInfo> refactoringInfos = new ArrayList<>();
        for(String commit : commits) {
            //Testing purposes only
            if(refactoringInfos.size() > 1) {
                break;
            }

            try {
                List<RefactoringInfo> temp = refactoringsAtCommit(miner, repo, commit);
                refactoringInfos.addAll(temp);

                //Set the files changes in the commit, so I can find the correct file path later
                if(!temp.isEmpty()){
                    List<String> filesChanged = getFilesChanged(commit);
                    changedFiles.put(commit, filesChanged);
                }
            } catch (Exception e) {
                //I have to use an old implementation of RefactoringMiner to be compatible with the Plugin, so there are unresolved issues
                //Currently ignoring all the commits that provoke an error
                //System.out.println("Error at commits " + commits.get(i).getName() + ":\n" + e.getMessage());
            }
        }

        System.out.println("Refactorings extracted: " + refactoringInfos.size());

        setFilePaths(refactoringInfos);

        File metricsFile = new File(EXTRACTED_METRICS_FILE_PATH);
        FileWriter writer = new FileWriter(metricsFile, false);
        BufferedWriter bufferedWriter = new BufferedWriter(writer);

        for (RefactoringInfo refactoringInfo : refactoringInfos) {
            //String filePath = getFilePath(refactoringInfo);

            System.out.println("\nRefactoring in commit: " + refactoringInfo.getCommitId());
            System.out.println("Full Class name:" + refactoringInfo.getFullClass());
            System.out.println("Class name:" + refactoringInfo.getClassName());
            System.out.println("Method name:" + refactoringInfo.getMethodName());

            //System.out.println("Refactoring: " + refactoringInfo.getMethodName() + " at commit " + refactoringInfo.getCommitId());
            //System.out.println("File path: " + filePath);

            //refactoringInfo.setFilePath(filePath);
            //saveMetrics(refactoringInfo, bufferedWriter);
        }

        repo.close();
    }

    private List<RefactoringInfo> refactoringsAtCommit(GitHistoryRefactoringMiner miner, Repository repo, String commitId) {
        List<RefactoringInfo> refactoringInfos = new ArrayList<>();

        //For some reason, this method receives 2 strings, but only the second one is sued as the commit id (I tested it)
        miner.detectAtCommit(repo, null, commitId, new RefactoringHandler() {
            @Override
            public void handle(String commitId, List<Refactoring> refactorings) {
                for (Refactoring ref : refactorings) {

                    if(ref.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
                        System.out.println(ref);

                        RefactoringInfo refInfo = getRefactoringInfoFromRefactoring(ref);

                        refInfo.setCommitId(commitId);

                        refactoringInfos.add(refInfo);
                    }
                }
            }
        });

        return refactoringInfos;
    }

    private RefactoringInfo getRefactoringInfoFromRefactoring(Refactoring ref) {
        RefactoringInfo refInfo = new RefactoringInfo();

        refInfo.setMethodName(Utils.getMethodName(ref.toString()));

        Pair<String, String> classNames = Utils.getClassName(ref.toString());
        refInfo.setFullClass(classNames.getFirst());
        refInfo.setClassName(classNames.getSecond());

        return refInfo;
    }

    private Set<String> getCommits() throws GitAPIException, IOException {
        Repository repository = Git.open(new File(REPOSITORY_PATH)).getRepository();

        Set<String> commits = new HashSet<>();
        try (Git git = new Git(repository)) {
            Iterable<RevCommit> allCommits = git.log().add(repository.resolve(BRANCH_NAME)).call();

            allCommits.forEach(commit -> commits.add(commit.getName()));
        }

        repository.close();
        return commits;
    }

    private void saveMetrics(RefactoringInfo refInfo, BufferedWriter bufferedWriter) throws IOException {
        Repository repo = Git.open(new File(REPOSITORY_PATH)).getRepository();

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

        File file = new File(REPOSITORY_PATH + "\\" + refInfo.getFilePath().replace(".", "\\"));

        //PsiJavaFile psiFile = Utils.loadFile(file.getAbsolutePath(), this.project);

        //refInfo.setBeforeFile(psiFile);

        //Utils.saveMetricsToFile(bufferedWriter, refInfo, true);
    }

    private void setFilePaths(List<RefactoringInfo> refactoringInfos){
        String filePath = null;

        for(RefactoringInfo refactoringInfo : refactoringInfos){
            boolean found = false;
            for(String file : changedFiles.get(refactoringInfo.getCommitId())){
                if(file.contains(refactoringInfo.getFullClass().replace(".", "/"))){
                    filePath = file;
                    refactoringInfo.setFilePath(filePath);
                    found = true;
                    break;
                }
            }

            if(found) break;

            //If file has multiple classes
            for(String file : changedFiles.get(refactoringInfo.getCommitId())){
                PsiJavaFile psiFile = Utils.loadFile(file, this.project);
                assert psiFile != null;
                for (PsiClass _class : psiFile.getClasses()){
                    if (_class.getName().equals(refactoringInfo.getClassName())){
                        filePath = file;
                        refactoringInfo.setFilePath(filePath);
                        found = true;
                        break;
                    }
                }
            }

            if(!found){
                System.out.println("File not found");
            }

        }

        //List<String> filesChanged = getFilesChanged(refactoringInfo.getCommitId());
    }

    //TODO: Change this because the command isn't working on merges
    List<String> getFilesChanged(String commitId) {
        List<String> filesChanged = new ArrayList<>();
        try {
            //Example: git diff-tree --no-commit-id --name-only 3b20ca83da2b7546989701211f4878efc800b0ec -r
            ProcessBuilder processBuilder = new ProcessBuilder("git", "diff-tree", "--no-commit-id", "--name-only", commitId, "-r");
            processBuilder.redirectErrorStream(true);
            processBuilder.directory(new File(REPOSITORY_PATH));
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            //System.out.println("Files changed in commit " + commitId + ":");
            while ((line = reader.readLine()) != null) {
                //System.out.println(line);
                filesChanged.add(line);
            }

            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return filesChanged;
    }


}
