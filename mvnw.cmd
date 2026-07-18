@REM Maven Wrapper for Windows
@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "MAVEN_VERSION=3.9.6"
set "MAVEN_HOME=%MAVEN_PROJECTBASEDIR%.mvn\maven-%MAVEN_VERSION%"
set "MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_ZIP=%MAVEN_PROJECTBASEDIR%.mvn\apache-maven-%MAVEN_VERSION%-bin.zip"

@REM Check JAVA_HOME
if not defined JAVA_HOME (
    echo ERROR: JAVA_HOME is not set.
    echo Please set JAVA_HOME to point to your JDK installation.
    exit /b 1
)

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
    exit /b 1
)

@REM Download Maven if not present
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Maven not found, downloading Apache Maven %MAVEN_VERSION%...

    if not exist "%MAVEN_ZIP%" (
        powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('%MAVEN_URL%', '%MAVEN_ZIP%')"
        if errorlevel 1 (
            echo ERROR: Failed to download Maven.
            exit /b 1
        )
    )

    echo Extracting Maven...
    powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%MAVEN_PROJECTBASEDIR%.mvn' -Force"
    if errorlevel 1 (
        echo ERROR: Failed to extract Maven.
        exit /b 1
    )

    @REM Rename extracted folder
    if exist "%MAVEN_PROJECTBASEDIR%.mvn\apache-maven-%MAVEN_VERSION%" (
        if exist "%MAVEN_HOME%" rmdir /s /q "%MAVEN_HOME%"
        rename "%MAVEN_PROJECTBASEDIR%.mvn\apache-maven-%MAVEN_VERSION%" "maven-%MAVEN_VERSION%"
    )

    echo Maven %MAVEN_VERSION% installed successfully.
)

@REM Run Maven
set "MAVEN_OPTS=-Xmx512m"
"%MAVEN_HOME%\bin\mvn.cmd" %*

endlocal
