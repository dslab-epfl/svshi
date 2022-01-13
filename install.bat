@ECHO OFF
echo Installing svshi ...

cd %LOCALAPPDATA%
mkdir Svshi

cd %SVSHI_HOME%
cd svshi\core\target\pack
robocopy . "%LOCALAPPDATA%\Svshi" /E

setx path "%PATH%;%LOCALAPPDATA%/Svshi"

cd %SVSHI_HOME%/svshi
python -m pip install -r requirements.txt

echo ...svshi building done!
pause