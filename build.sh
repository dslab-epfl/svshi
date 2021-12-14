#!/bin/sh
echo "Building the svshi CLI...\n"
cd svshi/core
sbt "pack; packInstall"
echo "\n...svshi building done!"