#!/bin/sh
echo "Installing the svshi CLI...\n"
cd svshi/core
sbt "pack; packInstall"
echo "\n...svshi installation done!"