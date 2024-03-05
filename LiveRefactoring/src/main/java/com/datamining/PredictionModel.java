package com.datamining;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public PredictionModel() {
        System.out.println("Prediction Model");
    }

    public static boolean predict(ArrayList<Double> data) throws IOException, InterruptedException {
        // Construct the command to execute the Python script
        String pythonPath = System.getenv("C:\\Program Files\\Python310\\python.exe");

        List<String> command = new ArrayList<>();
        command.add(pythonPath);
        command.add(PYTHON_PATH);

        // Start the Python process
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        // Pass input to the Python script
        OutputStream stdin = process.getOutputStream();
        for (Double value : data) {
            stdin.write((value.toString() + "\n").getBytes());
        }
        stdin.close();

        // Read output from the Python script
        InputStream stdout = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
        String output = reader.readLine();

        // Wait for the process to finish and get the return code
        int returnCode = process.waitFor();

        // Handle any errors
        if (returnCode != 0) {
            throw new IOException("Python script returned non-zero exit status: " + returnCode);
        }

        // Convert the output to a double
        Float result = Float.parseFloat(output);

        // Close streams
        reader.close();
        stdout.close();

        // Result will be -1 for outliers or 1 for inliers
        return result == 1;
    }
}
