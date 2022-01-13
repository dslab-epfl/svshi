#!/bin/sh
echo "Building svshi...\n"
cd svshi/core
sbt "pack; packInstall"
cd ..
pip3 install -r requirements.txt
echo "\n...done!"