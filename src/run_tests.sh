#!/bin/sh
echo "Running Python tests in 'generator'... \n"
cd generator
python3 -m pytest

sleep 1

echo "\nRunning Python tests in 'runtime'... \n"
cd ..
cd runtime
python3 -m pytest

sleep 1

echo "\nRunning Python tests in 'verification'... \n"
cd ..
cd verification
python3 -m pytest

echo "\nRunning Python tests in 'extended_verification'... \n"
cd ..
cd extended_verification
python3 -m pytest


sleep 1

echo "\nRunning Scala tests in 'core'... \n"
cd ..
cd core
sbt test
sbt it:test