#!/bin/sh
echo "Building svshi...\n"
cd src/core
sbt "pack; packInstall"
cd ..
python3 -m pip install -r requirements.txt
echo "\n...done!"