#!/bin/sh

# Compile the source code
#javac -cp $SUBMISSION_REPOSITORY_PATH/libs/*:$SUBMISSION_REPOSITORY_PATH/src -d $SUBMISSION_REPOSITORY_PATH/bin
#$SUBMISSION_REPOSITORY_PATH/src/testpackage/Client.java

# Run the tests
#java -cp $SUBMISSION_REPOSITORY_PATH/libs/*:$SUBMISSION_REPOSITORY_PATH/bin:$TEST_REPOSITORY_PATH/libs/*:$TEST_REPOSITORY_PATH/src
#org.junit.runner.JUnitcore testpackage.MethodTest

# Save the test results
#mv $SUBMISSION_REPOSITORY_PATH

echo $SUBMISSION_REPOSITORY_PATH > /test_results/test.txt