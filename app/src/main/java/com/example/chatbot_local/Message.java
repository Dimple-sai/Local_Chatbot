package com.example.chatbot_local;

public class Message {
    public final long id;
    public final String role;
    public String content;

    public Message(long id, String role, String content) {
        this.id = id;
        this.role = role;
        this.content = content;
    }
}
