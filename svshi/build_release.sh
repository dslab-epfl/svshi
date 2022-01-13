#!/bin/sh
echo "Building the release... \n"
VERSION=1.2.0 

cd core
rm -rf target/*.tar.gz
sbt packArchive

cd ../..
zip svshi-v$VERSION.zip -r install.sh install.bat create_svshi_home_env_var.bat svshi/core svshi/generator svshi/runtime svshi/verification svshi/__init__.py svshi/requirements.txt svshi/requirements_without_crosshair.txt -x "svshi/generator/__pycache__/*" "svshi/generator/.pytest_cache/*" "svshi/runtime/__pycache__/*" "svshi/runtime/.pytest_cache/*" "svshi/verification/__pycache__/*" "svshi/verification/.pytest_cache/*" "svshi/generator/tests/*" "svshi/runtime/tests/*" "svshi/verification/tests/*" "svshi/generator/.coveragerc" "svshi/runtime/.coveragerc" "svshi/verification/.coveragerc" "svshi/core/.bloop/*" "svshi/core/.bsp/*" "svshi/core/.metals/*" "svshi/core/.vscode/*" "svshi/core/.bloop/*" "svshi/core/.gitignore" "svshi/core/.scalafmt.conf"

echo "\n...done! The .zip file can be found at the root"
