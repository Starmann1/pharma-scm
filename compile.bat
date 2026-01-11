@echo off
echo Compiling Java files...
javac -d bin -cp "lib/*" ^
src\pharma\App.java ^
src\pharma\model\*.java ^
src\pharma\service\*.java ^
src\pharma\gui\*.java ^
src\pharma\gui\components\*.java

if %ERRORLEVEL% EQU 0 (
    echo Compilation successful!
) else (
    echo Compilation failed!
)
