# ⚛️ 理物 — 物理竞赛 AI 助手

> **「理物」** — 寓理于物，格物致知  
> 一个专为物理竞赛打造的 Android AI 助手

---

## 📱 项目简介

**理物**是一款运行在 Android 平台上的 AI 对话应用，接入 **DeepSeek V4 Flash** 大模型，专为物理竞赛学习而设计。整个聊天界面采用**全屏 WebView** 实现，消息通过 JS 注入渲染，MathJax 统一处理 LaTeX 公式，支持代码高亮、OCR 识图、联网搜索等多功能。

---

## ✨ 功能一览

| 功能 | 说明 |
|------|------|
| 🧠 **AI 对话** | 接入 DeepSeek V4 Flash，专注物理竞赛辅导 |
| 📐 **LaTeX 公式** | MathJax 3 CDN 实时渲染，行内/块级公式完美支持 |
| 📷 **OCR 识图** | ML Kit 中文识别，支持相册选取和拍照两种入口 |
| 🌐 **联网搜索** | 语义关键词提取 + Bing / 百度双引擎，搜索结果不暴露 |
| 🐍 **物理绘图** | AI 自动生成 Matplotlib 代码绘制物理图示/受力分析 |
| 🎨 **多色主题** | 天空蓝 / 樱花粉 / 薄荷绿 / 暖阳橙 / 薰衣草紫 五套可选 |
| 🌙 **深色模式** | 跟随系统 / 浅色 / 深色，一键切换 |
| 💬 **对话管理** | 侧边栏历史列表，支持重命名、置顶、删除（需确认） |
| 🔄 **一键复制** | 代码块和 AI 回复均可一键复制 |
| ⏳ **思考计时** | 实时显示 AI 思考秒数，等待不枯燥 |
| ✨ **动效优化** | 消息入场动画、按钮按压反馈、平滑滚动、侧栏渐入 |
| 📁 **数据持久化** | JSON 文件存储对话历史，索引+对话文件分离 |

---

## 📸 界面预览

| 聊天界面 | 欢迎页 | 侧边栏 |
|---------|--------|--------|
| 全屏 WebView，LaTeX 实时渲染，代码块高亮+「📋复制」按钮 | ⚛️ 原子图标 + 闪烁星光，首条消息发送后动画消失 | 对话卡片列表，置顶/时间排序，⋮ 菜单含重命名、置顶、删除 |

> 截图待补充

---

## 🛠️ 技术架构

### 项目结构

```
Li-Wu/
├── build.gradle                  # 项目级 Gradle (AGP 8.2.0)
├── settings.gradle               # 项目设置
├── gradle.properties             # AndroidX 配置
│
└── app/
    ├── build.gradle              # 模块依赖 (OkHttp, Gson, ML Kit, AppCompat)
    └── src/main/
        ├── AndroidManifest.xml   # 权限声明 (INTERNET, CAMERA, 存储)
        │
        ├── java/com/example/liwu/
        │   ├── MainActivity.java           # ★ 主逻辑 (~800行)
        │   ├── Conversation.java           # 对话数据模型 (~35行)
        │   ├── ConversationManager.java    # JSON 持久化管理器 (~170行)
        │   ├── OcrHelper.java             # ML Kit OCR 中文识别 (~40行)
        │   ├── OcrPreprocessor.java       # 图片预处理 (灰度/对比度/二值化)
        │   ├── SearchEngine.java          # Bing + 百度双引擎搜索 (~220行)
        │   ├── KeywordExtractor.java      # 语义关键词提取 (~140行)
        │   ├── ThemeManager.java          # 5 套主题色板 (~100行)
        │   │
        │   └── latex/
        │       └── LatexUtils.java        # WebView HTML/CSS/JS 工厂 (~260行)
        │
        └── res/
            ├── layout/activity_main.xml   # DrawerLayout 布局 (~230行)
            ├── drawable/                  # 8 个样式 XML
            └── values/colors.xml          # 颜色定义
```

### 核心技术参数

