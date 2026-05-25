package com.example.helloworld;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 对话数据模型
 */
public class Conversation {
    public String id;
    public String title;
    public long timestamp;
    public List<Message> messages;

    public Conversation() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.messages = new ArrayList<>();
        this.title = "新对话";
    }

    public static class Message {
        public String role; // "user" 或 "ai"
        public String content;
        public long timestamp;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
