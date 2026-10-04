@echo off
setlocal
if exist taskflow.jar (
    java -jar taskflow.jar %*
) else if exist bin\com\taskflow\Main.class (
    java -cp bin com.taskflow.Main %*
) else (
    echo [INFO] Project not built yet. Building now...
    call build.bat
    if exist taskflow.jar (
        java -jar taskflow.jar %*
    ) else (
        java -cp bin com.taskflow.Main %*
    )
)
