#!/system/bin/sh
#
# Magisk 25.x 的模块安装流程：安装器先把 zip 整个解到模块目录、套一遍默认权限，
# 然后 "source"（不是执行）这个文件。见 https://topjohnwu.github.io/Magisk/guides.html
#
# 这里只补一件事：给 system 下的脚本显式加上执行位。
# 理论上 zip 能存住 0755，但打包工具不一定写这个位，显式设一遍最稳。
# （原来这套逻辑写在 install.sh 的 set_permissions() 里，而官方文档明确要求
#   "DO NOT add a file named install.sh"，所以搬到 customize.sh。）

ui_print "- 修正文件权限"

set_perm_recursive "$MODPATH/system" 0 0 0755 0755
set_perm "$MODPATH/system/bin/methods" 0 0 0755
set_perm "$MODPATH/system/bin/test"    0 0 0755

# 不要在这里调用 exit：安装器还要做收尾工作。
