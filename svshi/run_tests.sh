#!/bin/sh
echo "Running Python tests in 'generator'... \n"
cd generator
pytest

sleep 1

echo "\nRunning Python tests in 'runtime'... \n"
cd ..
cd runtime
pytest

sleep 1

echo "\nRunning Python tests in 'verification'... \n"
cd ..
cd verification
pytest

sleep 1

echo "\nRunning Scala tests in 'core'... \n"
cd ..
cd core
sbt test