$javac = "C:\Program Files\Java\jdk-26.0.2\bin\javac.exe"
if (-not (Test-Path $javac)) { $javac = "javac" }

$m2 = "$HOME\.m2\repository"
$jfx = "$m2\org\openjfx"
$mp = "$jfx\javafx-base\21.0.2\javafx-base-21.0.2.jar;$jfx\javafx-base\21.0.2\javafx-base-21.0.2-win.jar;$jfx\javafx-graphics\21.0.2\javafx-graphics-21.0.2.jar;$jfx\javafx-graphics\21.0.2\javafx-graphics-21.0.2-win.jar;$jfx\javafx-controls\21.0.2\javafx-controls-21.0.2.jar;$jfx\javafx-controls\21.0.2\javafx-controls-21.0.2-win.jar"
$cp = "$m2\org\xerial\sqlite-jdbc\3.45.3.0\sqlite-jdbc-3.45.3.0.jar;$m2\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar"

New-Item -ItemType Directory -Force -Path "target\classes" | Out-Null
New-Item -ItemType Directory -Force -Path "target\test-classes" | Out-Null

Write-Host "Compiling main sources..."
$sources = (Get-ChildItem -Recurse -Filter "*.java" src\main\java).FullName
& $javac --release 21 --module-path $mp --add-modules javafx.controls -cp $cp -d target\classes $sources
Copy-Item src\main\resources\* target\classes -Force

Write-Host "Compiling test sources..."
$testSources = (Get-ChildItem -Recurse -Filter "*.java" src\test\java).FullName
& $javac --release 21 --module-path $mp --add-modules javafx.controls -cp "target\classes;$cp" -d target\test-classes $testSources

Write-Host "Build complete."