| 参数 | 值 |
|------|-----|
| **语言** | **Java**（纯 Java，无 Kotlin） |
| **最低 SDK** | 21 (Android 5.0) |
| **目标 SDK** | 34 (Android 14) |
| **AI 模型** | `deepseek-v4-flash` |
| **API 地址** | `https://api.deepseek.com/v1/chat/completions` |
| **OCR 引擎** | Google ML Kit — Chinese Text Recognition |
| **联网搜索** | Bing + 百度（网页爬取，无需 API Key） |
| **LaTeX 渲染** | MathJax 3 (SVG) — CDN 加载 |
| **网络请求** | OkHttp 4.12 |
| **数据存储** | 内部文件系统 — JSON 格式 |
| **序列化** | Gson 2.10.1 |

### 核心数据流

```
用户输入 → 提取关键词 (如有搜索) → Bing/百度搜索 → 构建上下文
    ↓
WebView JS注入 ←—— convertToHtml() ←—— DeepSeek API (OkHttp)
    ↓
MathJax 渲染 → 显示消息
```

---

## 🚀 快速开始

### 环境要求

- Android 5.0+ 设备
- 网络连接（用于 API 调用和 MathJax CDN）

### 下载安装

从 [Releases](https://github.com/JamesBHI/Li-Wu/releases) 下载最新 APK 安装包，或自行编译。

---

## 🔧 开发相关

### 在 Termux 中编译

```bash
cd ~/HelloWorld
./gradlew assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk ~/storage/downloads/理物_1.2.apk
pm install ~/storage/downloads/理物_1.2.apk
```

### 备份项目

```bash
cd ~
tar --exclude='HelloWorld/.gradle' --exclude='HelloWorld/app/build' \
  -czf ~/storage/downloads/Li-Wu_源码.tar.gz HelloWorld/
```

### 项目依赖 (`app/build.gradle`)

```groovy
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.activity:activity:1.9.0'
    implementation 'androidx.drawerlayout:drawerlayout:1.2.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'com.google.mlkit:text-recognition-chinese:16.0.0'
}
```

---

## 📋 完整功能清单

| # | 功能 | 状态 |
|---|------|------|
| 1 | 全屏 WebView 聊天界面 | ✅ |
| 2 | LaTeX 公式渲染（MathJax CDN） | ✅ |
| 3 | 代码块渲染 + 一键复制 | ✅ |
| 4 | AI 回复一键复制（📋按钮） | ✅ |
| 5 | 输入工具栏（$ ^ _ 按钮） | ✅ |
| 6 | 思考等待提示（含跳点动画） | ✅ |
| 7 | Token 消耗显示 | ✅ |
| 8 | 对话历史 JSON 存储 + 侧边栏管理 | ✅ |
| 9 | 取消请求（发送↔取消切换） | ✅ |
| 10 | 设置菜单（深色 / 主题 / LaTeX教程 / 关于） | ✅ |
| 11 | DrawerLayout 侧边栏 | ✅ |
| 12 | 深色模式（跟随系统 / 浅色 / 深色，持久化） | ✅ |
| 13 | 对话重命名（⋮菜单） | ✅ |
| 14 | 对话置顶（按置顶+时间排序） | ✅ |
| 15 | 对话删除（需确认弹窗） | ✅ |
| 16 | 思考计时器（⏳ 0s → ⏳ 1s → ...） | ✅ |
| 17 | 欢迎消息（⚛️✨ 动画，首条消息后隐藏） | ✅ |
| 18 | 联网搜索（关键词提取 + Bing + 百度，隐藏上下文） | ✅ |
| 19 | OCR 识图（ML Kit 中文，相册 / 拍照，预处理增强） | ✅ |
| 20 | 按钮按压动画、卡片渐入、平滑滚动 | ✅ |
| 21 | ChatGPT 风格气泡（渐变 / 阴影 / 圆角） | ✅ |
| 22 | 五套浅色主题 + 深色模式 | ✅ |
| 23 | 空对话不保存 | ✅ |

---

## 📄 开源许可

本项目仅供个人学习与交流使用。

---

## 🙏 致谢

- **[DeepSeek](https://deepseek.com/)** — 强大的 AI 推理模型
- **[Google ML Kit](https://developers.google.com/ml-kit)** — 离线 OCR 引擎
- **[MathJax](https://www.mathjax.org/)** — 优秀的 LaTeX 渲染库
- **[OkHttp](https://square.github.io/okhttp/)** — 高效的网络请求库
- **[Gson](https://github.com/google/gson)** — JSON 序列化库

---

<p align="center">
  ⚛️ <strong>理物</strong> — 让物理学习更简单<br>
  <sub>开发者：Castorice · © 2026</sub>
</p>
