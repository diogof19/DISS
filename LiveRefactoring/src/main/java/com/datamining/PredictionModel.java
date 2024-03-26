package com.datamining;

import com.analysis.metrics.MethodMetrics;
import com.utils.importantValues.Values;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

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
            pythonPredictionFilePath = extractFile("predict.py");

        if(modelFilePath == null || modelFilePath.isEmpty())
            modelFilePath = extractFile("model.joblib");

        String pythonPath = getPythonPath();

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

    public static void biasModel(ArrayList<String> authors) throws IOException, InterruptedException {
        if(pythonBiasFilePath == null || pythonBiasFilePath.isEmpty())
            pythonBiasFilePath = extractFile("bias.py");

        if(modelFilePath == null || modelFilePath.isEmpty())
            modelFilePath = extractFile("model.joblib");

        if(dataFilePath == null || dataFilePath.isEmpty())
            dataFilePath = extractFile("extracted_metrics.csv");

        String pythonPath = getPythonPath();

        ArrayList<String> command = new ArrayList<>();
        command.add(pythonPath);
        command.add(pythonBiasFilePath);
        command.add(modelFilePath);
        command.add(dataFilePath);
        command.addAll(authors);

        pythonScriptRun(command);

    }

    private static String getPythonPath() {
        String pythonPath = Values.pythonPath;

        //TODO: Uncomment the exception
        //TODO: Add pop-up when the python path is not set
        if (pythonPath.isEmpty()) {
            //throw new IOException("Python path not set");
            System.out.println("Python path not set");
            pythonPath = "C:\\Program Files\\Python310\\python.exe"; //For testing purposes
        }

        return pythonPath;
    }

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

}


