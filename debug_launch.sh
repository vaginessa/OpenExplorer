ant debug
adb install -r `ls -tr bin/*.apk | tail -n1`
adb shell "am start -n org.brandroid.openmanager/.activities.OpenExplorer"
