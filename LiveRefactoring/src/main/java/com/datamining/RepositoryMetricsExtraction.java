package com.datamining;

import com.core.Pair;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.refactoringminer.api.*;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//TODO: Give credits to RefactoringMiner
//      https://github.com/tsantalis/RefactoringMiner?tab=readme-ov-file#how-to-cite-refactoringminer
public class RepositoryMetricsExtraction extends AnAction {
    private static final String REPOSITORY_PATH = "C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\lpoo-2021-g61";
    private static final String EXTRACTED_METRICS_FILE_PATH = "src/main/java/com/datamining/data/repo_extracted_metrics.csv";
    private Project project;
    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        this.project = anActionEvent.getProject();

        System.out.println("Extracting metrics");
        try {
            new RepositoryMetricsExtraction();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        System.out.println("Extracting metrics");
        try {
            new RepositoryMetricsExtraction();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RepositoryMetricsExtraction() throws Exception {
        extractMetrics();
    }

    private void extractMetrics() throws Exception {
        GitService gitService = new GitServiceImpl();
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

        Repository repo = gitService.openRepository(REPOSITORY_PATH);

        List<RefactoringInfo> refactoringInfos = new ArrayList<>();
        miner.detectAll(repo, "main", new RefactoringHandler() {
            @Override
            public void handle(String commitId, List<Refactoring> refactorings) {
                System.out.println("Refactorings at " + commitId);
                for (Refactoring ref : refactorings) {
                    //Example ref:
                    // Extract Method	public execute(command Command<MovableElement>) : void extracted from public redo() : void in class com.lpoo_2021_g61.Model.Elements.MovableElement
                    if(ref.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)){
                        System.out.println("\n\n\nExtract operation refactoring");
                        System.out.println(ref+"\n\n\n");

                        //Only one class is involved in this refactoring
                        Set<ImmutablePair<String, String>> classes = ref.getInvolvedClassesBeforeRefactoring();

                        String filePath = classes.stream().findFirst().map(ImmutablePair::getLeft).orElse(null);
                        if (filePath == null) {
                            System.out.println("File path is null");
                            continue;
                        }

                        RefactoringInfo refInfo = getRefactoringInfoFromRefactoring(ref);
                        refInfo.setCommitId(commitId);
                        refInfo.setFilePath(filePath);

                        refactoringInfos.add(refInfo);

                    }
                }
            }
        });

        File metricsFile = new File(EXTRACTED_METRICS_FILE_PATH);
        FileWriter writer = new FileWriter(metricsFile, false);
        BufferedWriter bufferedWriter = new BufferedWriter(writer);

        try {
            for (RefactoringInfo refactoringInfo : refactoringInfos) {
                saveMetrics(refactoringInfo, bufferedWriter);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        bufferedWriter.close();
    }

    private RefactoringInfo getRefactoringInfoFromRefactoring(Refactoring ref) {
        RefactoringInfo refInfo = new RefactoringInfo();

        refInfo.setMethodName(Utils.getMethodName(ref.toString()));

        Pair<String, String> classNames = Utils.getClassName(ref.toString());
        refInfo.setFullClass(classNames.getFirst());
        refInfo.setClassName(classNames.getSecond());

        return refInfo;
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

        PsiJavaFile psiFile = Utils.loadFile(file.getAbsolutePath(), this.project);

        refInfo.setBeforeFile(psiFile);

        Utils.saveMetricsToFile(bufferedWriter, refInfo, true);
    }

}
