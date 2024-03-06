package com.datamining;

import com.analysis.metrics.MethodMetrics;
import com.utils.importantValues.Values;

import java.io.*;
import java.util.ArrayList;

public class PredictionModel {
    private static final String FILE_PATH = "C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\Classification Model\\prediction.py";
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
            return predictPython(data);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean predictPython(ArrayList<Double> data) throws IOException, InterruptedException {
        String pythonPath = Values.pythonPath;

        //TODO: Uncomment the exception
        if (pythonPath.isEmpty()) {
            //throw new IOException("Python path not set");
            System.out.println("Python path not set");
            pythonPath = "C:\\Program Files\\Python310\\python.exe"; //For testing purposes
        }

        Process process = new ProcessBuilder(pythonPath, FILE_PATH).redirectErrorStream(true).start();

        OutputStream stdin = process.getOutputStream();
        for (Double value : data) {
            stdin.write((value.toString() + "\n").getBytes());
        }
        stdin.close();

        BufferedReader stdOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

        StringBuilder output = new StringBuilder();
        String s;
        while ((s = stdOutput.readLine()) != null) {
            output.append(s);
        }

        int returnCode = process.waitFor();
        if (returnCode != 0 || output.toString().contains("Warning")) {
            throw new IOException("Python script Problem: " + returnCode + "\n" + output);
        }

        float result = Float.parseFloat(output.toString());

        stdOutput.close();

        // Result will be -1 for outliers or 1 for inliers
        return result == 1;
    }
}
