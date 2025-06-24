#!/bin/bash

keystore=/home/illa4257/.android/debug.keystore
keystorePass=android
keystoreAlias=androiddebugkey
sdkRoot=/home/illa4257/Tools/android-sdk
platform=${sdkRoot}/platforms/android-35
buildTools=${sdkRoot}/build-tools/34.0.0
libs=/home/illa4257/IdeaProjects/i4Platform

# Clean previous build artifacts
rm -v -f -r ./obj
rm -v -f -r ./bin
rm -v -f -r ./compiled_res
mkdir ./obj
mkdir ./bin
mkdir ./compiled_res

# Compile resources with aapt2
${buildTools}/aapt2 compile -o ./compiled_res/ --dir ./androidRes

# Link resources with aapt2
${buildTools}/aapt2 link -o ./bin/app.unsigned.apk -I ${platform}/android.jar --manifest ./AndroidManifest.xml -A ${libs}/i4Framework/assets -A assets -R ./compiled_res/*.flat

# Compile Java sources
# javac -d ./obj/ -source 1.8 -target 1.8 -classpath ${platform}/android.jar -sourcepath ${libs}/i4Utils/src:${libs}/i4Framework/src:${libs}/i4Framework.android/src:./src/ ./src/illa4257/testApp/*

# Convert .class files to .dex using d8
#${buildTools}/d8 --output ./bin $(find ./obj -name "*.class")
${buildTools}/d8 --output ./bin /home/illa4257/IdeaProjects/i4Platform/out/artifacts/i4TestApp_jar/i4TestApp.jar

cd ./bin
zip -u app.unsigned.apk classes.dex
cd ..

# Align the APK
${buildTools}/zipalign -v 4 ./bin/app.unsigned.apk ./bin/app.aligned.apk

# Sign the APK
${buildTools}/apksigner sign --ks $keystore --ks-pass pass:$keystorePass --key-pass pass:$keystorePass --out ./bin/app.apk ./bin/app.aligned.apk

# Verify the APK
${buildTools}/apksigner verify ./bin/app.apk
