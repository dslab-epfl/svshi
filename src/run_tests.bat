@ECHO OFF 
echo Running Python tests in 'generator'...
cd generator
python3 -m pytest

sleep 1

echo Running Python tests in 'runtime'...
cd ..
cd runtime
python3 -m pytest

sleep 1

echo Running Python tests in 'verification'...
cd ..
cd verification
python3 -m pytest

sleep 1

echo Running Scala tests in 'core'...
cd ..
cd core
sbt test