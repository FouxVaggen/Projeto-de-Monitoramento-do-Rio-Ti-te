@echo off
REM ============================================================
REM  Chat Rio Tietê - APS UNIP 2026/1
REM  Script de compilação e execução (Windows)
REM ============================================================

echo Compilando...
javac Servidor.java ClienteChat.java
if errorlevel 1 (
    echo ERRO na compilacao. Verifique o Java JDK instalado.
    pause
    exit /b 1
)
echo Compilacao OK!
echo.
echo Escolha o que deseja executar:
echo [1] Iniciar SERVIDOR
echo [2] Iniciar CLIENTE (interface grafica)
echo.
set /p opcao="Opcao: "

if "%opcao%"=="1" (
    echo Iniciando servidor na porta 12345...
    java Servidor
) else if "%opcao%"=="2" (
    echo Iniciando cliente...
    java ClienteChat
) else (
    echo Opcao invalida.
)
pause
