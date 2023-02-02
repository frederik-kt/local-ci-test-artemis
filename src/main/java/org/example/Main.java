package org.example;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Path assignmentRepositoryPath = Paths.get("repositories", "gradle", "assignment-success").toAbsolutePath();
        Path testRepositoryPath = Paths.get("repositories", "gradle", "test").toAbsolutePath();
        Path scriptPath = Paths.get("script_gradle.sh").toAbsolutePath();
        LocalCIBuildJob localCIBuildJob = new LocalCIBuildJob(assignmentRepositoryPath, testRepositoryPath, scriptPath);
        try {
            localCIBuildJob.runBuildJob();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}