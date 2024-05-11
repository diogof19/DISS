package com.datamining;

import com.analysis.metrics.MethodMetrics;
import com.core.Pair;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.utils.importantValues.Values;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;

//TODO: Create a 'requirements.txt' folder and function that runs it with pip install
//TODO: Handle python path not set now that it is saved in MySettings
public class PredictionModel {
    private static final String PYTHON_PREDICTION_FILE_PATH = Values.dataFolder + "python/prediction.py";
    private static final String PYTHON_BIAS_FILE_PATH = Values.dataFolder + "python/bias_model.py";
    private static final String DATA_FILE_PATH = Values.dataFolder + "metrics.db";
    private static final String SCALER_FILE_PATH = Values.dataFolder + "python/scaler.pkl";

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


    /* PREDICTION */

    /**
     * Predicts if a method is an outlier or not
     * @param methodMetrics the metrics of the method
     * @return true if the method is an inlier, false if it is an outlier
     */
    public static boolean predict(MethodMetrics methodMetrics, Project project){
        ArrayList<Double> data = getMetrics(methodMetrics);

        try {
            System.out.println("Starting Prediction");
            boolean prediction = predictPython(data, project);
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
    private static boolean predictPython(ArrayList<Double> data, Project project) throws IOException, InterruptedException {
        String pythonPath = getPythonPath(project);
        if (pythonPath == null)
            return false;

        String modelFilePath = Database.getSelectedModelFilePath();
        File modelFile = new File(Values.dataFolder + modelFilePath);
        File scalerFile = new File(SCALER_FILE_PATH);

        ArrayList<String> command = new ArrayList<>();
        command.add(pythonPath);
        command.add(PYTHON_PREDICTION_FILE_PATH);
        command.add(modelFile.getAbsolutePath());
        command.add(scalerFile.getAbsolutePath());
        for (Double value : data) {
            command.add(value.toString());
        }

        String output = pythonScriptRun(command);

        float result = Float.parseFloat(output);

        // Result will be -1 for outliers or 1 for inliers
        return result == 1;
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


    /* BIASING/UPDATING MODEL */

    /**
     * Creates the command to bias the model for the selected authors
     * @param project the project
     * @param pythonPath the path of the python executable
     * @param oldModelPath the path of the model to load settings from
     * @param newModelPath the path to save the new model (if the same as the old model, it will be updated)
     * @param authors the authors to bias the model with (if empty, no bias is applied)
     * @return the command to bias the model
     */
    public static ArrayList<String> biasModelCommand(Project project, String pythonPath, String oldModelPath, String newModelPath, Set<AuthorInfo> authors) {
        ArrayList<String> command = new ArrayList<>();

        File scalerFile = new File(SCALER_FILE_PATH);
        MySettings mySettings = project.getService(MySettings.class);

        command.add(pythonPath);
        command.add(PYTHON_BIAS_FILE_PATH);
        command.add(oldModelPath);
        command.add(newModelPath);
        command.add(scalerFile.getAbsolutePath());
        command.add(DATA_FILE_PATH);
        command.add(String.valueOf(mySettings.getState().biasMultiplier));

        if(authors.isEmpty())
            command.add("no_bias");
        else {
            for (AuthorInfo author : authors) {
                command.add(author.toString());
            }
        }

        return command;
    }

    /**
     * Uses the python script to bias the model for the selected authors
     * @throws IOException if the python script has a problem
     * @throws InterruptedException if the process is interrupted
     */
    public static void biasModel(Project project, String modelName) throws IOException, InterruptedException {
        String pythonPath = getPythonPath(project);
        if (pythonPath == null)
            return;

        Pair<String, String> modelInfo;
        if (modelName == null) {
            modelInfo = Database.getSelectedModel();
        } else {
            String path = Database.getModelPathByName(modelName);
            modelInfo = new Pair<>(modelName, path);
        }

        File modelFile = new File(Values.dataFolder + modelInfo.getSecond());
        Set<AuthorInfo> authors = Database.getSelectedAuthorsPerModel(modelInfo.getFirst());

        ArrayList<String> command = biasModelCommand(project, pythonPath, modelFile.getAbsolutePath(), modelFile.getAbsolutePath(), authors);

        pythonScriptRun(command);

        Utils.popup(project,
                "LiveRef",
                "Model successfully updated.",
                NotificationType.INFORMATION);

        project.getService(MySettings.class).getState().counter = 0;
    }

    /**
     * Creates a new model biased with the selected authors
     * @param project the project
     * @param path the path of the new model
     * @param authors the authors to bias the model with (if empty, no bias is applied)
     */
    public static void createModel(Project project, String path, Set<AuthorInfo> authors) {
        String pythonPath = getPythonPath(project);
        if (pythonPath == null)
            return;

        Pair<String, String> oldModelInfo = Database.getSelectedModel();
        File oldModelFile = new File(Values.dataFolder + oldModelInfo.getSecond());

        File newModelFile = new File(Values.dataFolder + path);

        ArrayList<String> command = biasModelCommand(project, pythonPath, oldModelFile.getAbsolutePath(), newModelFile.getAbsolutePath(), authors);

        try {
            pythonScriptRun(command);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

//        Utils.popup(project,
//                "LiveRef",
//                "Profile successfully created.",
//                NotificationType.INFORMATION);

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
            author = new Pair<>("hello", "hello@g.com");
        }

        Database.saveMethodMetrics(author, methodMetrics, null);

        MySettings.State state = project.getService(MySettings.class).getState();
        state.counter++;

        if(state.counter >= state.maxExtractMethodsBefUpdate) {
            System.out.println("Biasing model");
            biasModel(project, null);
            System.out.println("Model biased");
            state.counter = 0;
        }

    }

    /**
     * Deletes the model at the given path
     * @param model the name of the model
     */
    public static void deleteModel(Project project, String model) throws IOException, InterruptedException {
        String path = Database.getModelPathByName(model);
        File modelFile = new File(Values.dataFolder + path);
        modelFile.delete();

        boolean wasSelected = Database.getSelectedModel().getFirst().equals(model);

        Database.deleteModel(model);

        if (wasSelected) {
            String newSelected = Database.getAnyModelName();
            Database.setSelectedModel(newSelected);
            PredictionModel.biasModel(project, newSelected);
        }
    }


    /* PIP REQUIREMENTS */

    /**
     * Checks if the new python path is valid and if the pip requirements are installed (if not, installs them)
     * @param project the project
     * @param pythonPath the new python path
     * @throws IOException if the python script has a problem
     * @throws InterruptedException if the process is interrupted
     */
    public static void checkPipRequirements(Project project, String pythonPath) throws IOException, InterruptedException {
        if(!isPythonPathValid(pythonPath)){
            Utils.popup(project,
                    "LiveRef - Python path not valid",
                    "Please check if the path to the python executable is correct.",
                    NotificationType.ERROR);
            return;
        }

        File requirementsFile = new File(Values.dataFolder + "requirements.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(requirementsFile.toURI().toURL().openStream()));

        ArrayList<String> installedRequirements = getPipRequirements(pythonPath);

        boolean requirementsInstalled = true;
        String line;
        while ((line = reader.readLine()) != null) {
            if(!installedRequirements.contains(line.split("==")[0])){
                System.out.println("Requirement not installed: " + line.split("==")[0]);
                requirementsInstalled = false;
                break;
            }
        }


        if (!requirementsInstalled) {
            ArrayList<String> command = new ArrayList<>();
            command.add("\"" + pythonPath + "\"");
            command.add("-m");
            command.add("pip");
            command.add("install");
            command.add("-r");
            command.add(requirementsFile.getAbsolutePath());
            pythonScriptRun(command);
        }

        Utils.popup(project,
                "LiveRef",
                "Python path update & all requirements are installed.",
                NotificationType.INFORMATION);
    }

    /**
     * Checks if the python path is valid by running the python version command
     * @param pythonPath the python path
     * @return true if the python path is valid, false otherwise
     */
    public static boolean isPythonPathValid(String pythonPath) {
        if (pythonPath == null)
            return false;

        File pythonExe = new File(pythonPath);
        if (!pythonExe.exists() || !pythonExe.isFile())
            return false;

        try {
            Process process = Runtime.getRuntime().exec(pythonPath + " --version");
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets the installed pip requirements
     * @param pythonPath the python path
     * @return the installed pip requirements
     */
    private static ArrayList<String> getPipRequirements(String pythonPath) {
        ArrayList<String> requirements = new ArrayList<>();
        try {
            Process process = new ProcessBuilder(pythonPath, "-m", "pip", "freeze").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                requirements.add(line.split("==")[0]);
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return requirements;
    }


    /* UTILS */

    /**
     * Gets the python path from the settings
     * @return the python path or null if it is not set
     */
    private static String getPythonPath(Project project) {
        MySettings mySettings = project.getService(MySettings.class);
        String pythonPath = mySettings.getState().pythonPath;

        if (pythonPath.isEmpty()) {
            Utils.popup(project,
                    "LiveRef - Python path not set",
                    "Please set the python path by opening the 'Configure Tool' window and going to the 'Advanced Extract Method' tab.",
                    NotificationType.ERROR);

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

    //TODO: Test when the plugin is actually installed
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
}


