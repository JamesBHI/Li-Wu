package com.example.helloworld;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.*;
import android.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.example.helloworld.latex.LatexUtils;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import com.example.helloworld.ThemeManager;
import com.example.helloworld.ThemeManager.ThemeColor;

public class MainActivity extends Activity {

    private static final int REQUEST_PICK_IMAGE = 1001;
    private static final int REQUEST_TAKE_PHOTO = 1002;
    private static final int REQUEST_CAMERA_PERMISSION = 2001;

    private DrawerLayout drawerLayout;
    private LinearLayout drawerContent;
    private LinearLayout conversationList;
    private EditText inputMessage;
    private Button btnSend, btnNewFromDrawer, btnDollar, btnCaret, btnUnderscore, btnImage, btnSearch;
    private ImageButton btnMenu, btnNewChat, btnSettings;
    private TextView titleText, conversationCount, thinkingStatus;
    private FrameLayout chatContainer;

    private static final String API_KEY = "sk-2a9eb729832b4bdebdbb8e095f6c28fb";
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String MODEL_NAME = "deepseek-v4-flash";
    private static final String APP_VERSION = "1.2";

    private static final String SYSTEM_PROMPT =
            "你是一个物理竞赛指导教师，注意你的回答里不能存在说教等行为。" +
            "我会问你物理竞赛题，可能很难也可能很简单。有时我会想你阐明对于一道题 目我自己的思路，这时候你需要提出可能会让下次做题时犯错的问题来强化我对知识的理解。" +
            "你的回答应该具有专业性，但注意不要使用难度过高的数学技巧，尽量做到不 跳步骤，回答清晰。注意式子标注序号，用圆圈序号而不是括号序号，每个式子用latex格式 写出，使用$$...$$作为块级公式分隔符，使用$...$作为行内公式分隔符。" +
            "此外对于光路和图示使用Python代码画出，允许使用numpy, matplotlib, scipy，受力分析等图像明确标出各个力。" +
            "注意plt使用plt.savefig()，文件名称是当前图像的内容，禁止使用plt.show()，图标内容不允许出现中文，保存路径/storage/emulated/0/Physic，保存过程中提示\"少 女画画中\"保存之后print\"她画完了！，文件名...\"提示。" +
            "此外为了让人看清楚，你的图像应该适当夸张来体现重点，比如一些比较近的 点，你可以画得开一点，并且应该用你式子中的字母标注图上的线段，角度等。" +
            "我不说画图可以不画图，要求制作视频时使用matplotlib+FFmpeg，保存路径和命名格式和之前一样，保存过程中提示\"少女录像中\"，保存完提示\"她录好了！文件叫做...\"";

    private static final String LATEX_TUTORIAL =
            "📐 LaTeX 常见语法\n\n" +
            "1️⃣ 行内公式  $...$\n   例：$E = mc^2$、$F = ma$\n\n" +
            "2️⃣ 块级公式  $$...$$\n   例：$$\\int_0^\\infty e^{-x^2}dx = \\frac{\\sqrt{\\pi}}{2}$$\n\n" +
            "3️⃣ 分式  \\frac{分子}{分母}\n   例：$\\frac{1}{2}$、$\\frac{dy}{dx}$\n\n" +
            "4️⃣ 上下标  ^ 和 _\n   例：$x^2$、$a_1$、$x_i^2$\n\n" +
            "5️⃣ 根号  \\sqrt{}\n   例：$\\sqrt{2}$、$\\sqrt[3]{8}$\n\n" +
            "6️⃣ 希腊字母\n   例：$\\alpha$、$\\beta$、$\\theta$、$\\pi$、$\\Delta$\n\n" +
            "7️⃣ 积分  \\int\n   例：$\\int_a^b f(x)dx$\n\n" +
            "8️⃣ 求和  \\sum\n   例：$\\sum_{i=1}^n i^2$\n\n" +
            "9️⃣ 向量  \\vec{}\n   例：$\\vec{F} = m\\vec{a}$\n\n" +
            "🔟 矩阵  \\begin{matrix}\n   例：$$\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}$$\n\n" +
            "在输入框中直接输入含 $...$ 或 $$...$$ 的文本即可自动渲染。";

    private OkHttpClient client;
    private Call currentCall;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ConversationManager convManager;

    private Conversation currentConversation;
    private boolean isWaitingResponse = false;
    private boolean hasSentSystemPrompt = false;
    private boolean webViewReady = false;

    private WebView chatWebView;

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "gamma_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_APP_THEME = "app_theme";
    private int themeMode = 0;
    private int appThemeIndex = 0;
    private boolean isDarkMode = false;
    private boolean hasSentFirstMessage = false;

    private Handler thinkingTimer = new Handler();
    private long thinkingStartTime = 0;
    private Runnable thinkingRunnable;

