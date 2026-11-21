@echo off
setlocal
title LPSX Admin
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-admin.ps1"
if errorlevel 1 pause
