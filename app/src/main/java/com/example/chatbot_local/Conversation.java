package com.example.chatbot_local;

public class Conversation {
    public final long id;
    public String title;
    public final long updatedAt;

    public Conversation(long id, String title, long updatedAt) {
        this.id = id;
        this.title = title;
        this.updatedAt = updatedAt;
    }
}
