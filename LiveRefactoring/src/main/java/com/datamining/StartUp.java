package com.datamining;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URL;

public class StartUp  implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        Thread thread = new Thread(new StartUpRunnable());
        thread.start();
    }

    private static class StartUpRunnable implements Runnable {
        @Override
        public void run() {
            try {
                StartUp startUp = new StartUp();
                startUp.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start() throws IOException {
        extractFileIfNotExists("metrics.db");
        extractFileIfNotExists("python/bias_model.py");
        extractFileIfNotExists("python/prediction.py");
        extractFileIfNotExists("requirements.txt");
        extractFileIfNotExists("models/model.joblib");

        PredictionModel.setModelFilePath(Database.getSelectedModelFilePath());
    }

    /**
     * Extracts the file if it does not exist
     * @param filePath path of the file to extract
     * @throws IOException if an I/O error occurs
     */
    private void extractFileIfNotExists(String filePath) throws IOException {
        File file = new File("tmp/" + filePath);

        if (!file.exists()) {
            extractFile(filePath);
            System.out.println("Extracted " + filePath);
        }
    }

    /**
     * Extracts a file from the jar file to the tmp folder
     * @param fileName the name of the file to be extracted
     * @throws IOException if there is a problem extracting the file
     */
    private void extractFile(String fileName) throws IOException {
        File tmpFile = new File("tmp/" + fileName);
        File parentDir = tmpFile.getParentFile();

        if(!parentDir.exists()){
            parentDir.mkdirs();
        }

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
