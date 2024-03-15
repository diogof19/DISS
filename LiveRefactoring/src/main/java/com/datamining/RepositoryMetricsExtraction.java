package com.datamining;

import com.core.Pair;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
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

//TODO: Maybe I need to save the branch and commit the repo currently is, so I can return to it after the extraction
public class RepositoryMetricsExtraction extends AnAction {
//public class RepositoryMetricsExtraction {
    private static final String REPOSITORY_PATH = "C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\lpoo-2021-g61";
    private static final String EXTRACTED_METRICS_FILE_PATH = "tmp/repo_extracted_metrics.csv";
    private static final String BRANCH_NAME = "main";
    private Project project;
    private Map<String, Set<String>> changedFiles = new HashMap<>();
    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        this.project = anActionEvent.getProject();

        System.out.println("Extracting metrics");

        try {
            extractMetrics();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
//    public static void main(String[] args) {
//        System.out.println("Extracting metrics");
//        new RepositoryMetricsExtraction();
//    }
//
//    public RepositoryMetricsExtraction() {
//        System.out.println("\n\nRunning this\n\n");
//        try {
//            extractMetrics();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    private void extractMetrics() throws Exception {
        GitService gitService = new GitServiceImpl();
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

        Repository repo = gitService.openRepository(REPOSITORY_PATH);

        Set<String> commits = getCommits();

        Set<RefactoringInfo> refactoringInfos = new HashSet<>();
        //TODO: add threads to this for loop - just for this part
        for(String commit : commits) {
            //Testing purposes only
//            if(refactoringInfos.size() > 1) {
//                break;
//            }

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
                //System.out.println("Error at commits " + commits.get(i).getName() + ":\n" + e.getMessage());
            }
        }

        System.out.println("Refactorings extracted: " + refactoringInfos.size());

        File tmpFolder = new File("tmp");
        if(!tmpFolder.exists()) {
            tmpFolder.mkdir();
        }

        File metricsFile = new File(EXTRACTED_METRICS_FILE_PATH);
        if(!metricsFile.exists()) {
            metricsFile.createNewFile();
        }

        System.out.println("Metrics file path: " + metricsFile.getAbsolutePath());

        FileWriter writer = new FileWriter(metricsFile.getAbsolutePath(), false);
        BufferedWriter bufferedWriter = new BufferedWriter(writer);

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

    private void saveMetrics(RefactoringInfo refInfo, BufferedWriter bufferedWriter) throws IOException, GitAPIException, InterruptedException {
        Git git = Git.open(new File(REPOSITORY_PATH));

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

        //checkout to the correct commit
        git.checkout().setName(previousCommit.getName()).call();

        revWalk.close();
        repo.close();
        git.close();

        System.out.println("Checkout to commit: " + previousCommit.getName());

        String temp = refInfo.getFilePath().replace(".java", "").replace("\\", ".");
        String filePath = REPOSITORY_PATH + "\\" + temp + ".java";

        System.out.println("File path: " + filePath);

        PsiJavaFile psiFile = Utils.loadFile(filePath, this.project);

        refInfo.setBeforeFile(psiFile);

        Utils.saveMetricsToFile(bufferedWriter, refInfo, true);
    }

    private String getFilePath(RefactoringInfo refactoringInfo){
        Set<String> filesChanged = changedFiles.get(refactoringInfo.getCommitId());

        if (filesChanged.size() == 1){
            return filesChanged.iterator().next();
        }

        //TODO: Change this
        for(String file : filesChanged){
            if(file.contains("/" + refactoringInfo.getFullClass().replace(".", "/") + "/")){
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

    Set<String> getFilesChanged(String commitId) {
        Set<String> filesChanged = new HashSet<>();
        try {
            //Example: git diff-tree --no-commit-id --name-only 3b20ca83da2b7546989701211f4878efc800b0ec -r
            //Example: git log -m -1 --name-only --pretty="format:" 38983ec445339ed03efe389c53cc0bb6861f30f2
            ProcessBuilder processBuilder = new ProcessBuilder("git", "log", "-m", "-1", "--name-only", "--pretty=\"format:\"", commitId);
            processBuilder.redirectErrorStream(true);
            processBuilder.directory(new File(REPOSITORY_PATH));
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
