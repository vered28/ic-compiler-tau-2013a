@ECHO OFF
PUSHD %~dp0

java JFlex.Main --jlex -v IC.lex

POPD
PAUSE
