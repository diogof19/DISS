package com.datamining;

import com.utils.importantValues.Values;

import java.io.*;
import java.util.ArrayList;

public class PredictionModel {
    private static final String PYTHON_PATH = "C:\\Users\\dluis\\Documents\\Docs\\Universidade\\M 2 ano\\Thesis\\DISS\\Classification Model\\prediction.py";
    public static void main(String[] args) {
        ArrayList<Double> data = new ArrayList<>();

        // Add 18 test values to the array
        for (int i = 0; i < 18; i++) {
            data.add(0.0 + i);
        }

        try {
            System.out.println(predict(data));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public PredictionModel() {
        System.out.println("Prediction Model");
    }

    public static boolean predict(ArrayList<Double> data) throws IOException, InterruptedException {
        String pythonPath = Values.pythonPath;

        //TODO: Uncomment the exception
        if (pythonPath.isEmpty()) {
            //throw new IOException("Python path not set");
            pythonPath = "C:\\Program Files\\Python310\\python.exe"; //For testing purposes
        }

        Process process = new ProcessBuilder(pythonPath, PYTHON_PATH).redirectErrorStream(true).start();

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
