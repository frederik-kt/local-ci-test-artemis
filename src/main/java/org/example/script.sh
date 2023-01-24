#!/bin/sh

# Compile the source code
javac -cp /repo1/libs/*:/repo1/src -d /repo1/bin
/repo1/src/com/example/Main.java

# Run the tests
java -cp /repo1/libs/*:/repo1/bin:/repo2/libs/*:/repo2/src
org.junit.runner.JUnitcore com.example.MainTest

# Save the test results
mv /repo1