#!/bin/sh
echo "Building the release... \n"
VERSION=1.4.2-SNAPSHOT

cd core
rm -rf target/*.tar.gz
sbt compile
sbt packArchive

cd ../svshi_gui
npm run build

cd ../..
zip svshi-v$VERSION.zip -r install.sh install.ps1 setup-python3.ps1 install-pip.ps1 src/core src/generator src/runtime src/verification src/__init__.py src/requirements.txt src/requirements_without_crosshair.txt src/get-pip.py src/svshi_gui/dist -x "src/generator/__pycache__/*" "src/generator/.pytest_cache/*" "src/runtime/__pycache__/*" "src/runtime/.pytest_cache/*" "src/verification/__pycache__/*" "src/verification/.pytest_cache/*" "src/generator/tests/*" "src/runtime/tests/*" "src/verification/tests/*" "src/generator/.coveragerc" "src/runtime/.coveragerc" "src/verification/.coveragerc" "src/core/.bloop/*" "src/core/.bsp/*" "src/core/.metals/*" "src/core/.vscode/*" "src/core/.bloop/*" "src/core/.gitignore" "src/core/.scalafmt.conf"

echo "\n...done! The .zip file can be found at the root"
