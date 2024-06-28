package com.datamining;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.utils.importantValues.Values;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URL;

public class StartUp implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        Utils.popup(project,
                "LiveRef",
                "Starting up the plugin. This may take a few seconds.",
                NotificationType.INFORMATION
        );
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
        File folder = new File(PathManager.getConfigPath() + "/liveRefData");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        Values.dataFolder = folder.getAbsolutePath() + "/";

        extractFileIfNotExists(folder.getAbsolutePath(),"/metrics.db");
        extractFileIfNotExists(folder.getAbsolutePath(),"/python/bias_model.py");
        extractFileIfNotExists(folder.getAbsolutePath(),"/python/prediction.py");
        extractFileIfNotExists(folder.getAbsolutePath(),"/python/scalerEM.pkl");
        extractFileIfNotExists(folder.getAbsolutePath(),"/python/scalerEC.pkl");
        extractFileIfNotExists(folder.getAbsolutePath(),"/requirements.txt");

        if(extractFileIfNotExists(folder.getAbsolutePath(),"/models/DefaultEM.joblib") &&
                extractFileIfNotExists(folder.getAbsolutePath(),"/models/DefaultEC.joblib")){
            Database.createModel(new ModelInfo("Default",
                    folder.getAbsolutePath() + "/models/DefaultEM.joblib",
                    folder.getAbsolutePath() + "/models/DefaultEC.joblib",
                    true));
        }
    }

    /**
     * Extracts the file if it does not exist
     * @param path path to folder where to extract the file to
     * @param name name of the file to extract (partial path starting from resources)
     * @throws IOException if an I/O error occurs
     */
    private boolean extractFileIfNotExists(String path, String name) throws IOException {
        File file = new File(path + name);

        boolean extracted = false;
        if (!file.exists()) {
            extractFile(path, name);
            System.out.println("Extracted " + name);
            extracted = true;
        }

        return extracted;
    }

    /**
     * Extracts a file from the jar file to the tmp folder
     * @param path path to folder where to extract the file to
     * @param name name of the file to extract (partial path starting from resources)
     * @throws IOException if there is a problem extracting the file
     */
    private void extractFile(String path, String name) throws IOException {
        File tmpFile = new File(path + name);
        File parentDir = tmpFile.getParentFile();

        if(!parentDir.exists()){
            parentDir.mkdirs();
        }

        URL url = PredictionModel.class.getResource(name);

        InputStream inputStream = url.openStream();
        OutputStream outputStream = new FileOutputStream(path + name);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        inputStream.close();
        outputStream.close();
    }

}
