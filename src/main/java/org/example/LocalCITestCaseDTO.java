package org.example;

import java.util.List;

public class LocalCITestCaseDTO {
    private String name;
    private List<String> message;

    public LocalCITestCaseDTO(String name, List<String> message) {
        this.name = name;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getMessage() {
        return message;
    }

    public void setMessage(List<String> message) {
        this.message = message;
    }
}
