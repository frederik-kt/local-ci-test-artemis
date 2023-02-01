package org.example;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

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

    private final Path assignmentRepositoryPath;

    private final Path testRepositoryPath;

    private final Path scriptPath;

    private final DockerClient dockerClient;

    public LocalCIBuildJob(Path assignmentRepositoryPath, Path testRepositoryPath, Path scriptPath) {
        this.assignmentRepositoryPath = assignmentRepositoryPath;
        this.testRepositoryPath = testRepositoryPath;
        this.scriptPath = scriptPath;

        String connectionUri;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            connectionUri = "tcp://localhost:2375";
        } else {
            connectionUri = "unix:///var/run/docker.sock";
        }

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(connectionUri)
                .build();
        dockerClient = DockerClientBuilder.getInstance(config).build();
    }

    public void runBuildJob() throws IOException {

        HostConfig bindConfig = new HostConfig();
        bindConfig.setBinds(new Bind(assignmentRepositoryPath.toString(), new Volume("/assignment-repository")),
                new Bind(testRepositoryPath.toString(), new Volume("/test-repository")),
                new Bind(scriptPath.toString(), new Volume("/script.sh")));

        // Create the container with the local paths to the Git repositories and the
        // shell script bound to it.
        CreateContainerResponse container = dockerClient.createContainerCmd("bash:5.2.15")
                .withCmd("/bin/sh", "-c", "while true; do echo 'running'; sleep 1; done")
                // .withCmd("sh", "script.sh")
                .withHostConfig(bindConfig)
                .exec();

        try {
            // Start the container.
            dockerClient.startContainerCmd(container.getId()).exec();

            if (true)
                return;

            // Wait for the container to finish.
            dockerClient.waitContainerCmd(container.getId()).exec(new WaitContainerResultCallback());

            // Retrieve the test results from the volume.
            TarArchiveInputStream tarInputStream = new TarArchiveInputStream(
                    dockerClient
                            .copyArchiveFromContainerCmd(container.getId(), "/test-repository/build/test-results/test")
                            .exec());

            // Create folder to save the test results into.
            Path testResultsHostPath = Path.of("host-test-result").toAbsolutePath();
            Files.createDirectories(testResultsHostPath);

            // Go through all xml result files created in /build/test-results/test, and
            // parse them with JUnitParser.
            TarArchiveEntry tarEntry = null;
            while ((tarEntry = tarInputStream.getNextTarEntry()) != null) {
                if (!tarEntry.isDirectory()) {
                    Path testResultHostPath = testResultsHostPath.resolve(tarEntry.getName());
                    File testResultFile = Files.createFile(testResultHostPath).toFile();
                    FileOutputStream fileOutputStream = new FileOutputStream(testResultFile);
                    IOUtils.copy(tarInputStream, fileOutputStream);
                    fileOutputStream.close();
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            System.out.println(e.getStackTrace());
        } finally {
            // Clean up the container.
            dockerClient.removeContainerCmd(container.getId()).exec();
        }
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
