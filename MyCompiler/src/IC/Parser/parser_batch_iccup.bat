@ECHO OFF
PUSHD %~dp0
 
java java_cup.Main -parser Parser IC.cup

POPD
PAUSE
