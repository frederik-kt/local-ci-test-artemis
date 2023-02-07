package org.example;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Path assignmentRepositoryPath = Paths.get("repositories", "gradle", "bare", "assignment-success.git").toAbsolutePath();
        Path testRepositoryPath = Paths.get("repositories", "gradle", "bare", "tests.git").toAbsolutePath();
        Path scriptPath = Paths.get("script.sh").toAbsolutePath();
        LocalCIBuildJob localCIBuildJob = new LocalCIBuildJob(BuildTool.GRADLE, assignmentRepositoryPath, testRepositoryPath, scriptPath);
        try {
            localCIBuildJob.runBuildJob();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}