package com.example.helloworld;

/**
 * 主题管理器 - 定义所有主题色板
 */
public class ThemeManager {

    public static class ThemeColor {
        public final int topbar;          // 顶栏背景
        public final int topbarText;      // 顶栏文字（保持白色不用改）
        public final int drawerHeader;    // 侧边栏头部
        public final int toolRow;         // 工具栏行背景
        public final int toolRowDark;     // 深色工具栏行背景
        public final int sendBtn;         // 发送按钮
        public final int sendBtnDark;     // 深色发送按钮
        public final int userBubble;      // 用户气泡
        public final int userBubbleDark;  // 深色用户气泡
        public final int aiBubble;        // AI气泡边框色
        public final int accent;          // 强调色（按钮文字、链接）
        public final int accentDark;      // 深色强调色
        public final int welcomeTag;      // 欢迎页标签色
        public final String name;         // 主题名

        public ThemeColor(int topbar, int drawerHeader, int toolRow, int toolRowDark,
                          int sendBtn, int sendBtnDark, int userBubble, int userBubbleDark,
                          int aiBubble, int accent, int accentDark, int welcomeTag, String name) {
            this.topbar = topbar;
            this.topbarText = 0xFFFFFFFF;
            this.drawerHeader = drawerHeader;
            this.toolRow = toolRow;
            this.toolRowDark = toolRowDark;
            this.sendBtn = sendBtn;
            this.sendBtnDark = sendBtnDark;
            this.userBubble = userBubble;
            this.userBubbleDark = userBubbleDark;
            this.aiBubble = aiBubble;
            this.accent = accent;
            this.accentDark = accentDark;
            this.welcomeTag = welcomeTag;
            this.name = name;
        }
    }

    public static final ThemeColor[] THEMES = {
        // 🌀 天空蓝（默认）
        new ThemeColor(
            0xFF81D4FA, 0xFF81D4FA, 0xFFF5FBFF, 0xFF333333,
            0xFF64B5F6, 0xFF5C9EDB, 0xFFB3E5FC, 0xFF1E3A5F,
            0xFF81D4FA, 0xFF64B5F6, 0xFF81D4FA, 0xFF64B5F6,
            "🌀 天空蓝"
        ),
        // 🌸 樱花粉
        new ThemeColor(
            0xFFF8BBD0, 0xFFF8BBD0, 0xFFFFF0F5, 0xFF3D2A35,
            0xFFF48FB1, 0xFFE87A9E, 0xFFF8BBD0, 0xFF4A2D3A,
            0xFFF48FB1, 0xFFF06292, 0xFFEC407A, 0xFFF06292,
            "🌸 樱花粉"
        ),
        // 🌿 薄荷绿
        new ThemeColor(
            0xFFA5D6A7, 0xFFA5D6A7, 0xFFF0FAF0, 0xFF2A3D2E,
            0xFF81C784, 0xFF66BB6A, 0xFFC8E6C9, 0xFF2D4A32,
            0xFF81C784, 0xFF66BB6A, 0xFF4CAF50, 0xFF66BB6A,
            "🌿 薄荷绿"
        ),
        // 🍊 暖阳橙
        new ThemeColor(
            0xFFFFCC80, 0xFFFFCC80, 0xFFFFF8F0, 0xFF3D352A,
            0xFFFFB74D, 0xFFFFA726, 0xFFFFE0B2, 0xFF4A3D2D,
            0xFFFFB74D, 0xFFFF9800, 0xFFF57C00, 0xFFFF9800,
            "🍊 暖阳橙"
        ),
        // 🍇 薰衣草紫
        new ThemeColor(
            0xFFCE93D8, 0xFFCE93D8, 0xFFF8F0FF, 0xFF352D3D,
            0xFFBA68C8, 0xFFAB47BC, 0xFFE1BEE7, 0xFF3A2D4A,
            0xFFBA68C8, 0xFF9C27B0, 0xFF7B1FA2, 0xFF9C27B0,
            "🍇 薰衣草紫"
        )
    };

    public static ThemeColor getTheme(int index) {
        if (index < 0 || index >= THEMES.length) return THEMES[0];
        return THEMES[index];
    }
}
