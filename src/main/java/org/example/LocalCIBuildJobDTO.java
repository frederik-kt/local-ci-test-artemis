package org.example;

import java.util.List;

public class LocalCIBuildJobDTO {

    private boolean isBuildSuccessful;

    private String commitHashAssignmentRepository;

    private String commitHashTestRepository;

    private List<LocalCITestCaseDTO> failedTests;

    private List<LocalCITestCaseDTO> successfulTests;

    public LocalCIBuildJobDTO(boolean isBuildSuccessful, String commitHashAssignmentRepository, String commitHashTestRepository, List<LocalCITestCaseDTO> failedTests, List<LocalCITestCaseDTO> successfulTests) {
        this.isBuildSuccessful = isBuildSuccessful;
        this.commitHashAssignmentRepository = commitHashAssignmentRepository;
        this.commitHashTestRepository = commitHashTestRepository;
        this.failedTests = failedTests;
        this.successfulTests = successfulTests;
    }

    public boolean isBuildSuccessful() {
        return isBuildSuccessful;
    }

    public void setBuildSuccessful(boolean buildSuccessful) {
        isBuildSuccessful = buildSuccessful;
    }

    public String getCommitHashAssignmentRepository() {
        return commitHashAssignmentRepository;
    }

    public void setCommitHashAssignmentRepository(String commitHashAssignmentRepository) {
        this.commitHashAssignmentRepository = commitHashAssignmentRepository;
    }

    public String getCommitHashTestRepository() {
        return commitHashTestRepository;
    }

    public void setCommitHashTestRepository(String commitHashTestRepository) {
        this.commitHashTestRepository = commitHashTestRepository;
    }

    public List<LocalCITestCaseDTO> getFailedTests() {
        return failedTests;
    }

    public void setFailedTests(List<LocalCITestCaseDTO> failedTests) {
        this.failedTests = failedTests;
    }

    public List<LocalCITestCaseDTO> getSuccessfulTests() {
        return successfulTests;
    }

    public void setSuccessfulTests(List<LocalCITestCaseDTO> successfulTests) {
        this.successfulTests = successfulTests;
    }
}
