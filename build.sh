#!/bin/sh
echo "Building svshi...\n"
cd src/core
sbt "pack; packInstall"
cd ..
pip3 install -r requirements.txt
echo "\n...done!"