#!/bin/sh
echo "Installing the svshi CLI..."
cd svshi/core
sbt "pack; packInstall"
echo "\n...svshi installation done!"