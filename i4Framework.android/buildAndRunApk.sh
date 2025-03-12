./buildApkOld.sh

adb shell am force-stop illa4257.testApp
#adb uninstall illa4257.testApp
adb install -d ./bin/app.apk
adb logcat -c
adb shell am start -W -c api.android.intent.LAUNCHER -a api.android.category.MAIN -n illa4257.i4Studio/illa4257.testApp.AndroidLauncher
pid=$(adb shell pidof -s illa4257.testApp)
adb logcat --pid=$pid
#adb logcat | grep 'illa4257.testApp'