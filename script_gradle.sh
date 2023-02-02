#!/bin/sh

# Create the folder "assignment" in the test-repository.
mkdir /test-repository/assignment

# Copy the content of the assignment-repository into the folder "assignment" in the test-repository.
cp -a /assignment-repository/. /test-repository/assignment/

# Execute the tests.
cd /test-repository
chmod +x gradlew
./gradlew clean test