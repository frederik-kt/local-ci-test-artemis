#!/bin/bash

# Create the folder "assignment" in the test-repository.
mkdir /test-repository/assignment

# Copy the content of the assignment-repository into the folder "assignment" in the test-repository.
cp -a /assignment-repository/. /test-repository/assignment/

# Execute the tests.

cd /test-repository
# Make sure line endings are correct (might be wrong when taking the file from a Windows file system). TODO: Copy file instead of binding.
sed -i -e 's/\r$//' gradlew
chmod +x gradlew
./gradlew clean test