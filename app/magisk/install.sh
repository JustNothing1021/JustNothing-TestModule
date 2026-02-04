#!/system/bin/sh

##########################################################################################
#
# 模块安装脚本
#
##########################################################################################

##########################################################################################
# 配置
##########################################################################################

SKIPMOUNT=false
PROPFILE=false
POSTFSDATA=true
LATESTARTSERVICE=true

##########################################################################################
# 安装信息
##########################################################################################

print_modname() {
  ui_print "*******************************"
  ui_print "   JustNothing TestModule"
  ui_print "      By 真的啥也不是啊   "
  ui_print "*******************************"
}

##########################################################################################
# 文件替换列表
##########################################################################################

REPLACE="

"

##########################################################################################
# 进行安装
##########################################################################################

on_install() {
  ui_print "- 正在释放文件"
  unzip -o "$ZIPFILE" 'system/*' -d $MODPATH >&2
  unzip -o "$ZIPFILE" 'common/*' -d $MODPATH >&2

  ui_print "- 检测设备架构: $ARCH"
  
  ui_print "- 复制C++客户端"
  # 根据设备架构选择对应的客户端
  case "$ARCH" in
    arm64)
      CLIENT_ARCH="arm64-v8a"
      ;;
    arm)
      CLIENT_ARCH="armeabi-v7a"
      ;;
    x64)
      CLIENT_ARCH="x86_64"
      ;;
    x86)
      CLIENT_ARCH="x86"
      ;;
    *)
      ui_print "  警告: 未知架构$ARCH，将会使用arm64-v8a"
      CLIENT_ARCH="arm64-v8a"
      ;;
  esac
  
  if [ -f "$MODPATH/common/binary/$CLIENT_ARCH/methods_client_$CLIENT_ARCH" ]; then
    cp "$MODPATH/common/binary/$CLIENT_ARCH/methods_client_$CLIENT_ARCH" "$MODPATH/system/framework/methods_client"
    ui_print "  C++客户端已复制: $CLIENT_ARCH"
  else
    ui_print "  警告: 找不到C++客户端 ($CLIENT_ARCH)"
  fi
}

##########################################################################################
# 设置权限
##########################################################################################

set_permissions() {
  ui_print "- 设置文件权限"

  set_perm_recursive  $MODPATH         0  0  0755  0644
  set_perm_recursive  $MODPATH/system  0  0  0755  0755

  if [ -f "$MODPATH/system/framework/methods_client" ]; then
    set_perm  $MODPATH/system/framework/methods_client  0  0  0755
  fi
}

