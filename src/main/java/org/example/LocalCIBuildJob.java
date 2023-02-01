package org.example;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

public class LocalCIBuildJob {

    private final Path submissionRepositoryPath;

    private final Path testRepositoryPath;

    private final Path scriptPath;

    private final DockerClient dockerClient;

    public LocalCIBuildJob(Path submissionRepositoryPath, Path testRepositoryPath, Path scriptPath) {
        this.submissionRepositoryPath = submissionRepositoryPath;
        this.testRepositoryPath = testRepositoryPath;
        this.scriptPath = scriptPath;

        String connectionUri;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            connectionUri = "tcp://localhost:2375";
        } else {
            connectionUri = "unix:///var/run/docker.sock";
        }
        dockerClient = DockerClientBuilder.getInstance(connectionUri).build();
    }

    public String runBuildJob() throws IOException {
        // Create a volume to store the test results.
        Volume testResultsVolume = new Volume("/test-results");

        HostConfig bindConfig = new HostConfig();
        bindConfig.setBinds(new Bind(submissionRepositoryPath.toString(), new Volume("/submission-repository")),
                new Bind(testRepositoryPath.toString(), new Volume("/test-repository")),
                new Bind(scriptPath.toString(), new Volume("/script.sh")));

        // Create the container with the local paths to the Git repositories and the
        // shell script bound to it.
        CreateContainerResponse container = dockerClient.createContainerCmd("openjdk:8-jre-alpine")
                // .withCmd("/bin/sh", "-c", "while true; do echo 'running'; sleep 1; done")
                .withCmd("sh", "script.sh")
                .withHostConfig(bindConfig)
                .withVolumes(testResultsVolume)
                .withEnv("SUBMISSION_REPOSITORY_PATH=" + submissionRepositoryPath,
                        "TEST_REPOSITORY_PATH=" + testRepositoryPath)
                .exec();

        // Start the container.
        dockerClient.startContainerCmd(container.getId()).exec();

        // Wait for the container to finish.
        dockerClient.waitContainerCmd(container.getId()).exec(new WaitContainerResultCallback());

        // Next step: Wie kriege ich den Content eines dort erstellten Files in eine
        // Form, in der ich ihn weiter verarbeiten kann?

        // Retrieve the test results from the volume.
        TarArchiveInputStream tarInputStream = new TarArchiveInputStream(
                dockerClient.copyArchiveFromContainerCmd(container.getId(), "/test-results").exec());

        // TODO: For this to work the host-test-results folder must exist -> Check
        // whether folder exists else create.
        Path testResultsHost = Paths.get("host-test-results", "results.txt").toAbsolutePath();

        unTar(tarInputStream, Files.createFile(testResultsHost));

        InputStream testResults = dockerClient.copyArchiveFromContainerCmd(container.getId(),
                "/test-results").exec();

        String testResultsString = new String(testResults.readAllBytes());

        // Clean up the container.
        dockerClient.removeContainerCmd(container.getId()).exec();

        return testResultsString;
    }

    private void unTar(TarArchiveInputStream tarInputStream, Path destFile) throws IOException {
        TarArchiveEntry tarEntry = null;
        while ((tarEntry = tarInputStream.getNextTarEntry()) != null) {
            if (tarEntry.isDirectory()) {
                if (!Files.exists(destFile)) {
                    Files.createDirectory(destFile);
                }
            } else {
                FileOutputStream fileOutputStream = new FileOutputStream(destFile.toFile());
                IOUtils.copy(tarInputStream, fileOutputStream);
                fileOutputStream.close();
            }
        }
        tarInputStream.close();
    }

}
