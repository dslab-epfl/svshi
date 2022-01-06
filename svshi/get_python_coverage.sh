cd generator
pytest --cov-config=.coveragerc --cov=.
cd ..

cd runtime
pytest --cov-config=.coveragerc --cov=.
cd ..

cd verification
pytest --cov-config=.coveragerc --cov=.
cd ..

coverage combine generator/.coverage verification/.coverage runtime/.coverage

coverage report

coverage xml