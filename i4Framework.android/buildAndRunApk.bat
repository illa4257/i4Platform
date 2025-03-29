call buildApk.bat

set appId=example.testApp

echo Installing ...

adb shell am force-stop %appId%
adb install -d bin/app.apk
adb logcat -c
adb shell am start -W -c api.android.intent.LAUNCHER -a api.android.category.MAIN -n %appId%/%appId%.AndroidLauncher
FOR /F "tokens=* USEBACKQ" %%F IN (`adb shell pidof -s %appId%`) DO SET pid=%%F
adb logcat --pid=%pid%