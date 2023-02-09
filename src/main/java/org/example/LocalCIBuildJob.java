package org.example;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

public class LocalCIBuildJob {

    private final BuildTool buildTool;

    private final Path assignmentRepositoryPath;

    private final Path testRepositoryPath;

    private final Path scriptPath;

    private final DockerClient dockerClient;

    public LocalCIBuildJob(BuildTool buildTool, Path assignmentRepositoryPath, Path testRepositoryPath, Path scriptPath) {
        this.buildTool = buildTool;
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

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withAutoRemove(true) // Automatically remove the container when it exits.
                .withBinds(new Bind(assignmentRepositoryPath.toString(), new Volume("/assignment-repository")),
                        new Bind(testRepositoryPath.toString(), new Volume("/test-repository")),
                        new Bind(scriptPath.toString(), new Volume("/script.sh")));

        // Create the container from the "ls1tum/artemis-maven-template:java17-13" image with the local paths to the Git repositories and the shell script bound to it.
        CreateContainerResponse container = dockerClient.createContainerCmd("ls1tum/artemis-maven-template:java17-13")
                .withHostConfig(hostConfig)
                .withEnv("ARTEMIS_BUILD_TOOL=" + buildTool.toString().toLowerCase(), "ARTEMIS_DEFAULT_BRANCH=main") // TODO: Replace with default branch for participation.
                // Command to run when the container starts. This is the command that will be executed in the container's main process, which runs in the foreground and blocks the container from exiting until it finishes.
                // It waits until the script that is running the tests (see below execCreateCmdResponse) is completed, which is running in the background and indicates termination by creating a file "script_completed.txt" in the root directory.
                .withCmd("sh", "-c", "while [ ! -f /script_completed.txt ]; do sleep 0.5; done")
                //.withCmd("tail", "-f", "/dev/null") // Activate for debugging purposes instead of the above command to get a running container that you can peek into using "docker exec -it <container-id> /bin/bash".
                .exec();

        try {
            // Start the container.
            dockerClient.startContainerCmd(container.getId()).exec();

            // The "sh script.sh" command specified here is run inside the container as an additional process. This command runs in the background, independent of the container's main process. The execution command can run concurrently with the main process.
            // Creates a script_completed file in the container's root directory when the script finishes. The main process is waiting for this file to appear and then stops the main process, thus stopping the container.
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(container.getId()).withAttachStdout(true).withAttachStderr(true).withCmd("sh", "-c", "sh script.sh; touch /script_completed.txt").exec();

            // Start the command and wait for it to complete.
            final CountDownLatch latch = new CountDownLatch(1);
            dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(new ResultCallback.Adapter<>() {
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });

            try {
                // Block until the latch reaches 0 or until the thread is interrupted.
                latch.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException("Interrupted while waiting for command to complete", e);
            }

            // When Gradle is used as the build tool, the test results are located in /repositories/test-repository/build/test-resuls/test/TEST-*.xml.
            // When Maven is used as the build tool, the test results are located in /repositories/test-repository/target/surefire-reports/TEST-*.xml.
            String testResultsPath;
            if (buildTool == BuildTool.GRADLE) {
                testResultsPath = "/repositories/test-repository/build/test-results/test";
            } else if (buildTool == BuildTool.MAVEN) {
                testResultsPath = "/repositories/test-repository/target/surefire-reports";
            } else {
                throw new IllegalStateException("Unknown build tool: " + buildTool);
            }

            // Get an input stream of the test result files.
            TarArchiveInputStream tarInputStream = new TarArchiveInputStream(
                    dockerClient
                            .copyArchiveFromContainerCmd(container.getId(), testResultsPath)
                            .exec());

            String resultsPath = "C:\\Users\\Kutsch\\IdeaProjects\\local-ci-test-artemis\\results\\";

            // For test purpuses, extract the contents of the tar archive. TODO: Try to go through entries in the tar file directly.
            TarArchiveEntry tarEntry;
            while ((tarEntry = tarInputStream.getNextTarEntry()) != null) {
                if (tarEntry.isDirectory()) {
                    // create the directory if it doesn't exist
                    new File(resultsPath + tarEntry.getName()).mkdirs();
                } else {
                    // create the file if it doesn't exist
                    File outputFile = new File(resultsPath + tarEntry.getName());
                    outputFile.createNewFile();

                    // write the contents of the tar entry to the file
                    try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
                        int count;
                        byte[] data = new byte[2048];
                        while ((count = tarInputStream.read(data)) != -1) {
                            fileOutputStream.write(data, 0, count);
                        }
                    }
                }
            }

            // Go through all xml result files created in /build/test-results/test, and extract data out of them.
            boolean isBuildSuccessful = true;
            List<LocalCITestCaseDTO> failedTests = new ArrayList<>();
            List<LocalCITestCaseDTO> successfulTests = new ArrayList<>();

            List<String> timestamps = new ArrayList<>();

            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

            File dir = new File(resultsPath + "test");
            File[] directoryListing = dir.listFiles();
            if (directoryListing != null) {
                for (File child : directoryListing) {
                    if (!child.isDirectory() && child.getName().startsWith("TEST-") && child.getName().endsWith(".xml")) {
                        // Create an XML stream reader for the entry.
                        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(new FileInputStream(child));

                        // Move to the first start element.
                        while (xmlStreamReader.hasNext() && !xmlStreamReader.isStartElement()) {
                            xmlStreamReader.next();
                        }

                        // Check if the start element is the "testsuite" node.
                        if (!xmlStreamReader.getLocalName().equals("testsuite")) {
                            throw new IllegalStateException("Expected testsuite element, but got " + xmlStreamReader.getLocalName());
                        }

                        // Extract the timestamp attribute from the "testsuite" node.
                        String timestamp = xmlStreamReader.getAttributeValue(null, "timestamp");
                        timestamps.add(timestamp);

                        // Go through all testcase nodes.
                        while (xmlStreamReader.hasNext()) {
                            xmlStreamReader.next();

                            if (xmlStreamReader.isStartElement() && xmlStreamReader.getLocalName().equals("testcase")) {
                                // Extract the name attribute from the "testcase" node.
                                String name = xmlStreamReader.getAttributeValue(null, "name");

                                // Check if there is a failure node inside the testcase node.
                                // Call next() until there is an end element (no failure node exists inside the testcase node) or a start element (failure node exists inside the testcase node).
                                xmlStreamReader.next();
                                while (!(xmlStreamReader.isEndElement() || xmlStreamReader.isStartElement())) {
                                    xmlStreamReader.next();
                                }
                                if (xmlStreamReader.isStartElement() && xmlStreamReader.getLocalName().equals("failure")) {
                                    // Extract the message attribute from the "failure" node.
                                    String message = xmlStreamReader.getAttributeValue(null, "message");

                                    // Add the failed test to the list of failed tests.
                                    failedTests.add(new LocalCITestCaseDTO(name, List.of(message)));

                                    // If there is at least one test case with a failure node, the build is not successful.
                                    isBuildSuccessful = false;
                                } else {
                                    // Add the successful test to the list of successful tests.
                                    successfulTests.add(new LocalCITestCaseDTO(name, null));
                                }
                            }
                        }
                        // Close the XML stream reader.
                        xmlStreamReader.close();
                    }
                }
            }

//            TarArchiveEntry tarEntry;
//            while ((tarEntry = tarInputStream.getNextTarEntry()) != null) {
//                if (!tarEntry.isDirectory() && tarEntry.getName().startsWith(buildTool == BuildTool.GRADLE ? "test" : "surefire-reports" + "/TEST-") && tarEntry.getName().endsWith(".xml")) {
//                    // Create an XML stream reader for the entry.
//                    XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(tarInputStream);
//
//                    // Move to the first start element.
//                    while (xmlStreamReader.hasNext() && !xmlStreamReader.isStartElement()) {
//                        xmlStreamReader.next();
//                    }
//
//                    // Check if the start element is the "testsuite" node.
//                    if (!xmlStreamReader.getLocalName().equals("testsuite")) {
//                        throw new IllegalStateException("Expected testsuite element, but got " + xmlStreamReader.getLocalName());
//                    }
//
//                    // Extract the timestamp attribute from the "testsuite" node.
//                    String timestamp = xmlStreamReader.getAttributeValue(null, "timestamp");
//                    timestamps.add(timestamp);
//
//                    // Go through all testcase nodes.
//                    while (xmlStreamReader.hasNext()) {
//                        xmlStreamReader.next();
//
//                        if (xmlStreamReader.isStartElement() && xmlStreamReader.getLocalName().equals("testcase")) {
//                            // Extract the name attribute from the "testcase" node.
//                            String name = xmlStreamReader.getAttributeValue(null, "name");
//
//                            // Check if there is a failure node inside the testcase node.
//                            if (xmlStreamReader.hasNext() && xmlStreamReader.isStartElement() && xmlStreamReader.getLocalName().equals("failure")) {
//                                // Extract the message attribute from the "failure" node.
//                                String message = xmlStreamReader.getAttributeValue(null, "message");
//
//                                // Add the failed test to the list of failed tests.
//                                failedTests.add(new LocalCITestCaseDTO(name, List.of(message)));
//
//                                // If there is at least one test case with a failure node, the build is not successful.
//                                isBuildSuccessful = false;
//                            } else {
//                                // Add the successful test to the list of successful tests.
//                                successfulTests.add(new LocalCITestCaseDTO(name, null));
//                            }
//                        }
//                    }
//                    // Close the XML stream reader.
//                    xmlStreamReader.close();
//                }
//            }
//
//            // Close the TAR archive input stream.
//            tarInputStream.close();

            // Find the latest timestamp found in the test results.

            // The commit hash of the assignment repository is located in /repositories/assignment-repository/.git/refs/heads/main.
            String commitHashAssignmentRepository = null;

            // The commit hash of the test repository is located in /repositories/test-repository/.git/refs/heads/main.
            String commitHashTestRepository = null;

            LocalCIBuildJobDTO buildJob = new LocalCIBuildJobDTO(isBuildSuccessful, commitHashAssignmentRepository, commitHashTestRepository, failedTests, successfulTests);

            System.out.println(timestamps);
            System.out.println(buildJob);

        } catch (Exception e) {
            // TODO: Handle exception, i.e. notify Artemis that the build failed because of some internal issue.
            System.out.println(e.getMessage());
        }
    }
}
