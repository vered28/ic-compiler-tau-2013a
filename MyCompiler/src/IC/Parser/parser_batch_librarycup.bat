@ECHO OFF
PUSHD %~dp0
 
java java_cup.Main -nowarn -dump_grammar -parser LibraryParser Library.cup

POPD
PAUSE
