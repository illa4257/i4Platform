@echo off
set keystore=%userprofile%\.android\debug.keystore
set keystorePass=android
set keystoreAlias=androiddebugkey
set sdkRoot=%ANDROID_HOME%
set platform=%sdkRoot%/platforms/android-35
set buildTools=%sdkRoot%/build-tools/35.0.1
set libs=%userprofile%\IdeaProjects\i4Platform
set "jdk=C:\Program Files\Java\jdk-23"

setlocal EnableDelayedExpansion

IF "%1"==gen-keystore (
    "%jdk%\bin\keytool" -genkeypair -v -keystore %keystore% -alias %keystoreAlias% -storepass %keystorePass% -keyalg RSA -keysize 2048 -validity 10000
    goto end
) ELSE (
    echo Building ...
)

REM Clean previous build artifacts
del /Q obj
del /Q bin
del /Q compiled_res
mkdir obj
mkdir bin
mkdir compiled_res

REM Compile resources with aapt2
%buildTools%/aapt2 compile -o compiled_res/ --dir androidRes

REM Link resources with aapt2
SET var=
FOR /F "tokens=* USEBACKQ" %%F IN (`where /r compiled_res *.flat`) DO SET "var=!var! %%F"
%buildTools%/aapt2 link -o ./bin/app.unsigned.apk -I %platform%/android.jar --manifest AndroidManifest.xml -A assets -A %libs%\i4Framework\assets -R %var%

call %buildTools%\d8 --output bin input.jar

cd bin
"%jdk%\bin\jar" -ufM app.unsigned.apk classes.dex
cd ..

REM Align the APK
%buildTools%/zipalign -v 4 ./bin/app.unsigned.apk ./bin/app.aligned.apk

REM Sign the APK
%buildTools%/apksigner sign --min-sdk-version 24 --ks %keystore% --ks-pass pass:%keystorePass% --key-pass pass:%keystorePass% --out ./bin/app.apk bin/app.aligned.apk

REM Verify the APK
%buildTools%/apksigner verify ./bin/app.apk

:end