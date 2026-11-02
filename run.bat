@echo off
REM Compile the game fresh from src into out, then run it
REM (does not depend on the VS Code compile cache in bin)
cd /d "%~dp0"
if exist out rmdir /s /q out
javac -encoding UTF-8 -d out -sourcepath src src\main\Main.java
if errorlevel 1 (
    echo.
    echo Compile failed - see the errors above
    pause
    exit /b 1
)
java -cp out main.Main
