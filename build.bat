@echo off
setlocal enabledelayedexpansion

echo ======================================================================
echo   🔨 Building TaskFlow (Java 21)
echo ======================================================================

if not exist bin mkdir bin
if not exist data mkdir data

echo [1/3] Compiling Java source files...
javac -encoding UTF-8 -d bin src\com\taskflow\model\*.java src\com\taskflow\util\*.java src\com\taskflow\repository\*.java src\com\taskflow\service\*.java src\com\taskflow\server\*.java src\com\taskflow\cli\*.java src\com\taskflow\test\*.java src\com\taskflow\Main.java

if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed!
    exit /b %errorlevel%
)

echo [2/3] Detecting jar packager...
set JAR_BIN=jar
where jar >nul 2>&1
if %errorlevel% neq 0 (
    if exist "C:\Program Files\Java\latest\jdk-21\bin\jar.exe" (
        set "JAR_BIN=C:\Program Files\Java\latest\jdk-21\bin\jar.exe"
    ) else if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\jar.exe" (
            set "JAR_BIN=%JAVA_HOME%\bin\jar.exe"
        )
    )
)

echo [3/3] Packaging into standalone executable JAR (taskflow.jar)...
echo Main-Class: com.taskflow.Main > bin\manifest.txt
"%JAR_BIN%" cvfm taskflow.jar bin\manifest.txt -C bin com >nul 2>&1

if exist taskflow.jar (
    echo ======================================================================
    echo   🎉 Build Successful!
    echo   Executable created: taskflow.jar
    echo   Run with: run.bat OR java -jar taskflow.jar
    echo ======================================================================
) else (
    echo [INFO] Compiled to bin/ folder successfully.
    echo Run directly with: java -cp bin com.taskflow.Main
)
