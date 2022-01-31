echo "Installing svshi..."
cd src/core/target/pack
make install

cd ../../..
python3 -m pip install -r requirements.txt
echo "...done!"