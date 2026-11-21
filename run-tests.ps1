$java = "C:\Program Files\Java\jdk-26.0.2\bin\java.exe"
if (-not (Test-Path $java)) {
    $java = "java"
}

$m2 = "$HOME\.m2\repository"
$jfx = "$m2\org\openjfx"
$mp = "$jfx\javafx-base\21.0.2\javafx-base-21.0.2.jar;$jfx\javafx-base\21.0.2\javafx-base-21.0.2-win.jar;$jfx\javafx-graphics\21.0.2\javafx-graphics-21.0.2.jar;$jfx\javafx-graphics\21.0.2\javafx-graphics-21.0.2-win.jar;$jfx\javafx-controls\21.0.2\javafx-controls-21.0.2.jar;$jfx\javafx-controls\21.0.2\javafx-controls-21.0.2-win.jar"
$cp = "target\test-classes;target\classes;$m2\org\xerial\sqlite-jdbc\3.45.3.0\sqlite-jdbc-3.45.3.0.jar;$m2\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar"

& $java --module-path $mp --add-modules javafx.controls --enable-native-access=javafx.graphics -cp $cp com.finance.TestRunner $args