    private Bitmap currentImageBitmap;
    private boolean isSearchEnabled = false;
    private String originalUserQuery = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        themeMode = prefs.getInt(KEY_THEME_MODE, 0);
        appThemeIndex = prefs.getInt(KEY_APP_THEME, 0);
        detectAndApplyTheme();

        convManager = new ConversationManager(this);
        initViews();

        chatWebView = LatexUtils.createChatWebView(this);
        chatContainer.addView(chatWebView);

        mainHandler.postDelayed(() -> {
            if (webViewReady) {
                chatWebView.evaluateJavascript("setThemeMode(" + isDarkMode + ")", null);
                applyWebViewTheme();
            }
        }, 100);

        mainHandler.postDelayed(() -> {
            webViewReady = true;
            applyWebViewTheme();
            try {
                List<Conversation> existing = convManager.getAllConversations();
                if (!existing.isEmpty()) {
                    loadConversation(existing.get(0).id, false);
                } else {
                    createNewConversation(false);
                }
            } catch (Exception e) {
                createNewConversation(false);
            }
        }, 1500);
    }

    private void applyWebViewTheme() {
        if (!webViewReady || chatWebView == null) return;
        ThemeColor tc = ThemeManager.getTheme(appThemeIndex);

        int bgR = (tc.userBubble >> 16) & 0xFF;
        int bgG = (tc.userBubble >> 8) & 0xFF;
        int bgB = tc.userBubble & 0xFF;
        float lum = (0.299f * bgR + 0.587f * bgG + 0.114f * bgB) / 255f;
        String cuText = lum > 0.5f ? "#1C1B1F" : "#FFFFFF";

        int aiR = (tc.sendBtn >> 16) & 0xFF;
        int aiG = (tc.sendBtn >> 8) & 0xFF;
        int aiB = tc.sendBtn & 0xFF;
        float aiLum = (0.299f * aiR + 0.587f * aiG + 0.114f * aiB) / 255f;
        String caText = aiLum > 0.4f ? "#1C1B1F" : "#E0E0E0";

        String sColor, tColor, tkColor, thColor;
        if (isDarkMode) {
            sColor = "#CCCCCC"; tColor = "#888888"; tkColor = "#999999"; thColor = "#BBBBBB";
        } else {
            sColor = "#888888"; tColor = "#BBBBBB"; tkColor = "#BBBBBB"; thColor = "#999999";
        }

        String userBubble, aiBubble, aiShadow, welcomeTag, welcomeTitle, welcomeSub;
        if (isDarkMode) {
            welcomeTitle = "#CCCCCC"; welcomeSub = "#888888"; welcomeTag = "#81D4FA";
            userBubble = "linear-gradient(135deg, #1E3A5F, #2A5A8F)";
            aiBubble = "#2D2D3F";
            aiShadow = "rgba(0,0,0,0.3)";
            cuText = "#FFFFFF";
            caText = "#E0E0E0";
        } else {
            welcomeTitle = "#444444"; welcomeSub = "#999999";
            welcomeTag = String.format("#%06X", tc.accent & 0xFFFFFF);
            int r2 = (tc.userBubble >> 16) & 0xFF;
            int g2 = (tc.userBubble >> 8) & 0xFF;
            int b2 = tc.userBubble & 0xFF;
            String ubHex = String.format("#%02X%02X%02X", r2, g2, b2);
            String acHex = String.format("#%06X", tc.accent & 0xFFFFFF);
            userBubble = "linear-gradient(135deg, " + ubHex + ", " + acHex + ")";
            aiBubble = "#FFFFFF";
            aiShadow = "rgba(0,0,0,0.08)";
        }

        int accentR = (tc.accent >> 16) & 0xFF;
        int accentG = (tc.accent >> 8) & 0xFF;
        int accentB = tc.accent & 0xFF;

        String js = String.format(
            "setWebTheme(%d,%d,%d,'%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s');",
            accentR, accentG, accentB,
            userBubble, aiBubble, aiShadow,
            welcomeTitle, welcomeSub, welcomeTag,
            cuText, caText,
            sColor, tColor, tkColor, thColor
        );
        chatWebView.evaluateJavascript(js, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) handleImageSelected(uri);
        }
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK && data != null) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            if (photo != null) {
                int maxDim = 2048;
                int w = photo.getWidth(), h = photo.getHeight();
                float scale = Math.min((float) maxDim / w, (float) maxDim / h);
                if (scale < 1.0f) {
                    currentImageBitmap = Bitmap.createScaledBitmap(photo, (int)(w*scale), (int)(h*scale), true);
                    photo.recycle();
                } else {
                    currentImageBitmap = photo;
                }
                showImagePreview();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        drawerContent = findViewById(R.id.drawerContent);
        chatContainer = findViewById(R.id.chatContainer);
        conversationList = findViewById(R.id.conversationList);
        inputMessage = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.btnSend);
        btnNewFromDrawer = findViewById(R.id.btnNewFromDrawer);
        btnMenu = findViewById(R.id.btnMenu);
        btnNewChat = findViewById(R.id.btnNewChat);
        btnSettings = findViewById(R.id.btnSettings);
        btnDollar = findViewById(R.id.btnDollar);
        btnCaret = findViewById(R.id.btnCaret);
        btnUnderscore = findViewById(R.id.btnUnderscore);
        btnImage = findViewById(R.id.btnImage);
        btnSearch = findViewById(R.id.btnSearch);
        titleText = findViewById(R.id.titleText);
        conversationCount = findViewById(R.id.conversationCount);
        thinkingStatus = findViewById(R.id.thinkingStatus);

        btnDollar.setOnClickListener(v -> { int p=inputMessage.getSelectionStart(); if(p<0)p=inputMessage.getText().length(); inputMessage.getText().insert(p,"$"); animatePress(v); });
        btnCaret.setOnClickListener(v -> { int p=inputMessage.getSelectionStart(); if(p<0)p=inputMessage.getText().length(); inputMessage.getText().insert(p,"^"); animatePress(v); });
        btnUnderscore.setOnClickListener(v -> { int p=inputMessage.getSelectionStart(); if(p<0)p=inputMessage.getText().length(); inputMessage.getText().insert(p,"_"); animatePress(v); });

        btnImage.setOnClickListener(v -> {
            animatePress(v);
            new android.app.AlertDialog.Builder(this)
                    .setTitle("📷 选择图片来源")
                    .setItems(new String[]{"📁 从相册选择", "📸 拍照"}, (d, w) -> {
                        if (w == 0) {
                            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            intent.setType("image/*");
                            startActivityForResult(intent, REQUEST_PICK_IMAGE);
                        } else {
                            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                            } else {
                                openCamera();
                            }
                        }
                    }).show();
        });

        updateSearchButtonStyle();
        btnSearch.setOnClickListener(v -> { animatePress(v); isSearchEnabled=!isSearchEnabled; updateSearchButtonStyle(); Toast.makeText(MainActivity.this, isSearchEnabled?"🌐 联网搜索已开启":"🌐 联网搜索已关闭", Toast.LENGTH_SHORT).show(); });

        btnMenu.setOnClickListener(v -> { refreshConversationList(); drawerLayout.openDrawer(drawerContent); });

        btnSettings.setOnClickListener(v -> {
            animatePress(v);
            final String[] items = {"通用", "LaTeX 教程", "关于 Gamma"};
            new android.app.AlertDialog.Builder(this)
                    .setTitle("设置")
                    .setItems(items, (dialog, which) -> {
                        if (which == 0) showGeneralSettings();
                        else if (which == 1) {
                            new android.app.AlertDialog.Builder(this).setTitle("LaTeX 教程").setMessage(LATEX_TUTORIAL).setPositiveButton("确定", null).show();
                        } else {
                            String aboutMsg = "📐 理物 v" + APP_VERSION + "\n\n开发者：Castorice\n\n一个专为物理竞赛打造的 AI 助手，\n接入 DeepSeek V4 Flash，支持\nLaTeX 渲染、OCR 识图、联网搜索、\n多色主题、Python 绘图等。\n\n© 2026 Castorice. All rights reserved.";
                            new android.app.AlertDialog.Builder(this).setTitle("关于 Gamma").setMessage(aboutMsg).setPositiveButton("确定", null).show();
                        }
                    }).show();
        });

        View.OnClickListener newChatListener = v -> { animatePress(v); createNewConversation(false); };
        btnNewChat.setOnClickListener(newChatListener);
        btnNewFromDrawer.setOnClickListener(v -> { animatePress(v); drawerLayout.closeDrawer(drawerContent); createNewConversation(false); });

        btnSend.setOnClickListener(v -> {
            if (isWaitingResponse) { cancelCurrentRequest(); }
            else { String t = inputMessage.getText().toString().trim(); if(!t.isEmpty()){ inputMessage.setText(""); hideKeyboard(); sendUserMessage(t); } }
        });

        inputMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) inputMessage.animate().scaleX(1.02f).scaleY(1.02f).setDuration(150).start();
            else inputMessage.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
        });
    }

    private void openCamera() {
        startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQUEST_TAKE_PHOTO);
    }

    private void animatePress(View v) {
        v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(80).withEndAction(()->v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()).start();
    }

    private void updateSearchButtonStyle() {
        if (btnSearch == null) return;
        ThemeColor tc = ThemeManager.getTheme(appThemeIndex);
        if (isSearchEnabled) {
            btnSearch.setTextColor(tc.accent);
            btnSearch.setBackgroundColor((tc.accent & 0x00FFFFFF) | 0x33000000);
            btnSearch.setText("🌐");
        } else {
            int toolTextColor = isDarkMode ? 0xFFAAAAAA : 0xFF888888;
            btnSearch.setTextColor(toolTextColor);
            btnSearch.setBackgroundResource(isDarkMode ? R.drawable.btn_tool_dark : R.drawable.btn_tool);
            btnSearch.setText("🌐");
        }
    }

    private void showGeneralSettings() {
        final String[] items = {"深色模式", "主题"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("通用").setItems(items, (dialog, which) -> {
                    if (which == 0) showDarkModeSettings();
                    else if (which == 1) showThemeSettings();
                }).setNegativeButton("返回", null).show();
    }

    private void showDarkModeSettings() {
        final String[] themeItems = {"📱 跟随系统", "☀️ 浅色模式", "🌙 深色模式"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("深色模式")
                .setSingleChoiceItems(themeItems, themeMode, (d, w) -> {
                    themeMode = w;
                    prefs.edit().putInt(KEY_THEME_MODE, w).apply();
                    detectAndApplyTheme();
                    d.dismiss();
                }).setNegativeButton("取消", null).show();
    }

    private void showThemeSettings() {
        String[] themeLabels = new String[ThemeManager.THEMES.length];
        for (int i = 0; i < ThemeManager.THEMES.length; i++) {
            themeLabels[i] = ThemeManager.THEMES[i].name;
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle("选择主题")
                .setSingleChoiceItems(themeLabels, appThemeIndex, (d, w) -> {
                    appThemeIndex = w;
                    prefs.edit().putInt(KEY_APP_THEME, w).apply();
                    applyTheme();
                    d.dismiss();
                }).setNegativeButton("取消", null).show();
    }

    private void detectAndApplyTheme() {
        boolean systemDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        switch (themeMode) {
            case 0: isDarkMode = systemDark; break;
            case 1: isDarkMode = false; break;
            case 2: isDarkMode = true; break;
        }
        applyTheme();
    }

    private void applyTheme() {
        if (webViewReady && chatWebView != null) {
            chatWebView.evaluateJavascript("setThemeMode(" + isDarkMode + ")", null);
            applyWebViewTheme();
        }
        applyNativeTheme(isDarkMode);
    }

    private void applyNativeTheme(boolean isDark) {
        ThemeColor tc = ThemeManager.getTheme(appThemeIndex);

        LinearLayout topBar = findViewById(R.id.topBar);
        if (topBar != null) topBar.setBackgroundColor(isDark ? 0xFF1E3A5F : tc.topbar);
        LinearLayout bottomBar = findViewById(R.id.bottomBar);
        if (bottomBar != null) bottomBar.setBackgroundColor(isDark ? 0xFF2D2D2D : 0xFFFFFFFF);
        LinearLayout toolRow = findViewById(R.id.toolRow);
        if (toolRow != null) toolRow.setBackgroundColor(isDark ? tc.toolRowDark : tc.toolRow);
        EditText input = findViewById(R.id.inputMessage);
        if (input != null) {
            input.setBackgroundResource(isDark ? R.drawable.input_bg_dark : R.drawable.input_bg);
            input.setTextColor(isDark ? 0xFFE0E0E0 : 0xFF1C1B1F);
            input.setHintTextColor(isDark ? 0xFF888888 : 0xFF9E9E9E);
        }
        LinearLayout drawerContent = findViewById(R.id.drawerContent);
        if (drawerContent != null) drawerContent.setBackgroundColor(isDark ? 0xFF1E1E1E : 0xFFF0F8FF);
        LinearLayout drawerHeader = findViewById(R.id.drawerHeader);
        if (drawerHeader != null) drawerHeader.setBackgroundColor(isDark ? 0xFF1E3A5F : tc.drawerHeader);
        TextView convCount = findViewById(R.id.conversationCount);
        if (convCount != null) convCount.setTextColor(isDark ? 0xFF90CAF9 : 0xFFE0F7FA);
        TextView status = findViewById(R.id.thinkingStatus);
        if (status != null) status.setTextColor(isDark ? 0xFFAAAAAA : 0xFF888888);
        Button sendBtn = findViewById(R.id.btnSend);
        if (sendBtn != null) sendBtn.setBackgroundColor(isDark ? tc.sendBtnDark : tc.sendBtn);

        int toolTextColor = isDark ? 0xFFAAAAAA : 0xFF888888;
        int toolBg = isDark ? R.drawable.btn_tool_dark : R.drawable.btn_tool;
        if (btnDollar != null) { btnDollar.setBackgroundResource(toolBg); btnDollar.setTextColor(toolTextColor); }
        if (btnCaret != null) { btnCaret.setBackgroundResource(toolBg); btnCaret.setTextColor(toolTextColor); }
        if (btnUnderscore != null) { btnUnderscore.setBackgroundResource(toolBg); btnUnderscore.setTextColor(toolTextColor); }
        if (btnImage != null) { btnImage.setBackgroundResource(toolBg); btnImage.setTextColor(toolTextColor); }
        updateSearchButtonStyle();
        refreshConversationList();
    }

    private void handleImageSelected(Uri uri) {
        try {
            Bitmap original = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
            if (original == null) { Toast.makeText(this, "无法解码图片", Toast.LENGTH_SHORT).show(); return; }
            int maxDim = 2048;
            int w = original.getWidth(), h = original.getHeight();
            float scale = Math.min((float) maxDim / w, (float) maxDim / h);
            if (scale < 1.0f) { currentImageBitmap = Bitmap.createScaledBitmap(original, (int)(w*scale), (int)(h*scale), true); original.recycle(); }
            else { currentImageBitmap = original; }
        } catch (Exception e) { Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show(); return; }
        showImagePreview();
    }

    private void showImagePreview() {
        if (currentImageBitmap == null) return;
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(currentImageBitmap);
        imageView.setMaxHeight(800); imageView.setAdjustViewBounds(true); imageView.setPadding(20,20,20,20);
        imageView.setAlpha(0f); imageView.animate().alpha(1f).setDuration(300).start();
        new android.app.AlertDialog.Builder(this)
                .setTitle("📷 识图预览").setView(imageView)
                .setMessage("识别图片中的文字并发送到对话")
                .setPositiveButton("发送到对话", (d,w) -> performOcrAndSend())
                .setNegativeButton("取消", (d,w) -> { currentImageBitmap = null; }).show();
    }

    private void performOcrAndSend() {
        if (currentImageBitmap == null) return;
        thinkingStatus.setText("📷 识别中...");
        new Thread(() -> {
            try {
                String ocrText = OcrHelper.recognizeText(currentImageBitmap);
                mainHandler.post(() -> {
                    thinkingStatus.setText("");
                    if (ocrText.isEmpty()) { Toast.makeText(MainActivity.this, "未识别到文字", Toast.LENGTH_SHORT).show(); return; }
                    doSendUserMessage("📷 [图片识别结果]\n" + ocrText);
                    currentImageBitmap = null;
                });
            } catch (Exception e) {
                mainHandler.post(() -> { thinkingStatus.setText(""); Toast.makeText(MainActivity.this, "识别失败："+e.getMessage(), Toast.LENGTH_SHORT).show(); currentImageBitmap = null; });
            }
        }).start();
    }

    private void sendUserMessage(String text) {
        if (!hasSentFirstMessage) { hasSentFirstMessage = true; if (webViewReady) chatWebView.evaluateJavascript("hideWelcome()", null); }
        if (isSearchEnabled && !text.startsWith("📷")) { performSearchAndSendToDS(text); }
        else { doSendUserMessage(text); }
    }

    private void performSearchAndSendToDS(String query) {
        isWaitingResponse = true; btnSend.setText("取消"); thinkingStatus.setText("🧠 提取关键词...");
        String htmlSafe = query.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
        addUserMsgToWebView(htmlSafe);
        originalUserQuery = query;
        currentConversation.messages.add(new Conversation.Message("user", query));
        convManager.updateTitle(currentConversation);
        titleText.setText(currentConversation.title);

        new Thread(() -> {
            try {
                String keywords = KeywordExtractor.extractKeywords(query);
                mainHandler.post(() -> thinkingStatus.setText("🌐 搜索关键词：" + keywords));
                long searchStart = System.currentTimeMillis();
                List<SearchEngine.SearchResult> results = SearchEngine.search(keywords);
                long searchTime = (System.currentTimeMillis() - searchStart) / 1000;
                StringBuilder sc = new StringBuilder();
                sc.append("【以下是从 Bing 和百度搜索到的相关信息】\n搜索关键词：").append(keywords).append("\n\n");
                if (results.isEmpty()) sc.append("⚠️ 未搜索到相关结果，请基于你的知识回答。\n\n");
                else {
                    for (int i = 0; i < results.size(); i++) {
                        SearchEngine.SearchResult r = results.get(i);
                        sc.append("📄 ").append(r.title).append("\n   ").append(r.snippet).append("\n   来源：").append(r.url).append("\n\n");
                    }
                }
                sc.append("搜索耗时：").append(searchTime).append("s\n请基于以上信息回答。如果搜索结果与问题无关，请忽略并基于你的知识回答。\n\n用户问题：").append(query);
                final String finalText = sc.toString();
                mainHandler.post(() -> {
                    thinkingStatus.setText("⏳ 0s"); startThinkingTimer();
                    if (webViewReady) chatWebView.evaluateJavascript("showThinking()", null);
                    Conversation.Message lastMsg = currentConversation.messages.get(currentConversation.messages.size()-1);
                    String orig = lastMsg.content;
                    lastMsg.content = finalText;
                    new Thread(() -> {
                        try {
                            JSONObject body = buildChatRequest(finalText);
                            String resultJson = callApiRaw(body);
                            lastMsg.content = orig;
                            if (Thread.currentThread().isInterrupted()) return;
                            JSONObject rj = new JSONObject(resultJson);
                            String fc = rj.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                            JSONObject usage = rj.optJSONObject("usage");
                            String ti = null;
                            if (usage != null) ti = String.format("⬆ %d  ⬇ %d  ∑ %d tokens", usage.optInt("prompt_tokens",0), usage.optInt("completion_tokens",0), usage.optInt("total_tokens",0));
                            final String tdi = ti; final String c = fc;
                            mainHandler.post(() -> {
                                currentConversation.messages.add(new Conversation.Message("ai", c));
                                convManager.saveConversation(currentConversation);
                                addAiMsgToWebView(c, tdi);
                                isWaitingResponse=false; btnSend.setText("发送"); thinkingStatus.setText(""); stopThinkingTimer();
                                originalUserQuery=null;
                            });
                        } catch (Exception e) {
                            if (currentConversation.messages.size()>0) { Conversation.Message lm = currentConversation.messages.get(currentConversation.messages.size()-1); if(lm.content.contains("【以下")) lm.content = originalUserQuery!=null?originalUserQuery:""; }
                            String em = e.getMessage()!=null?e.getMessage():"未知错误";
                            if(em.contains("Canceled")||em.contains("canceled")||em.contains("Socket closed")){ mainHandler.post(()->{isWaitingResponse=false;btnSend.setText("发送");thinkingStatus.setText("");stopThinkingTimer();if(webViewReady)chatWebView.evaluateJavascript("hideThinking()",null);}); return; }
                            mainHandler.post(()->{addAiMsgToWebView("⚠️ 请求失败："+em,null);isWaitingResponse=false;btnSend.setText("发送");thinkingStatus.setText("");stopThinkingTimer();});
                        }
                    }).start();
                });
            } catch (Exception e) {
                mainHandler.post(() -> { thinkingStatus.setText("🌐 搜索失败，改用普通模式"); Toast.makeText(MainActivity.this, "搜索失败："+e.getMessage(), Toast.LENGTH_SHORT).show(); mainHandler.postDelayed(()->{thinkingStatus.setText("⏳ 0s");doSendUserMessage(query);},500); });
            }
        }).start();
    }

    private void cancelCurrentRequest() {
        if (currentCall != null && !currentCall.isCanceled()) { currentCall.cancel(); currentCall = null; }
        isWaitingResponse=false; btnSend.setText("发送"); btnSend.setEnabled(true); thinkingStatus.setText(""); stopThinkingTimer();
        if (webViewReady) chatWebView.evaluateJavascript("hideThinking()", null);
    }

    private void startThinkingTimer() {
        thinkingStartTime = System.currentTimeMillis();
        thinkingTimer.removeCallbacksAndMessages(null);
        thinkingRunnable = new Runnable() {
            @Override
            public void run() { if (isWaitingResponse) { long e = (System.currentTimeMillis()-thinkingStartTime)/1000; thinkingStatus.setText("⏳ "+e+"s"); thinkingTimer.postDelayed(this,1000); } }
        };
        thinkingTimer.postDelayed(thinkingRunnable, 1000);
    }

    private void stopThinkingTimer() { thinkingTimer.removeCallbacksAndMessages(null); }

    private void createNewConversation(boolean unused) {
        try { if (currentConversation != null && currentConversation.messages.size() > 0) convManager.saveConversation(currentConversation); } catch (Exception ignored) {}
        if (currentCall != null) { currentCall.cancel(); currentCall = null; }
        currentConversation = convManager.createNewConversation();
        titleText.setText("理物"); isWaitingResponse=false; hasSentSystemPrompt=false;
        btnSend.setText("发送"); btnSend.setEnabled(true); thinkingStatus.setText("");
        hasSentFirstMessage=false;
        if (webViewReady) { chatWebView.evaluateJavascript("clearChat()",null); chatWebView.evaluateJavascript("showWelcome()",null); }
        refreshConversationList();
    }

    private void loadConversation(String convId, boolean closeDrawer) {
        try {
            Conversation conv = convManager.loadConversation(convId);
            if (conv == null) return;
            if (currentConversation != null && currentConversation.messages.size() > 0) convManager.saveConversation(currentConversation);
            if (currentCall != null) { currentCall.cancel(); currentCall = null; }
            currentConversation = conv; isWaitingResponse=false; titleText.setText(conv.title);
            hasSentSystemPrompt = conv.messages.size() > 0;
            btnSend.setText("发送"); btnSend.setEnabled(true); thinkingStatus.setText("");
            hasSentFirstMessage = conv.messages.size() > 0;
            if (webViewReady) {
                chatWebView.evaluateJavascript("clearChat()",null);
                if (conv.messages.isEmpty()) chatWebView.evaluateJavascript("showWelcome()",null);
                else chatWebView.evaluateJavascript("hideWelcome()",null);
                mainHandler.postDelayed(()->{ for(Conversation.Message msg:conv.messages){ if("user".equals(msg.role)) addUserMsgToWebView(msg.content); else addAiMsgToWebView(msg.content,null); } },300);
            }
            if (closeDrawer) drawerLayout.closeDrawer(drawerContent);
        } catch (Exception ignored) {}
    }

    private void loadConversation(String convId) { loadConversation(convId, true); }

    private void doSendUserMessage(String text) {
        if (!hasSentFirstMessage) { hasSentFirstMessage=true; if(webViewReady) chatWebView.evaluateJavascript("hideWelcome()",null); }
        isWaitingResponse=true; btnSend.setText("取消"); btnSend.setEnabled(true); thinkingStatus.setText("⏳ 0s"); startThinkingTimer();
        if (webViewReady) chatWebView.evaluateJavascript("showThinking()",null);
        String safe = text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
        addUserMsgToWebView(safe);
        try { currentConversation.messages.add(new Conversation.Message("user",text)); convManager.updateTitle(currentConversation); titleText.setText(currentConversation.title); } catch(Exception ignored){}
        final long st = System.currentTimeMillis();
        new Thread(()->{
            try {
                JSONObject body = buildChatRequest(text);
                String rj = callApiRaw(body);
                if (Thread.currentThread().isInterrupted()) return;
                JSONObject jo = new JSONObject(rj);
                String fc = jo.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                JSONObject usage = jo.optJSONObject("usage");
                String ti = null;
                if (usage != null) ti = String.format("⬆ %d  ⬇ %d  ∑ %d tokens", usage.optInt("prompt_tokens",0), usage.optInt("completion_tokens",0), usage.optInt("total_tokens",0));
                final String tdi=ti; final String c=fc;
                mainHandler.post(()->{ currentConversation.messages.add(new Conversation.Message("ai",c)); convManager.saveConversation(currentConversation); addAiMsgToWebView(c,tdi); isWaitingResponse=false; btnSend.setText("发送"); thinkingStatus.setText(""); stopThinkingTimer(); });
            } catch(Exception e) {
                String em = e.getMessage()!=null?e.getMessage():"未知错误";
                if(em.contains("Canceled")||em.contains("canceled")||em.contains("Socket closed")){ mainHandler.post(()->{isWaitingResponse=false;btnSend.setText("发送");thinkingStatus.setText("");stopThinkingTimer();if(webViewReady)chatWebView.evaluateJavascript("hideThinking()",null);}); return; }
                mainHandler.post(()->{addAiMsgToWebView("⚠️ 请求失败："+em,null);isWaitingResponse=false;btnSend.setText("发送");thinkingStatus.setText("");stopThinkingTimer();});
            }
        }).start();
    }

    private void addUserMsgToWebView(String text) { chatWebView.evaluateJavascript("addUserMsg("+JSONObject.quote(text)+")",null); }

    private void addAiMsgToWebView(String content, String tokenInfo) {
        String html = convertToHtml(content);
        chatWebView.evaluateJavascript("addAiMsg("+JSONObject.quote(html)+","+JSONObject.quote(content)+","+(tokenInfo!=null?JSONObject.quote(tokenInfo):"null")+")",null);
    }

    private String convertToHtml(String content) {
        StringBuilder html = new StringBuilder();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("```(\\w*)\\n([\\s\\S]*?)```", java.util.regex.Pattern.MULTILINE).matcher(content);
        int lastEnd = 0;
        while (matcher.find()) {
            html.append(escapeHtml(content.substring(lastEnd,matcher.start())).replace("\n","<br>"));
            html.append("<div class=\"cb\"><div class=\"ch\"><span>").append(matcher.group(1).isEmpty()?"code":matcher.group(1)).append("</span>");
            html.append("<button onclick=\"copyCode(this)\">📋复制</button></div><pre><code>").append(escapeHtml(matcher.group(2))).append("</code></pre></div>");
            lastEnd = matcher.end();
        }
        html.append(escapeHtml(content.substring(lastEnd)).replace("\n","<br>"));
        return html.toString();
    }

    private String escapeHtml(String text) { return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;"); }

    private JSONObject buildChatRequest(String userText) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", MODEL_NAME);
        JSONArray messages = new JSONArray();
        if (!hasSentSystemPrompt) {
            JSONObject sys = new JSONObject(); sys.put("role","system"); sys.put("content",SYSTEM_PROMPT);
            messages.put(sys); hasSentSystemPrompt=true;
        }
        for (Conversation.Message msg : currentConversation.messages) {
            JSONObject jm = new JSONObject(); jm.put("role","user".equals(msg.role)?"user":"assistant"); jm.put("content",msg.content);
            messages.put(jm);
        }
        body.put("messages",messages); body.put("stream",false);
        return body;
    }

    private String callApiRaw(JSONObject requestBody) throws IOException {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        Request request = new Request.Builder().url(API_URL).addHeader("Authorization","Bearer "+API_KEY).addHeader("Content-Type","application/json").post(RequestBody.create(requestBody.toString(),JSON)).build();
        currentCall = client.newCall(request);
        try (Response response = currentCall.execute()) {
            String body = response.body().string();
            if (!response.isSuccessful()) throw new IOException("API错误 "+response.code()+": "+body);
            return body;
        }
    }

    private void showRenameDialog(Conversation conv) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40,20,40,10);
        final EditText input = new EditText(this);
        input.setText(conv.title); input.setSelection(conv.title.length()); input.setPadding(16,12,16,12);
        layout.addView(input);
        new android.app.AlertDialog.Builder(this).setTitle("重命名对话").setView(layout)
                .setPositiveButton("确定",(d,w)->{ String nt=input.getText().toString().trim(); if(!nt.isEmpty()){ conv.title=nt; conv.timestamp=System.currentTimeMillis(); convManager.saveConversation(conv); if(conv.id.equals(currentConversation.id)) titleText.setText(nt); refreshConversationList(); } })
                .setNegativeButton("取消",null).show();
    }

    private void refreshConversationList() {
        try {
            conversationList.removeAllViews();
            List<Conversation> all = convManager.getAllConversations();
            conversationCount.setText(all.size()+"  个对话");
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
            for (Conversation conv : all) {
                boolean isActive = conv.id.equals(currentConversation.id);
                final String convId = conv.id;
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.HORIZONTAL); card.setPadding(12,12,12,12);
                card.setGravity(android.view.Gravity.CENTER_VERTICAL);
                if (isDarkMode) card.setBackgroundColor(isActive ? 0xFF2D3A4A : 0xFF2D2D2D);
                else { if(isActive) card.setBackgroundColor(0xFFE3F2FD); else card.setBackgroundResource(R.drawable.card_bg); }
                LinearLayout.LayoutParams cardP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                cardP.setMargins(4,4,4,4); card.setLayoutParams(cardP); card.setElevation(isActive ? 4 : 1);

                LinearLayout textArea = new LinearLayout(this);
                textArea.setOrientation(LinearLayout.VERTICAL);
                textArea.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                LinearLayout titleRow = new LinearLayout(this);
                titleRow.setOrientation(LinearLayout.HORIZONTAL); titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
                if (conv.pinned) { TextView pin = new TextView(this); pin.setText("📌 "); pin.setTextSize(12); titleRow.addView(pin); }
                TextView title = new TextView(this);
                title.setText(conv.title); title.setTextSize(13);
                if (isDarkMode) title.setTextColor(0xFFE0E0E0); else title.setTextColor(0xFF1C1B1F);
                title.setMaxLines(1); titleRow.addView(title); textArea.addView(titleRow);
                TextView info = new TextView(this);
                info.setText(sdf.format(new Date(conv.timestamp))+"  ·  "+conv.messages.size()+"条消息"); info.setTextSize(11);
                if (isDarkMode) info.setTextColor(0xFFAAAAAA); else info.setTextColor(0xFF888888);
                info.setPadding(0,4,0,0); textArea.addView(info); card.addView(textArea);

                ImageButton moreBtn = new ImageButton(this);
                moreBtn.setImageResource(android.R.drawable.ic_menu_more); moreBtn.setBackground(null); moreBtn.setPadding(8,8,8,8);
                moreBtn.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
                int iconColor = isDarkMode ? 0xFFAAAAAA : 0xFF888888;
                moreBtn.setColorFilter(iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                bp.setMargins(4,0,0,0); moreBtn.setLayoutParams(bp);
                moreBtn.setOnClickListener(v -> {
                    PopupMenu popup = new PopupMenu(this, v);
                    popup.getMenu().add(0,1,0,"✏️ 重命名"); popup.getMenu().add(0,2,1,conv.pinned?"📌 取消置顶":"📌 置顶"); popup.getMenu().add(0,3,2,"🗑️ 删除");
                    popup.setOnMenuItemClickListener(item -> {
                        int id = item.getItemId();
                        if (id==1) { showRenameDialog(conv); return true; }
                        else if (id==2) { conv.pinned=!conv.pinned; convManager.saveConversation(conv); refreshConversationList(); return true; }
                        else if (id==3) { new android.app.AlertDialog.Builder(this).setTitle("删除对话").setMessage("确定要删除？").setPositiveButton("删除",(d,w)->{ if(conv.id.equals(currentConversation.id)) createNewConversation(false); convManager.deleteConversation(conv.id); refreshConversationList(); }).setNegativeButton("取消",null).show(); return true; }
                        return false;
                    }); popup.show();
                });
                card.addView(moreBtn);
                card.setAlpha(0f); card.setTranslationY(20f);
                card.animate().alpha(1f).translationY(0).setDuration(250).setStartDelay(30*conversationList.getChildCount()).start();
                card.setOnClickListener(v -> loadConversation(convId));
                conversationList.addView(card);
            }
        } catch (Exception ignored) {}
    }

    private void hideKeyboard() { InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE); if (imm != null) imm.hideSoftInputFromWindow(inputMessage.getWindowToken(), 0); }
}
