package com.example.helloworld;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 对话持久化管理器 - 使用 JSON 文件存储对话
 */
public class ConversationManager {

    private static final String INDEX_FILE = "conversations_index.json";
    private static final String CONV_DIR = "conversations";
    private final Context context;
    private final Gson gson;
    private List<Conversation> conversationList;

    public ConversationManager(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.conversationList = new ArrayList<>();
        loadIndex();
    }

    // ===== 索引管理 =====

    private File getIndexFile() {
        return new File(context.getFilesDir(), INDEX_FILE);
    }

    private File getConvDir() {
        File dir = new File(context.getFilesDir(), CONV_DIR);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private File getConvFile(String convId) {
        return new File(getConvDir(), convId + ".json");
    }

    private void loadIndex() {
        File file = getIndexFile();
        if (file.exists()) {
            try {
                FileReader reader = new FileReader(file);
                Type type = new TypeToken<List<Conversation>>(){}.getType();
                conversationList = gson.fromJson(reader, type);
                reader.close();
                if (conversationList == null) conversationList = new ArrayList<>();
            } catch (Exception e) {
                conversationList = new ArrayList<>();
            }
        }
    }

    private void saveIndex() {
        try {
            FileWriter writer = new FileWriter(getIndexFile());
            gson.toJson(conversationList, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== 对话操作 =====

    /** 获取所有对话摘要列表 */
    public List<Conversation> getAllConversations() {
        return conversationList;
    }

    /** 创建新对话 */
    public Conversation createNewConversation() {
        Conversation conv = new Conversation();
        conversationList.add(0, conv);
        saveIndex();
        saveConversation(conv);
        return conv;
    }

    /** 保存单个对话的完整内容 */
    public void saveConversation(Conversation conv) {
        try {
            FileWriter writer = new FileWriter(getConvFile(conv.id));
            gson.toJson(conv, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 加载单个对话的完整消息 */
    public Conversation loadConversation(String convId) {
        File file = getConvFile(convId);
        if (file.exists()) {
            try {
                FileReader reader = new FileReader(file);
                Conversation conv = gson.fromJson(reader, Conversation.class);
                reader.close();
                return conv;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /** 删除对话 */
    public void deleteConversation(String convId) {
        getConvFile(convId).delete();
        Iterator<Conversation> it = conversationList.iterator();
        while (it.hasNext()) {
            if (it.next().id.equals(convId)) {
                it.remove();
                break;
            }
        }
        saveIndex();
    }

    /** 更新对话标题（取第一条用户消息的前20字） */
    public void updateTitle(Conversation conv) {
        for (Conversation.Message msg : conv.messages) {
            if ("user".equals(msg.role)) {
                String text = msg.content;
                if (text.length() > 20) text = text.substring(0, 20) + "...";
                conv.title = text;
                // 更新索引中的标题
                for (Conversation c : conversationList) {
                    if (c.id.equals(conv.id)) {
                        c.title = conv.title;
                        c.timestamp = conv.timestamp;
                        break;
                    }
                }
                saveIndex();
                saveConversation(conv);
                return;
            }
        }
    }
}
