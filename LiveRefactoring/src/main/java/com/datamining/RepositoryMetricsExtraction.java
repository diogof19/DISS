package com.datamining;

import com.core.Pair;
import com.intellij.psi.PsiJavaFile;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.*;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//TODO: Give credits to RefactoringMiner
//      https://github.com/tsantalis/RefactoringMiner?tab=readme-ov-file#how-to-cite-refactoringminer
public class RepositoryMetricsExtraction {
    private static String REPOSITORY_PATH = "C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\lpoo-2021-g61";
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
                        System.out.println("\n\n\nExtract operation refactoring\n\n\n");

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
                    System.out.println(ref.toString());
                }
            }
        });

        try {
            for (RefactoringInfo refactoringInfo : refactoringInfos) {
                getMetrics(refactoringInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private RefactoringInfo getRefactoringInfoFromRefactoring(Refactoring ref) {
        RefactoringInfo refInfo = new RefactoringInfo();

        refInfo.setMethodName(Utils.getMethodName(ref.toString()));

        Pair<String, String> classNames = Utils.getClassName(ref.toString());
        refInfo.setFullClass(classNames.getFirst());
        refInfo.setClassName(classNames.getSecond());

        return refInfo;
    }

    private void getMetrics(RefactoringInfo refInfo) throws IOException, GitAPIException {
        Repository repo = Git.open(new File(REPOSITORY_PATH)).getRepository();

        Git git = new Git(repo);

        git.checkout().setName(refInfo.getCommitId()).call();

        File file = new File(REPOSITORY_PATH + "\\" + refInfo.getFilePath().replace(".", "\\"));

        PsiJavaFile beforeFile = Utils.loadFile(file.getAbsolutePath(), null);
    }

}
