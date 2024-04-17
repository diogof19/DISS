package com.datamining;

import com.analysis.metrics.MethodMetrics;
import com.core.Pair;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.utils.importantValues.Values;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;

//TODO: Create a 'requirements.txt' folder and function that runs it with pip install
//TODO: Handle python path not set now that it is saved in MySettings
public class PredictionModel {
    private static final String PYTHON_PREDICTION_FILE_PATH = "tmp/prediction.py";
    private static String modelFilePath = "tmp/models/model.joblib";
    private static final String PYTHON_BIAS_FILE_PATH = "tmp/bias_model.py";
    private static final String DATA_FILE_PATH = "tmp/metrics.db";

    public static void main(String[] args) {
//        ArrayList<Double> data = new ArrayList<>();
//
//        // Add 18 test values to the array
//        for (int i = 0; i < 18; i++) {
//            data.add(0.0 + i);
//        }
//
//        try {
//            System.out.println(predictPython(data));
//        } catch (IOException | InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        System.out.println(getCurrentGitAuthor());
    }

    public static void setModelFilePath(String modelFilePath) {
        PredictionModel.modelFilePath = modelFilePath;
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
        command.add(PYTHON_PREDICTION_FILE_PATH);
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
        Set<AuthorInfo> authors = (Set<AuthorInfo>) Values.selectedAuthors;

        String pythonPath = getPythonPath();
        if (pythonPath == null) {
            Utils.popup(Values.event.getProject(),
                    "LiveRef - Python path not set",
                    "Please set the python path by opening the 'Configure Tool' window and going to the 'Advanced Extract Method' tab.",
                    NotificationType.ERROR);
            return;
        }

        ArrayList<String> command = new ArrayList<>();
        command.add(pythonPath);
        command.add(PYTHON_BIAS_FILE_PATH);
        command.add(modelFilePath);
        command.add(DATA_FILE_PATH);

        for (AuthorInfo author : authors) {
            command.add(author.toString());
        }

        //If there are no authors, add this string so the python script knows to add no bias
        if(authors.isEmpty())
            command.add("no_bias");

        pythonScriptRun(command);

        Utils.popup(Values.event.getProject(),
                "LiveRef",
                "Model successfully updated.",
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

    /**
     * Possible updates the model with the new method metrics
     * @param methodMetrics the metrics of the method
     * @param project the project
     * @throws IOException if the python script has a problem
     * @throws InterruptedException if the process is interrupted
     */
    public static void updateModel(MethodMetrics methodMetrics, Project project) throws IOException, InterruptedException, SQLException {
        Pair<String, String> author = getCurrentGitAuthor();
        if (author == null) {
            Utils.popup(project,
                    "LiveRef - Git author not found",
                    "The current git author could not be found. Please make sure you have git installed and configured.",
                    NotificationType.WARNING);
        }

        Database.saveMetrics(author, methodMetrics, null);

        MySettings mySettings = project.getService(MySettings.class);
        mySettings.getState().counter++;

        if(mySettings.getState().counter >= Values.maxExtractMethodsBefUpdate) {
            biasModel();
            mySettings.getState().counter = 0;
        }

    }

    /**
     * Gets the current git author
     * @return the current git author in the format "name (email)" or null if it is not found
     */
    private static Pair<String, String> getCurrentGitAuthor() {
        try {
            String email = runCommand("git config user.email");
            String name = runCommand("git config user.name");
            return new Pair<>(name, email);
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }

    /**
     * Runs a command in the terminal
     * @param command the command to run
     * @return the output of the command
     * @throws IOException if the command has a problem
     * @throws InterruptedException if the process is interrupted
     */
    private static String runCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder output = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            output.append(line);
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            return output.toString().trim();
        } else {
            System.err.println("Error executing command: " + command);
        }

        return output.toString();
    }

    public static boolean checkPipRequirements() throws IOException, InterruptedException {
        String pythonPath = getPythonPath();
        if (pythonPath == null) {
            Utils.popup(Values.event.getProject(),
                    "LiveRef - Python path not set",
                    "Cannot check pip requirements without python path. Please set the python path by opening the 'Configure Tool' window and going to the 'Advanced Extract Method' tab.",
                    NotificationType.ERROR);
            return false;
        }

        URL url = PredictionModel.class.getResource("/requirements.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            if(!isPythonPackagedInstalled(pythonPath, line))
                return false;
        }

        return true;
    }

    private static boolean isPythonPackagedInstalled(String pythonPath, String packageName) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(pythonPath, "-c",
                "import " + packageName).start();

        int exitCode = process.waitFor();

        return exitCode == 0;
    }

    public static void installPipRequirements() throws IOException, InterruptedException {
        String pythonPath = getPythonPath();
        if (pythonPath == null) {
            Utils.popup(Values.event.getProject(),
                    "LiveRef - Python path not set",
                    "Cannot install pip requirements without python path. Please set the python path by opening the 'Configure Tool' window and going to the 'Advanced Extract Method' tab.",
                    NotificationType.ERROR);
            return;
        }

        runCommand(pythonPath + " -m pip install -r tmp/requirements.txt");
    }
}


