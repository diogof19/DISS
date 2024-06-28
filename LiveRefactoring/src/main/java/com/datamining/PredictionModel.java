package com.datamining;

import com.analysis.metrics.ClassMetrics;
import com.analysis.metrics.MethodMetrics;
import com.core.Pair;
import com.core.Refactorings;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.utils.importantValues.Values;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;

public class PredictionModel {
    private static final String PYTHON_PREDICTION_FILE_PATH = Values.dataFolder + "python/prediction.py";
    private static final String PYTHON_BIAS_FILE_PATH = Values.dataFolder + "python/bias_model.py";
    private static final String DATA_FILE_PATH = Values.dataFolder + "metrics.db";
    private static final String EM_SCALER_FILE_PATH = Values.dataFolder + "python/scalerEM.pkl";
    private static final String EC_SCALER_FILE_PATH = Values.dataFolder + "python/scalerEC.pkl";

    /* PREDICTION */

    /**
     * Predicts if a method is an outlier or not
     * @param methodMetrics the metrics of the method
     * @return true if the method is an inlier, false if it is an outlier
     */
    public static boolean predictEM(MethodMetrics methodMetrics, Project project){
        ArrayList<Double> data = getMethodMetrics(methodMetrics);
        if(data.contains(null))
            return false;

        try {
            System.out.println("Starting EM Prediction");
            boolean prediction = predictPython(data, project, Refactorings.ExtractMethod);
            System.out.println("EM Prediction done: " + prediction);
            return prediction;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the required metrics for the prediction model from the method metrics
     * @param methodMetrics the metrics of the method
     * @return the metrics required for the prediction model
     */
    private static ArrayList<Double> getMethodMetrics(MethodMetrics methodMetrics) {
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
     * Predicts if a class is an outlier or not
     * @param classMetrics the metrics of the class
     * @param project the project
     * @return true if the class is an inlier, false if it is an outlier
     */
    public static boolean predictEC(ClassMetrics classMetrics, Project project){
        ArrayList<Double> data = getClassMetrics(classMetrics);
        if (data.contains(null))
            return false;

        try {
            System.out.println("Starting EC Prediction");
            boolean prediction = predictPython(data, project, Refactorings.ExtractClass);
            System.out.println("EC Prediction done: " + prediction);
            return prediction;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the required metrics for the prediction model from the class metrics
     * @param classMetrics the metrics of the class
     * @return the metrics required for the prediction model
     */
    private static ArrayList<Double> getClassMetrics(ClassMetrics classMetrics){
        ArrayList<Double> data = new ArrayList<>();

        data.add((double) classMetrics.numProperties);
        data.add((double) classMetrics.numPublicAttributes);
        data.add((double) classMetrics.numPublicMethods);
        data.add((double) classMetrics.numProtectedFields);
        data.add((double) classMetrics.numProtectedMethods);
        data.add((double) classMetrics.numLongMethods);
        data.add((double) classMetrics.numLinesCode);
        data.add(classMetrics.lackOfCohesion);
        data.add(classMetrics.complexity);
        data.add(classMetrics.cognitiveComplexity);
        data.add((double) classMetrics.numMethods);
        data.add((double) classMetrics.numConstructors);
        data.add(classMetrics.halsteadLength);
        data.add(classMetrics.halsteadVocabulary);
        data.add(classMetrics.halsteadVolume);
        data.add(classMetrics.halsteadDifficulty);
        data.add(classMetrics.halsteadEffort);
        data.add(classMetrics.halsteadLevel);
        data.add(classMetrics.halsteadTime);
        data.add(classMetrics.halsteadBugsDelivered);
        data.add(classMetrics.halsteadMaintainability);

        return data;
    }

    /**
     * Uses the python script to predict if a method or class is an outlier or not
     * @param data the data to be used for the prediction
     * @param project the project
     * @param type the type of model to use
     * @return true if the method is an inlier, false if it is an outlier
     * @throws IOException if the python script has a problem
     * @throws InterruptedException if the process is interrupted
     */
    private static boolean predictPython(ArrayList<Double> data, Project project, Refactorings type) throws IOException, InterruptedException {
        String pythonPath = getPythonPath(project);
        if (pythonPath == null)
            return false;

        ModelInfo modelInfo = Database.getSelectedModel();
        File scalerFile;
        String modelPath;
        if(type.equals(Refactorings.ExtractMethod)) {
            scalerFile = new File(EM_SCALER_FILE_PATH);
            modelPath = modelInfo.getPathEM();
            //System.out.println("Model path: " + modelPath);
        }
        else {
            scalerFile = new File(EC_SCALER_FILE_PATH);
            modelPath = modelInfo.getPathEC();
        }

        ArrayList<String> command = new ArrayList<>();
        command.add(pythonPath);
        command.add(PYTHON_PREDICTION_FILE_PATH);
        command.add(modelPath);
        command.add(scalerFile.getAbsolutePath());
        for (Double value : data) {
            command.add(value.toString());
        }

        String output = pythonScriptRun(command, null);

        float result = Float.parseFloat(output);

        // Result will be -1 for outliers or 1 for inliers
        return result == 1;
    }


    /* BIASING/UPDATING MODEL */

    /**
     * Creates the command to bias the model for the selected authors
     * @param project the project
     * @param pythonPath the path of the python executable
     * @param oldModel the information of the old model
     * @param newModel the information of the new model
     * @param authors the authors to bias the model with (if empty, no bias is applied)
     * @return the command to bias the model
     */
    public static ArrayList<String> biasModelCommand(Project project, String pythonPath, ModelInfo oldModel, ModelInfo newModel, Set<AuthorInfo> authors) {
        ArrayList<String> command = new ArrayList<>();

        MySettings mySettings = project.getService(MySettings.class);

        command.add(pythonPath);
        command.add(PYTHON_BIAS_FILE_PATH);
        command.add(oldModel.getPathEM());
        command.add(newModel.getPathEM());
        command.add(EM_SCALER_FILE_PATH);
        command.add(oldModel.getPathEC());
        command.add(newModel.getPathEC());
        command.add(EC_SCALER_FILE_PATH);
        command.add(DATA_FILE_PATH);
        command.add(String.valueOf(mySettings.getState().biasMultiplier));

        if(authors.isEmpty())
            command.add("no_bias");
        else {
            for (AuthorInfo author : authors) {
                command.add(Integer.toString(author.getId()));
            }
        }

        return command;
    }

    /**
     * Uses the python script to bias the model for the selected authors
     * @param project the project
     * @param modelInfo the information of the model
     * @throws IOException if the python script has a problem
     * @throws InterruptedException if the process is interrupted
     */
    public static void biasModel(Project project, ModelInfo modelInfo) throws IOException, InterruptedException {
        String pythonPath = getPythonPath(project);
        if (pythonPath == null)
            return;

        if(modelInfo == null)
            modelInfo = Database.getSelectedModel();

        Set<AuthorInfo> authors = Database.getSelectedAuthorsPerModel(modelInfo.getName());

        ArrayList<String> command = biasModelCommand(project, pythonPath, modelInfo, modelInfo, authors);

        pythonScriptRun(command, null);

        Utils.popup(project,
                "LiveRef",
                "Model successfully updated.",
                NotificationType.INFORMATION);

        project.getService(MySettings.class).getState().counter = 0;
    }

    /**
     * Creates a new model biased with the selected authors
     * @param project the project
     * @param newModelInfo the information of the new model
     * @param authors the authors to bias the model with (if empty, no bias is applied)
     */
    public static void createModel(Project project, ModelInfo newModelInfo, Set<AuthorInfo> authors) {
        String pythonPath = getPythonPath(project);
        if (pythonPath == null)
            return;

        ModelInfo oldModelInfo = Database.getSelectedModel();

        ArrayList<String> command = biasModelCommand(project, pythonPath, oldModelInfo, newModelInfo, authors);

        try {
            pythonScriptRun(command, null);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Possible updates the model with the new method metrics
     * @param methodMetrics the metrics of the method
     * @param project the project
     * @throws IOException if the python script has a problem
     * @throws InterruptedException if the process is interrupted
     */
    public static void updateEMModel(MethodMetrics methodMetrics, Project project) throws IOException, InterruptedException {
        Pair<String, String> author = getCurrentGitAuthor(project);
        if (author == null) {
            Utils.popup(project,
                    "LiveRef - Git author not found",
                    "The current git author could not be found. Please make sure you have git installed and configured.",
                    NotificationType.WARNING);
        }

        Database.saveMethodMetrics(author, methodMetrics, null);

        MySettings.State state = project.getService(MySettings.class).getState();
        state.counter++;

        if(state.counter >= state.maxRefactoringsBefUpdate) {
            System.out.println("Biasing model");
            biasModel(project, null);
            System.out.println("Model biased");
            state.counter = 0;
        }

    }

    public static void updateECModel(ClassMetrics classMetrics, Project project) throws IOException, InterruptedException {
        Pair<String, String> author = getCurrentGitAuthor(project);
        if (author == null) {
            Utils.popup(project,
                    "LiveRef - Git author not found",
                    "The current git author could not be found. Please make sure you have git installed and configured.",
                    NotificationType.WARNING);
        }

        Database.saveClassMetrics(author, classMetrics, null);

        MySettings.State state = project.getService(MySettings.class).getState();
        state.counter++;

        if(state.counter >= state.maxRefactoringsBefUpdate) {
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
        ModelInfo modelInfo = Database.getModelByName(model);
        File fileEM = new File(modelInfo.getPathEM());
        fileEM.delete();
        File fileEC = new File(modelInfo.getPathEC());
        fileEC.delete();

        boolean wasSelected = Database.getSelectedModel().getName().equals(model);

        Database.deleteModel(model);

        if (wasSelected) {
            ModelInfo newSelected = Database.getAnyModel();
            Database.setSelectedModel(newSelected.getName());
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
            pythonScriptRun(command, null);
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
    private static @NotNull String pythonScriptRun(ArrayList<String> command, String directory) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        if(directory != null)
            processBuilder.directory(new File(directory));
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

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
     * Gets the current git author
     * @return the current git author in the format "name (email)" or null if it is not found
     */
    public static Pair<String, String> getCurrentGitAuthor(Project project) {
        try {
            ArrayList<String> command = new ArrayList<>();
            command.add("git");
            command.add("config");
            command.add("user.email");
            String email = pythonScriptRun(command, project.getBasePath());

            command.set(2, "user.name");
            String name = pythonScriptRun(command, project.getBasePath());
            return new Pair<>(name, email);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

}


