@echo off
setlocal

set "JAVAC_EXE=C:\Program Files\Java\jdk-26.0.2\bin\javac.exe"
if not exist "%JAVAC_EXE%" set "JAVAC_EXE=javac"

set "M2=%USERPROFILE%\.m2\repository"
set "JFX=%M2%\org\openjfx"
set "MODULE_PATH=%JFX%\javafx-base\21.0.2\javafx-base-21.0.2.jar;%JFX%\javafx-base\21.0.2\javafx-base-21.0.2-win.jar;%JFX%\javafx-graphics\21.0.2\javafx-graphics-21.0.2.jar;%JFX%\javafx-graphics\21.0.2\javafx-graphics-21.0.2-win.jar;%JFX%\javafx-controls\21.0.2\javafx-controls-21.0.2.jar;%JFX%\javafx-controls\21.0.2\javafx-controls-21.0.2-win.jar"
set "CLASSPATH=%M2%\org\xerial\sqlite-jdbc\3.45.3.0\sqlite-jdbc-3.45.3.0.jar;%M2%\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar"

if not exist "target\classes" mkdir "target\classes"
if not exist "target\test-classes" mkdir "target\test-classes"

echo Compiling main classes...
powershell -Command "$s = (Get-ChildItem -Recurse -Filter '*.java' src\main\java).FullName; & '%JAVAC_EXE%' --release 21 --module-path '%MODULE_PATH%' --add-modules javafx.controls -cp '%CLASSPATH%' -d target\classes $s"
xcopy /Y /Q "src\main\resources\*" "target\classes\" >nul

echo Compiling test classes...
powershell -Command "$s = (Get-ChildItem -Recurse -Filter '*.java' src\test\java).FullName; & '%JAVAC_EXE%' --release 21 --module-path '%MODULE_PATH%' --add-modules javafx.controls -cp 'target\classes;%CLASSPATH%' -d target\test-classes $s"

echo Build complete.

