package com.example.chatbot_local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ChatDatabase extends SQLiteOpenHelper {
    private static ChatDatabase instance;

    public static synchronized ChatDatabase get(Context context) {
        if (instance == null) {
            instance = new ChatDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private ChatDatabase(Context context) {
        super(context, "chat.db", null, 1);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE conversations ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "title TEXT NOT NULL,"
                + "created_at INTEGER NOT NULL,"
                + "updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE messages ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "conversation_id INTEGER NOT NULL,"
                + "role TEXT NOT NULL,"
                + "content TEXT NOT NULL,"
                + "created_at INTEGER NOT NULL,"
                + "FOREIGN KEY(conversation_id) REFERENCES conversations(id) ON DELETE CASCADE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS messages");
        db.execSQL("DROP TABLE IF EXISTS conversations");
        onCreate(db);
    }

    public synchronized long createConversation(String title) {
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("created_at", now);
        values.put("updated_at", now);
        return getWritableDatabase().insert("conversations", null, values);
    }

    public synchronized void renameConversation(long id, String title) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        getWritableDatabase().update("conversations", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public synchronized void touchConversation(long id) {
        ContentValues values = new ContentValues();
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("conversations", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public synchronized void deleteConversation(long id) {
        getWritableDatabase().delete("conversations", "id = ?", new String[]{String.valueOf(id)});
    }

    public synchronized String conversationTitle(long id) {
        Cursor cursor = getReadableDatabase().query(
                "conversations",
                new String[]{"title"},
                "id = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null);
        try {
            if (!cursor.moveToFirst()) return "";
            return cursor.getString(0);
        } finally {
            cursor.close();
        }
    }

    public synchronized List<Conversation> conversations() {
        List<Conversation> items = new ArrayList<Conversation>();
        Cursor cursor = getReadableDatabase().query(
                "conversations",
                new String[]{"id", "title", "updated_at"},
                null,
                null,
                null,
                null,
                "updated_at DESC");
        try {
            while (cursor.moveToNext()) {
                items.add(new Conversation(cursor.getLong(0), cursor.getString(1), cursor.getLong(2)));
            }
        } finally {
            cursor.close();
        }
        return items;
    }

    public synchronized Message insertMessage(long conversationId, String role, String content) {
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("conversation_id", conversationId);
        values.put("role", role);
        values.put("content", content);
        values.put("created_at", now);
        long id = getWritableDatabase().insert("messages", null, values);
        touchConversation(conversationId);
        return new Message(id, role, content);
    }

    public synchronized void updateMessage(long id, String content) {
        ContentValues values = new ContentValues();
        values.put("content", content);
        getWritableDatabase().update("messages", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public synchronized List<Message> messages(long conversationId) {
        List<Message> items = new ArrayList<Message>();
        Cursor cursor = getReadableDatabase().query(
                "messages",
                new String[]{"id", "role", "content"},
                "conversation_id = ?",
                new String[]{String.valueOf(conversationId)},
                null,
                null,
                "id ASC");
        try {
            while (cursor.moveToNext()) {
                items.add(new Message(cursor.getLong(0), cursor.getString(1), cursor.getString(2)));
            }
        } finally {
            cursor.close();
        }
        return items;
    }
}
