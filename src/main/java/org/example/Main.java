package org.example;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Path submissionRepositoryPath = Paths.get("C:\\Users\\Kutsch\\vs-ws\\artemis_repos\\fredtestcoursetestex1-artemis_test_user_1");
        Path testRepositoryPath = Paths.get("C:\\Users\\Kutsch\\vs-ws\\artemis_repos\\fredtestcoursetestex1-tests");
        Path scriptPath = Paths.get("script.sh");
        LocalCIBuildJob localCIBuildJob = new LocalCIBuildJob(submissionRepositoryPath, testRepositoryPath, scriptPath);
        try {
            localCIBuildJob.runBuildJob();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}