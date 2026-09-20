#!/system/bin/sh
#
# JustNothing TestModule —— Magisk 模块安装脚本
#
# Magisk 装模块时会 source 这个文件，然后回调下面这几个函数。函数名和变量名是 Magisk
# 安装器的接口，不能改：
#   print_modname     打印模块信息
#   on_install        释放文件
#   set_permissions   设置权限
#   SKIPMOUNT / PROPFILE / POSTFSDATA / LATESTARTSERVICE / REPLACE   安装开关
# 能用的工具函数由 Magisk 提供（ui_print、set_perm、set_perm_recursive），
# 环境变量 $MODPATH、$ZIPFILE 也是它给的。
#

# 早年 Magisk 的开关。现在的 Magisk 改成直接读模块目录下的 service.sh / post-fs-data.sh，
# 这几个变量留着只为兼容老安装器，不影响新版本。
SKIPMOUNT=false
PROPFILE=false
POSTFSDATA=true
LATESTARTSERVICE=true

# 需要顶掉的已有文件或目录，本模块没有
REPLACE=""

print_modname() {
  ui_print "--------------------------"
  ui_print "  JustNothing TestModule"
  ui_print "  By 真的啥也不是啊"
  ui_print "--------------------------"
}

on_install() {
  ui_print "- 释放模块文件"
  unzip -o "$ZIPFILE" 'system/*' -d "$MODPATH" >&2
  unzip -o "$ZIPFILE" 'common/*' -d "$MODPATH" >&2
}

set_permissions() {
  ui_print "- 修正文件权限"

  set_perm_recursive "$MODPATH"         0 0 0755 0644
  set_perm_recursive "$MODPATH/system"  0 0 0755 0755

  set_perm "$MODPATH/system/bin/methods" 0 0 0755
  set_perm "$MODPATH/system/bin/test"    0 0 0755
}
