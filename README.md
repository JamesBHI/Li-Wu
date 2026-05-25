```markdown
# ⚛️ 理物

物理竞赛AI助手
成功跑在Android上了，我想我真牛逼


## 这玩意儿能干哈

就是一个物理竞赛助手
接了个DeepSeek进去


问问题能答
拍题能识别
搜题能联网
画图能Python
反正该有的都有了


## 目前能干的事

- [x] AI对话（DeepSeek V4 Flash）
- [x] LaTeX公式渲染（输$F=ma$就变好看）
- [x] 代码块高亮+一键复制
- [x] 拍题识别（相册和拍照都行）
- [x] 联网搜题（Bing+百度双搜）
- [x] AI画物理图（Python画受力分析）
- [x] 对话历史管理
- [x] 重命名对话
- [x] 置顶对话
- [x] 删除对话
- [x] 五套主题色（蓝粉绿橙紫）
- [x] 深色模式（熬夜刷题必备）
- [x] 思考计时器（看AI想了多少秒）
- [x] 按钮按压动画（很Q弹）
- [x] 空对话不保存
- [x] 消息淡入动画
- [x] 欢迎页（⚛️✨那个）



## 截图

妹想到吧！
还没截

后期补吧


## 怎么用

### 下载

去Releases页面下载
或者自己编译
看你了
不会下找院长

### 安装

允许未知来源
不会装找院长


## 🎮 功能入口

| 你想干嘛 | 点哪里 |
|---------|--------|
| 问问题 | 打字，发送 |
| 拍题 | 📷 → 拍照或相册 |
| 联网搜 | 🌐 变蓝了再发消息 |
| 换主题 | ⚙️ → 通用 → 主题 |
| 深色模式 | ⚙️ → 通用 → 深色模式 |
| 翻历史 | ☰ 侧边栏 |
| 删对话 | 侧边栏⋮ → 删除 |


## 技术栈

语言：Java（没用Kotlin，因为我不会）
最低API：21（Android 5.0）
目标API：34（Android 14）
AI模型：deepseek-v4-flash
OCR：Google ML Kit中文识别
搜索：Bing+百度（不用API Key，白嫖的）
公式：MathJax 3
网络：OkHttp 4.12
存储：JSON文件
主题：5套色板


## 项目结构

```
app/src/main/java/com/example/helloworld/
├── MainActivity.java          # 主逻辑 800行
├── Conversation.java          # 数据模型
├── ConversationManager.java   # JSON存储
├── OcrHelper.java             # OCR识别
├── OcrPreprocessor.java       # 图片预处理
├── SearchEngine.java          # 搜索引擎
├── KeywordExtractor.java      # 关键词提取
├── ThemeManager.java          # 主题色板
└── latex/
    └── LatexUtils.java        # WebView那一堆
```

资源文件：
```
res/
├── layout/activity_main.xml   # 布局
├── drawable/                  # 8个样式
└── values/colors.xml          # 颜色
```




## 🤦 已知问题

OCR识别率还是不太行
不知道为什么
考虑换个方案

买tokens的钱不够了！
没有经费！帮帮我们！


## 🙏 感谢

- DeepSeek — 永久降价！男人！什么罐头我说！
- MathJax — 公式渲染，差一点就变成手搓的了
- Google ML Kit — 离线OCR还行
- 关注院长喵~ 关注院长谢谢喵~
- 希望今年复赛不会炸缸


## 📄 许可证

仅供个人学习
别拿去卖钱求你了


---

<p align="center">
  <strong>⚛️ 理物 = 理解物理</strong><br>
  <sub>Succeed</sub><br>
  <sub>Made by Castorice 🧑‍💻</sub>
</p>
```
