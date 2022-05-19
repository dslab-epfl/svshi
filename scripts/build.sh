#!/bin/sh
cd ..
echo "Building svshi...\n"
cd src/core
sbt "pack; packInstall"
cd ..
cd svshi_gui
npm install
npm run build
cd ..
python3 -m pip install -r requirements.txt
echo "\n...done!"