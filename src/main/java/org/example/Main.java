package org.example;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Path assignmentRepositoryPath = Paths.get("repositories", "maven", "assignment-success").toAbsolutePath();
        Path testRepositoryPath = Paths.get("repositories", "maven", "test").toAbsolutePath();
        Path scriptPath = Paths.get("script_maven.sh").toAbsolutePath();
        LocalCIBuildJob localCIBuildJob = new LocalCIBuildJob(assignmentRepositoryPath, testRepositoryPath, scriptPath);
        try {
            localCIBuildJob.runBuildJob();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}