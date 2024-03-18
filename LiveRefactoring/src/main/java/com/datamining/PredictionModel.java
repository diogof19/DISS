package com.datamining;

import com.analysis.metrics.MethodMetrics;
import com.utils.importantValues.Values;

import java.io.*;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

//TODO: Create a 'requirements.txt' folder and function that runs it with pip install
public class PredictionModel {
    private static String pythonFilePath;
    private static String modelFilePath;
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
        if(pythonFilePath == null || pythonFilePath.isEmpty())
            extractPythonAndModelFiles();

        String pythonPath = Values.pythonPath;

        //TODO: Uncomment the exception
        if (pythonPath.isEmpty()) {
            //throw new IOException("Python path not set");
            System.out.println("Python path not set");
            pythonPath = "C:\\Program Files\\Python310\\python.exe"; //For testing purposes
        }

        List<String> command = new ArrayList<>();
        command.add(pythonPath);
        command.add(pythonFilePath);
        command.add(modelFilePath);
        for (Double value : data) {
            command.add(value.toString());
        }

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

        float result = Float.parseFloat(output.toString());

        stdOutput.close();

        // Result will be -1 for outliers or 1 for inliers
        return result == 1;
    }

    /**
     * Extracts the python script and the model file from the jar file
     * @throws IOException if there is a problem extracting the files
     */
    private static void extractPythonAndModelFiles() throws IOException {
        File file = new File("tmp");
        if(!file.exists()){
            file.mkdir();
        }

        extractFile("prediction.py");
        extractFile("model.joblib");

        pythonFilePath = Paths.get("tmp/prediction.py").toAbsolutePath().toString();
        modelFilePath = Paths.get("tmp/model.joblib").toAbsolutePath().toString();
    }

    /**
     * Extracts a file from the jar file
     * @param fileName the name of the file to be extracted
     * @throws IOException if there is a problem extracting the file
     */
    private static void extractFile(String fileName) throws IOException {
        URL url = PredictionModel.class.getResource("/" + fileName);

        InputStream inputStream = url.openStream();
        OutputStream outputStream = new FileOutputStream("tmp/" + fileName);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        inputStream.close();
        outputStream.close();
    }

}


