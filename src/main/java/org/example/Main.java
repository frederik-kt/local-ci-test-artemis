package org.example;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Path submissionRepositoryPath = Paths.get("repositories", "submission-success").toAbsolutePath();
        Path testRepositoryPath = Paths.get("repositories", "test").toAbsolutePath();
        Path scriptPath = Paths.get("script.sh").toAbsolutePath();
        LocalCIBuildJob localCIBuildJob = new LocalCIBuildJob(submissionRepositoryPath, testRepositoryPath, scriptPath);
        try {
            // String result = localCIBuildJob.runBuildJob();
            localCIBuildJob.runBuildJob();
            // System.out.println("Result: " + result);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}