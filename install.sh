echo "Installing svshi..."
cd svshi/core/target/pack
make install

cd ../../..
pip3 install -r requirements.txt
echo "...done!"