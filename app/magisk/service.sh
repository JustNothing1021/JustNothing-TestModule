#!/system/bin/sh
# Please don't hardcode /magisk/modname/... ; instead, please use $MODDIR/...
# This will make your scripts compatible even if Magisk change its mount point in the future
MODDIR=${0%/*}
METHODS_WORKDIR="/data/local/tmp/methods"

# This script will be executed in late_start service mode
# More info in the main Magisk thread

cp -r $MODDIR/system/terminfo $METHODS_WORKDIR


echo "#!/system/bin/sh\nexit 0" > /system/bin/test # JLine会调用这个, 而且不知道为啥模块总是不会复制文件过去
chmod 0777 /system/bin/test
