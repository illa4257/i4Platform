@echo off
echo Building ...
if not exist build mkdir build
gcc -O3 "-DCONFIG_FILENAME=\"test app\"" "-DCOMMAND=\"java\"" "-DARGS=\"-jar MyApp.jar\"" src/launcher.cpp -lstdc++ -o build/Launcher.exe