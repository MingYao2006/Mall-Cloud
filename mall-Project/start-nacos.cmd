@echo off
set JAVA_HOME=D:\Java\jdk-23.0.2
echo JAVA_HOME=%JAVA_HOME%
echo java.exe exists:
dir "%JAVA_HOME%\bin\java.exe"
echo Starting Nacos...
call "C:\Users\67161\ai-infra\nacos\standalone\nacos-3.2.2\bin\startup.cmd" -m standalone
