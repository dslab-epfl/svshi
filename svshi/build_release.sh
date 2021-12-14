#!/bin/sh
echo "Building the release... \n"
cd core
rm -rf target/*.tar.gz
sbt packArchive
mv target/*.tar.gz ..
echo "\n...done! The tar.gz archive can be found in svshi/"