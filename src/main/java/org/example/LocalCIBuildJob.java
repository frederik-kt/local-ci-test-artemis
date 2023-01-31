package org.example;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class LocalCIBuildJob {

    private final Path submissionRepositoryPath;

    private final Path testRepositoryPath;

    private final Path scriptPath;

    private final DockerClient dockerClient;

    public LocalCIBuildJob(Path submissionRepositoryPath, Path testRepositoryPath, Path scriptPath) {
        this.submissionRepositoryPath = submissionRepositoryPath;
        this.testRepositoryPath = testRepositoryPath;
        this.scriptPath = scriptPath;
        DockerClient dockerClient2 = DockerClientBuilder.getInstance().build();
        dockerClient = DockerClientBuilder.getInstance().build();
    }

    public String runBuildJob() throws IOException {
        // Create a volume to store the test results.
        Volume testResultsVolume = new Volume("/test-results");

        HostConfig bindConfig = new HostConfig();
        bindConfig.setBinds(new Bind(submissionRepositoryPath.toString(), new Volume("/submission-repository")),
                new Bind(testRepositoryPath.toString(), new Volume("/test-repository")),
                new Bind(scriptPath.toString(), new Volume("/script")));

        // Create the container with the local paths to the Git repositories and the shell script bound to it.
        CreateContainerResponse container = dockerClient.createContainerCmd("openjdk:8-jre-alpine")
                .withCmd("sh", scriptPath.toString())
                .withHostConfig(bindConfig)
                .withVolumes(testResultsVolume)
                .withEnv("SUBMISSION_REPOSITORY_PATH=" + submissionRepositoryPath, "TEST_REPOSITORY_PATH=" + testRepositoryPath)
                .exec();

        // Start the container.
        dockerClient.startContainerCmd(container.getId()).exec();

        // Wait for the container to finish.
        dockerClient.waitContainerCmd(container.getId()).exec(new WaitContainerResultCallback());

        // Retrieve the test results from the volume.
        InputStream testResults = dockerClient.copyArchiveFromContainerCmd(container.getId(), testResultsVolume.toString()).exec();

        String testResultsString = new String(testResults.readAllBytes());

        // Clean up the container.
        dockerClient.removeContainerCmd(container.getId()).exec();

        return testResultsString;
    }

}
