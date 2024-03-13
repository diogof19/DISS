package com.datamining;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

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

        new RepositoryMetricsExtraction();
    }
    public static void main(String[] args) {
        System.out.println("Extracting metrics");
        new RepositoryMetricsExtraction();
    }

    public RepositoryMetricsExtraction() {
        extractMetrics();
    }

    private void extractMetrics() {

    }

}
