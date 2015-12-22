@echo off
"%JAVA_HOME%/bin/java.exe" -Xmx4g -classpath "%~dp0/*" com.sourcegraph.toolchain.application.Main %*
