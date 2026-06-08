package com.justnothing.richconsole.widgets

interface StatusWidget {
    fun render(): List<String>   // 返回要在状态栏显示的行（从上到下）
}