package com.example.helloworld;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.*;
import androidx.drawerlayout.widget.DrawerLayout;
import com.example.helloworld.latex.MathJaxRenderer;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    private DrawerLayout drawerLayout;
    private LinearLayout drawerContent;
    private LinearLayout chatLayout;
    private LinearLayout conversationList;
    private EditText inputMessage;
    private Button btnSend, btnNewFromDrawer;
    private ImageButton btnMenu, btnNewChat;
    private ScrollView scrollView;
    private TextView titleText, conversationCount;

    private static final String API_KEY = "sk-2a9eb729832b4bdebdbb8e095f6c28fb";
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String MODEL_NAME = "deepseek-reasoner";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ConversationManager convManager;
    private MathJaxRenderer mathJax;

    private Conversation currentConversation;
    private boolean isWaitingResponse = false;
    private boolean hasSentSystemPrompt = false;
    private boolean userScrolling = false; // 用户是否正在手动滚动

    private static final String SYSTEM_PROMPT = "你是一个物理竞赛指导教师，注意你的回答里不能存在说教等行为。" +
            "我会问你物理竞赛题，可能很难也可能很简单。有时我会想你阐明对于一道题目我自己的思路，这时候你需要提出可能会让下次做题时犯错的问题来强化我对知识的理解。" +
            "你的回答应该具有专业性，但注意不要使用难度过高的数学技巧，尽量做到不跳步骤，回答清晰。注意式子标注序号，用圆圈序号而不是括号序号，每个式子用latex格式写出，使用$$...$$作为块级公式分隔符，使用$...$作为行内公式分隔符。" +
            "此外对于光路和图示使用Python代码画出，允许使用numpy, matplotlib, scipy，受力分析等图像明确标出各个力。" +
            "注意plt使用plt.savefig()，文件名称是当前图像的内容，禁止使用plt.show()，图标内容不允许出现中文，保存路径/storage/emulated/0/Physic，保存过程中提示\"少女画画中\"保存之后print\"她画完了！，文件名...\"提示。" +
            "此外为了让人看清楚，你的图像应该适当夸张来体现重点，比如一些比较近的点，你可以画得开一点，并且应该用你式子中的字母标注图上的线段，角度等。" +
            "我不说画图可以不画图，要求制作视频时使用matplotlib+FFmpeg，保存路径和命名格式和之前一样，保存过程中提示\"少女录像中\"，保存完提示\"她录好了！文件叫做...\"";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        convManager = new ConversationManager(this);
        mathJax = new MathJaxRenderer(this);
        initViews();

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
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        drawerContent = findViewById(R.id.drawerContent);
        chatLayout = findViewById(R.id.chatLayout);
        conversationList = findViewById(R.id.conversationList);
        inputMessage = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.btnSend);
        btnNewFromDrawer = findViewById(R.id.btnNewFromDrawer);
        btnMenu = findViewById(R.id.btnMenu);
        btnNewChat = findViewById(R.id.btnNewChat);
        scrollView = findViewById(R.id.scrollView);
        titleText = findViewById(R.id.titleText);
        conversationCount = findViewById(R.id.conversationCount);

        // ★ 检测用户是否在手动滚动 ★
        scrollView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                case android.view.MotionEvent.ACTION_MOVE:
                    userScrolling = true;
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    userScrolling = false;
                    break;
            }
            return false; // 不拦截事件
        });

        btnMenu.setOnClickListener(v -> {
            refreshConversationList();
            drawerLayout.openDrawer(drawerContent);
        });

        View.OnClickListener newChatListener = v -> createNewConversation(false);
        btnNewChat.setOnClickListener(newChatListener);
        btnNewFromDrawer.setOnClickListener(v -> {
            drawerLayout.closeDrawer(drawerContent);
            createNewConversation(false);
        });

        btnSend.setOnClickListener(v -> {
            String text = inputMessage.getText().toString().trim();
            if (!text.isEmpty() && !isWaitingResponse) {
                sendUserMessage(text);
                inputMessage.setText("");
                hideKeyboard();
            }
        });
    }

    private void createNewConversation(boolean unused) {
        try {
            if (currentConversation != null && currentConversation.messages.size() > 0) {
                convManager.saveConversation(currentConversation);
            }
        } catch (Exception ignored) {}
        currentConversation = convManager.createNewConversation();
        chatLayout.removeAllViews();
        titleText.setText("Gamma");
        isWaitingResponse = false;
        hasSentSystemPrompt = false;
        refreshConversationList();
    }

    private void sendUserMessage(String text) {
        isWaitingResponse = true;
        addMessageBubble("🧑 我", text, true, true);

        try {
            currentConversation.messages.add(new Conversation.Message("user", text));
            convManager.updateTitle(currentConversation);
            titleText.setText(currentConversation.title);
        } catch (Exception ignored) {}

        final LinearLayout loadingView = createLoadingView();
        final long startTime = System.currentTimeMillis();

        new Thread(() -> {
            try {
                JSONObject body = buildChatRequest(text);
                String resultJson = callApiRaw(body);
                JSONObject responseJson = new JSONObject(resultJson);
                double thinkingSec = (System.currentTimeMillis() - startTime) / 1000.0;

                JSONObject message = responseJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message");
                String reasoningContent = message.optString("reasoning_content", "");
                String finalContent = message.getString("content");
                String thinkingLabel = String.format(" (思考 %.1fs)", thinkingSec);

                mainHandler.post(() -> {
                    try {
                        chatLayout.removeView(loadingView);
                        if (!reasoningContent.isEmpty()) {
                            addReasoningView(reasoningContent, thinkingLabel);
                        }
                        currentConversation.messages.add(new Conversation.Message("ai", finalContent));
                        convManager.saveConversation(currentConversation);
                        addMessageBubble("🤖 Gamma" + thinkingLabel, finalContent, false, true);
                        isWaitingResponse = false;
                    } catch (Exception ignored) {
                        isWaitingResponse = false;
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    try {
                        chatLayout.removeView(loadingView);
                        addMessageBubble("🤖 Gamma", "⚠️ 请求失败：" + e.getMessage(), false, true);
                    } catch (Exception ignored) {}
                    isWaitingResponse = false;
                });
            }
        }).start();
    }

    // ===== 消息气泡 =====
    private void addMessageBubble(String sender, String content, boolean isUser, boolean scrollToEnd) {
        LinearLayout container = new LinearLayout(this);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        containerParams.setMargins(0, 6, 0, 6);
        container.setLayoutParams(containerParams);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(isUser ? 60 : 0, 0, isUser ? 0 : 60, 0);

        // 发送者标签
        TextView senderLabel = new TextView(this);
        senderLabel.setText(sender);
        senderLabel.setTextSize(11);
        senderLabel.setTextColor(0xFF888888);
        senderLabel.setPadding(isUser ? 0 : 8, 0, isUser ? 8 : 0, 2);
        senderLabel.setGravity(isUser ? Gravity.END : Gravity.START);
        container.addView(senderLabel);

        // 内容
        if (mathJax.containsLatex(content) || content.contains("```")) {
            renderMixedContent(container, content, isUser, scrollToEnd);
        } else {
            addPlainText(container, content, isUser);
        }

        // 时间戳
        TextView timeLabel = new TextView(this);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeLabel.setText(sdf.format(new Date()));
        timeLabel.setTextSize(10);
        timeLabel.setTextColor(0xFFBBBBBB);
        timeLabel.setPadding(isUser ? 0 : 8, 2, isUser ? 8 : 0, 0);
        timeLabel.setGravity(isUser ? Gravity.END : Gravity.START);
        container.addView(timeLabel);

        // 淡入动画
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(400);
        container.startAnimation(fadeIn);

        chatLayout.addView(container);

        // ★ 只在添加新消息时滚动到底部，且只在用户没有手动滚动时 ★
        if (scrollToEnd) {
            mainHandler.postDelayed(() -> scrollToBottomIfNeeded(), 300);
        }
    }

    // ===== 混合内容渲染 =====
    private void renderMixedContent(LinearLayout container, String content, boolean isUser, boolean scrollToEnd) {
        String[] parts = content.split("(?=```)");
        for (String part : parts) {
            if (part.startsWith("```")) {
                addCodeBlock(container, part, isUser);
            } else if (mathJax.containsLatex(part)) {
                renderLatex(container, part, isUser, scrollToEnd);
            } else {
                addPlainText(container, part, isUser);
            }
        }
    }

    // ===== LaTeX 渲染 =====
    private void renderLatex(LinearLayout container, String text, boolean isUser, boolean scrollToEnd) {
        // 分割普通文本和公式
        List<Segment> segments = splitText(text);
        for (Segment seg : segments) {
            if (seg.isLatex) {
                WebView lv = mathJax.render(seg.content, isUser, () -> {
                    // ★ 只在用户没有手动滚动时，才滚动到底部 ★
                    mainHandler.postDelayed(() -> scrollToBottomIfNeeded(), 100);
                });
                container.addView(lv);
            } else if (!seg.content.trim().isEmpty()) {
                addPlainText(container, seg.content, isUser);
            }
        }
    }

    // ===== 分割文本 =====
    private List<Segment> splitText(String text) {
        List<Segment> segments = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int nextDD = text.indexOf("$$", i);
            int nextBL = text.indexOf("\\[", i);
            int nextPL = text.indexOf("\\(", i);
            int nextSD = text.indexOf("$", i);
            // 跳过 $$ 中的 $
            if (nextSD >= 0 && nextSD + 1 < text.length() && text.charAt(nextSD + 1) == '$') {
                nextSD = text.indexOf("$", nextSD + 1);
            }

            int minPos = Integer.MAX_VALUE;
            String startDelim = null;
            String endDelim = null;

            if (nextDD >= i && nextDD < minPos) { minPos = nextDD; startDelim = "$$"; endDelim = "$$"; }
            if (nextBL >= i && nextBL < minPos) { minPos = nextBL; startDelim = "\\["; endDelim = "\\]"; }
            if (nextPL >= i && nextPL < minPos) { minPos = nextPL; startDelim = "\\("; endDelim = "\\)"; }
            if (nextSD >= i && nextSD < minPos && nextSD > 0) { 
                minPos = nextSD; startDelim = "$"; endDelim = "$"; 
            }

            if (startDelim == null) {
                if (i < text.length()) segments.add(new Segment(text.substring(i), false));
                break;
            }

            if (minPos > i) segments.add(new Segment(text.substring(i, minPos), false));

            int formulaStart = minPos + startDelim.length();
            int endPos = text.indexOf(endDelim, formulaStart);
            if (endPos >= 0) {
                String formula = text.substring(formulaStart, endPos).trim();
                if (!formula.isEmpty()) segments.add(new Segment(formula, true));
                i = endPos + endDelim.length();
            } else {
                String rest = text.substring(formulaStart).trim();
                if (!rest.isEmpty()) segments.add(new Segment(rest, true));
                i = text.length();
            }
        }
        return segments;
    }

    // ===== 纯文本 =====
    private void addPlainText(LinearLayout container, String text, boolean isUser) {
        if (text.trim().isEmpty()) return;
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(0xFF1C1B1F);
        tv.setLineSpacing(6, 1);
        tv.setPadding(16, 8, 16, 8);
        tv.setBackgroundResource(isUser ? R.drawable.bubble_user : R.drawable.bubble_ai);
        if (!isUser) tv.setElevation(2);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = isUser ? Gravity.END : Gravity.START;
        params.setMargins(0, 2, 0, 2);
        tv.setLayoutParams(params);
        container.addView(tv);
    }

    // ===== 代码块 =====
    private void addCodeBlock(LinearLayout container, String codeBlock, boolean isUser) {
        String code = codeBlock.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
        LinearLayout codeContainer = new LinearLayout(this);
        codeContainer.setOrientation(LinearLayout.VERTICAL);
        codeContainer.setBackgroundColor(0xFF2D2D2D);
        codeContainer.setPadding(12, 10, 12, 10);
        codeContainer.setBackgroundResource(R.drawable.code_bg);
        String lang = "";
        if (codeBlock.startsWith("```python")) lang = "Python";
        if (!lang.isEmpty()) {
            TextView langLabel = new TextView(this);
            langLabel.setText("  " + lang); langLabel.setTextSize(10); langLabel.setTextColor(0xFF888888);
            langLabel.setPadding(0, 0, 0, 6); codeContainer.addView(langLabel);
        }
        TextView codeView = new TextView(this);
        codeView.setText(code); codeView.setTextSize(11); codeView.setTextColor(0xFFD4D4D4);
        codeView.setTypeface(Typeface.MONOSPACE); codeView.setLineSpacing(4, 1); codeView.setPadding(0, 0, 0, 0);
        codeContainer.addView(codeView);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.START; params.setMargins(0, 6, 0, 6);
        codeContainer.setLayoutParams(params);
        container.addView(codeContainer);
    }

    private void addReasoningView(String reasoning, String thinkingLabel) {
        LinearLayout container = new LinearLayout(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 6, 0, 2); container.setLayoutParams(cp);
        container.setOrientation(LinearLayout.VERTICAL); container.setPadding(0, 0, 60, 0);
        TextView label = new TextView(this);
        label.setText("🤔 思考过程" + thinkingLabel); label.setTextSize(11); label.setTextColor(0xFF888888);
        label.setPadding(8, 0, 0, 2); container.addView(label);
        TextView content = new TextView(this);
        content.setText(reasoning); content.setTextSize(12); content.setTextColor(0xFFAAAAAA);
        content.setLineSpacing(4, 1); content.setPadding(16, 8, 16, 8);
        content.setBackgroundColor(0xFFF8F8F8); content.setMaxLines(6);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.START; content.setLayoutParams(params); container.addView(content);
        final boolean[] expanded = {false};
        container.setOnClickListener(v -> {
            expanded[0] = !expanded[0]; content.setMaxLines(expanded[0] ? 200 : 6);
        });
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(300); container.startAnimation(fadeIn);
        chatLayout.addView(container);
    }

    // ===== ★ 智能滚动：只在用户没有手动操作时滚动 ★ =====
    private void scrollToBottomIfNeeded() {
        if (!userScrolling) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    // ===== API 调用 =====
    private JSONObject buildChatRequest(String userText) throws Exception {
        JSONObject body = new JSONObject(); body.put("model", MODEL_NAME);
        JSONArray messages = new JSONArray();
        if (!hasSentSystemPrompt) {
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system"); systemMsg.put("content", SYSTEM_PROMPT);
            messages.put(systemMsg); hasSentSystemPrompt = true;
        }
        for (Conversation.Message msg : currentConversation.messages) {
            JSONObject jsonMsg = new JSONObject();
            jsonMsg.put("role", "user".equals(msg.role) ? "user" : "assistant");
            jsonMsg.put("content", msg.content); messages.put(jsonMsg);
        }
        body.put("messages", messages); body.put("stream", false);
        return body;
    }

    private String callApiRaw(JSONObject requestBody) throws IOException {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        Request request = new Request.Builder()
                .url(API_URL).addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), JSON)).build();
        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();
            if (!response.isSuccessful()) throw new IOException("API错误 " + response.code() + ": " + body);
            return body;
        }
    }

    private LinearLayout createLoadingView() {
        LinearLayout container = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 6, 0, 6); container.setLayoutParams(params);
        container.setPadding(0, 0, 60, 0);
        TextView label = new TextView(this);
        label.setText("🤖 Gamma"); label.setTextSize(11); label.setTextColor(0xFF888888);
        label.setPadding(8, 0, 0, 2); container.addView(label);
        TextView loadingView = new TextView(this);
        loadingView.setText("⏳ 思考中..."); loadingView.setTextSize(14); loadingView.setTextColor(0xFF888888);
        loadingView.setPadding(16, 12, 16, 12); loadingView.setBackgroundResource(R.drawable.bubble_ai);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.START; loadingView.setLayoutParams(lp); container.addView(loadingView);
        final Animation blink = new AlphaAnimation(1.0f, 0.3f);
        blink.setDuration(800); blink.setRepeatMode(Animation.REVERSE);
        blink.setRepeatCount(Animation.INFINITE); loadingView.startAnimation(blink);
        chatLayout.addView(container);
        return container;
    }

    private void loadConversation(String convId, boolean closeDrawer) {
        try {
            Conversation conv = convManager.loadConversation(convId);
            if (conv == null) return;
            if (currentConversation != null && currentConversation.messages.size() > 0)
                convManager.saveConversation(currentConversation);
            currentConversation = conv; chatLayout.removeAllViews();
            isWaitingResponse = false; titleText.setText(conv.title);
            hasSentSystemPrompt = conv.messages.size() > 0;
            for (Conversation.Message msg : conv.messages) {
                boolean isUser = "user".equals(msg.role);
                addMessageBubble(isUser ? "🧑 我" : "🤖 Gamma", msg.content, isUser, false);
            }
            if (closeDrawer) drawerLayout.closeDrawer(drawerContent);
        } catch (Exception ignored) {}
    }

    private void loadConversation(String convId) { loadConversation(convId, true); }

    private void refreshConversationList() {
        try {
            conversationList.removeAllViews();
            List<Conversation> all = convManager.getAllConversations();
            conversationCount.setText(all.size() + " 个对话");
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
            for (Conversation conv : all) {
                boolean isActive = conv.id.equals(currentConversation.id);
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL); card.setPadding(12, 12, 12, 12);
                card.setBackgroundResource(R.drawable.card_bg);
                if (isActive) card.setBackgroundColor(0xFFEDE7F6);
                LinearLayout.LayoutParams cardP = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                cardP.setMargins(4, 4, 4, 4); card.setLayoutParams(cardP);
                card.setElevation(isActive ? 3 : 1);
                TextView title = new TextView(this);
                title.setText(conv.title); title.setTextSize(13); title.setTextColor(0xFF1C1B1F);
                title.setMaxLines(1); card.addView(title);
                TextView info = new TextView(this);
                info.setText(sdf.format(new Date(conv.timestamp)) + "  ·  " + conv.messages.size() + "条消息");
                info.setTextSize(11); info.setTextColor(0xFF888888); info.setPadding(0, 4, 0, 0); card.addView(info);
                final String convId = conv.id;
                card.setOnClickListener(v -> loadConversation(convId));
                card.setOnLongClickListener(v -> {
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("删除对话").setMessage("确定要删除？")
                            .setPositiveButton("删除", (d, w) -> {
                                if (conv.id.equals(currentConversation.id)) createNewConversation(false);
                                convManager.deleteConversation(conv.id); refreshConversationList();
                            }).setNegativeButton("取消", null).show();
                    return true;
                });
                conversationList.addView(card);
            }
        } catch (Exception ignored) {}
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(inputMessage.getWindowToken(), 0);
    }

    private static class Segment {
        String content; boolean isLatex;
        Segment(String content, boolean isLatex) { this.content = content; this.isLatex = isLatex; }
    }
}
