./buildApk.sh

adb shell am force-stop illa4257.testApp
#adb uninstall illa4257.testApp
adb install -d ./bin/app.apk
adb logcat -c
adb shell am start -W -c api.android.intent.LAUNCHER -a api.android.category.MAIN -n illa4257.i4test/illa4257.i4test.AndroidLauncher
pid=$(adb shell pidof -s illa4257.i4test)
adb logcat --pid=$pid
#adb logcat | grep 'illa4257.testApp'