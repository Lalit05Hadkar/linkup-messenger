@echo off
title LinkUp Messenger Emulator
set ANDROID_AVD_HOME=D:\Android\avd
set ANDROID_SDK_ROOT=D:\Android\Sdk
set ANDROID_HOME=D:\Android\Sdk

echo ==============================================
echo Launching LinkUp Messenger in Android Emulator
echo ==============================================

start "" "D:\Android\Sdk\emulator\emulator.exe" -avd LinkUpDevice -no-snapshot-load

echo Waiting for emulator to connect...
"D:\Android\Sdk\platform-tools\adb.exe" wait-for-device

echo Installing latest APK...
"D:\Android\Sdk\platform-tools\adb.exe" install -r "%~dp0app\build\outputs\apk\debug\app-debug.apk"

echo Opening LinkUp Messenger...
"D:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.aistudio.linkup.mzypxr/com.example.MainActivity

echo LinkUp Messenger is running!
