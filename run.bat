@echo off
setlocal

set "JAVA_EXE=C:\Program Files\Java\jdk-26.0.2\bin\java.exe"
if not exist "%JAVA_EXE%" (
    set "JAVA_EXE=java"
)

set "M2=%USERPROFILE%\.m2\repository"
set "JFX=%M2%\org\openjfx"
set "MODULE_PATH=%JFX%\javafx-base\21.0.2\javafx-base-21.0.2.jar;%JFX%\javafx-base\21.0.2\javafx-base-21.0.2-win.jar;%JFX%\javafx-graphics\21.0.2\javafx-graphics-21.0.2.jar;%JFX%\javafx-graphics\21.0.2\javafx-graphics-21.0.2-win.jar;%JFX%\javafx-controls\21.0.2\javafx-controls-21.0.2.jar;%JFX%\javafx-controls\21.0.2\javafx-controls-21.0.2-win.jar"
set "CLASSPATH=target\classes;%M2%\org\xerial\sqlite-jdbc\3.45.3.0\sqlite-jdbc-3.45.3.0.jar;%M2%\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar"

"%JAVA_EXE%" --module-path "%MODULE_PATH%" --add-modules javafx.controls --enable-native-access=javafx.graphics -cp "%CLASSPATH%" com.finance.App %*

