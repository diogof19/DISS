package com.datamining;

import com.analysis.metrics.MethodMetrics;
import com.core.Pair;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.VcsDirtyScope;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.utils.importantValues.Values;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;

import static com.datamining.Utils.extractFile;

//TODO: Create a 'requirements.txt' folder and function that runs it with pip install
//TODO: Add a pop-up when python path is not set
public class PredictionModel {
    private static String pythonPredictionFilePath;
    private static String modelFilePath;
    private static String pythonBiasFilePath;
    private static String dataFilePath;
    public static void main(String[] args) {
        ArrayList<Double> data = new ArrayList<>();

        // Add 18 test values to the array
        for (int i = 0; i < 18; i++) {
            data.add(0.0 + i);
        }

        try {
            System.out.println(predictPython(data));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Predicts if a method is an outlier or not
     * @param methodMetrics the metrics of the method
     * @return true if the method is an inlier, false if it is an outlier
     */
    public static boolean predict(MethodMetrics methodMetrics){
        ArrayList<Double> data = getMetrics(methodMetrics);

        try {
            System.out.println("Starting Prediction");
            boolean prediction = predictPython(data);
            System.out.println("Prediction done: " + prediction);
            return prediction;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Uses the python script to predict if a method is an outlier or not
     * @param data the data to be used for the prediction
     * @return true if the method is an inlier, false if it is an outlier
     * @throws IOException if the python script has a problem
     * @throws InterruptedException if the process is interrupted
     */
    private static boolean predictPython(ArrayList<Double> data) throws IOException, InterruptedException {
        if(pythonPredictionFilePath == null || pythonPredictionFilePath.isEmpty())
            pythonPredictionFilePath = extractFile("prediction.py");

        if(modelFilePath == null || modelFilePath.isEmpty())
            modelFilePath = extractFile("model.joblib");

        String pythonPath = getPythonPath();
        if (pythonPath == null) {
            Utils.popup(Values.event.getProject(),
                    "LiveRef - Python path not set",
                    "Extract Method Refactoring won't work.Please set the python path by opening the 'Configure Tool' window and going to the 'Advanced Extract Method' tab.",
                    NotificationType.ERROR);
            return false;
        }

        ArrayList<String> command = new ArrayList<>();
        command.add(pythonPath);
        command.add(pythonPredictionFilePath);
        command.add(modelFilePath);
        for (Double value : data) {
            command.add(value.toString());
        }

        String output = pythonScriptRun(command);

        float result = Float.parseFloat(output);

        // Result will be -1 for outliers or 1 for inliers
        return result == 1;
    }

    /**
     * Uses the python script to bias the model for the selected authors
     * @throws IOException if the python script has a problem
     * @throws InterruptedException if the process is interrupted
     */
    public static void biasModel() throws IOException, InterruptedException {
        //TODO: Test better

        if(pythonBiasFilePath == null || pythonBiasFilePath.isEmpty())
            pythonBiasFilePath = extractFile("bias.py");

        if(modelFilePath == null || modelFilePath.isEmpty())
            modelFilePath = extractFile("model.joblib");

        if(dataFilePath == null || dataFilePath.isEmpty())
            dataFilePath = extractFile("extracted_metrics.csv");

        Set<AuthorInfo> authors = Values.selectedAuthors;

        String pythonPath = getPythonPath();
        if (pythonPath == null) {
            Utils.popup(Values.event.getProject(),
                    "LiveRef - Python path not set",
                    "Model bias won't work. Please set the python path by opening the 'Configure Tool' window and going to the 'Advanced Extract Method' tab.",
                    NotificationType.ERROR);
            return;
        }

        ArrayList<String> command = new ArrayList<>();
        command.add(pythonPath);
        command.add(pythonBiasFilePath);
        command.add(modelFilePath);
        command.add(dataFilePath);

        for (AuthorInfo author : authors) {
            command.add(author.toString());
        }

        //If there are no authors, add this string so the python script knows to add no bias
        if(authors.isEmpty())
            command.add("no_bias");

        pythonScriptRun(command);

        Utils.popup(Values.event.getProject(),
                "LiveRef - Model Bias",
                "Model bias has been applied for the selected authors.",
                NotificationType.INFORMATION);
    }

    /**
     * Gets the python path from the settings
     * @return the python path or null if it is not set
     */
    private static String getPythonPath() {
        String pythonPath = Values.pythonPath;

        if (pythonPath.isEmpty()) {
            //TODO: Removed this after testing
            pythonPath = "C:\\Program Files\\Python310\\python.exe";
            return null;
        }

        return pythonPath;
    }

    /**
     * Runs a python script with the given command
     * @param command the command to run the python script
     * @return the output of the python script
     * @throws IOException if the python script has a problem
     * @throws InterruptedException if the process is interrupted
     */
    private static @NotNull String pythonScriptRun(ArrayList<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

        BufferedReader stdOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

        StringBuilder output = new StringBuilder();
        String s;
        while ((s = stdOutput.readLine()) != null) {
            output.append(s);
        }

        int returnCode = process.waitFor();
        if (returnCode != 0 || output.toString().contains("Warning")) {
            throw new IOException("Python script problem: " + returnCode + "\n" + output);
        }

        stdOutput.close();

        return output.toString();
    }

    /**
     * Gets the required metrics for the prediction model from the method metrics
     * @param methodMetrics the metrics of the method
     * @return the metrics required for the prediction model
     */
    private static ArrayList<Double> getMetrics(MethodMetrics methodMetrics) {
        ArrayList<Double> data = new ArrayList<>();

        data.add((double) methodMetrics.numberLinesOfCode);
        data.add((double) methodMetrics.numberComments);
        data.add((double) methodMetrics.numberBlankLines);
        data.add((double) methodMetrics.numberLinesOfCode + methodMetrics.numberComments +
                methodMetrics.numberBlankLines);
        data.add((double) methodMetrics.numParameters);
        data.add((double) methodMetrics.numberOfStatements);
        data.add(methodMetrics.halsteadLength);
        data.add(methodMetrics.halsteadVocabulary);
        data.add(methodMetrics.halsteadVolume);
        data.add(methodMetrics.halsteadDifficulty);
        data.add(methodMetrics.halsteadEffort);
        data.add(methodMetrics.halsteadLevel);
        data.add(methodMetrics.halsteadTime);
        data.add(methodMetrics.halsteadBugsDelivered);
        data.add(methodMetrics.halsteadMaintainability);
        data.add((double) methodMetrics.complexityOfMethod);
        data.add((double) methodMetrics.cognitiveComplexity);
        data.add(methodMetrics.lackOfCohesionInMethod);

        return data;
    }

    public static void updateModel(MethodMetrics methodMetrics, Project project) {
        //TODO: How do I get the author?
        //TODO: Actually update the model - incremental training?

        ArrayList<Double> data = getMetrics(methodMetrics);
    }

    private static String getCurrentGitAuthor(Project project) {
        String author;

        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);

        if (vcsManager.checkVcsIsActive("Git")) {
            try {
                //Get user
                GitVcs gitVcs = (GitVcs) vcsManager.findVcsByName("Git");

                GitRepositoryManager repositoryManager = GitUtil.getRepositoryManager(project);

                VcsDirtyScopeManager dirtyScopeManager = VcsDirtyScopeManager.getInstance(project);
                VcsDirtyScope dirtyScope = dirtyScopeManager.


            } catch (VcsException e) {
                e.printStackTrace();
            }
        }

        return author;
    }
}


