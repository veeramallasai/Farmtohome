@echo off
setlocal

set "MVN_DIR=%~dp0.mvn\apache-maven-3.9.6"
set "MVN_BIN=%MVN_DIR%\bin\mvn.cmd"

if exist "%MVN_BIN%" (
    "%MVN_BIN%" %*
    exit /b %ERRORLEVEL%
)

where mvn >nul 2>nul
if %ERRORLEVEL%==0 (
    mvn %*
    exit /b %ERRORLEVEL%
)

echo Apache Maven not found. Auto-downloading portable Apache Maven 3.9.6 into .mvn...
powershell -Command "& { $ProgressPreference = 'SilentlyContinue'; Write-Host 'Downloading Maven...'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip' -OutFile '%~dp0.mvn\maven.zip'; Write-Host 'Extracting Maven...'; Expand-Archive -Path '%~dp0.mvn\maven.zip' -DestinationPath '%~dp0.mvn' -Force; Remove-Item '%~dp0.mvn\maven.zip' -Force }"

if exist "%MVN_BIN%" (
    echo Apache Maven 3.9.6 downloaded successfully!
    "%MVN_BIN%" %*
    exit /b %ERRORLEVEL%
) else (
    echo Failed to setup Maven automatically. Please download Maven manually.
    exit /b 1
)
