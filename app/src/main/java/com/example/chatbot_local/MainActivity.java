package com.example.chatbot_local;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ConversationAdapter.Callback {
    private ChatDatabase database;
    private OllamaClient client;
    private SharedPreferences prefs;
    private DrawerLayout drawer;
    private MaterialToolbar toolbar;
    private RecyclerView messagesView;
    private View emptyView;
    private TextView noChatsView;
    private EditText input;
    private ImageButton sendButton;
    private MessageAdapter messageAdapter;
    private ConversationAdapter conversationAdapter;

    private long conversationId;
    private boolean generating;
    private boolean stickToBottom = true;
    private boolean tokenQueued;
    private int requestSerial;
    private long pendingAssistantId;
    private String pendingContent = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int bar = getColor(R.color.chat_bar);
        if (isNight()) {
            EdgeToEdge.enable(this, SystemBarStyle.dark(bar), SystemBarStyle.dark(bar));
        } else {
            EdgeToEdge.enable(this, SystemBarStyle.light(bar, bar), SystemBarStyle.light(bar, bar));
        }
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }

        database = ChatDatabase.get(this);
        client = new OllamaClient();
        prefs = getSharedPreferences(ChatApp.PREFS, MODE_PRIVATE);

        drawer = findViewById(R.id.drawer);
        toolbar = findViewById(R.id.toolbar);
        messagesView = findViewById(R.id.messages);
        emptyView = findViewById(R.id.empty);
        noChatsView = findViewById(R.id.no_chats);
        input = findViewById(R.id.input);
        sendButton = findViewById(R.id.send);

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawer,
                toolbar,
                R.string.open_menu,
                R.string.close_menu);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        toolbar.setNavigationIconTint(getColor(R.color.assistant_text));

        messageAdapter = new MessageAdapter();
        LinearLayoutManager messageLayout = new LinearLayoutManager(this);
        messageLayout.setStackFromEnd(true);
        messagesView.setLayoutManager(messageLayout);
        messagesView.setItemAnimator(null);
        messagesView.setAdapter(messageAdapter);
        messagesView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING
                        || newState == RecyclerView.SCROLL_STATE_IDLE) {
                    stickToBottom = !recyclerView.canScrollVertically(1);
                }
            }
        });

        conversationAdapter = new ConversationAdapter(this);
        RecyclerView conversationsView = findViewById(R.id.conversations);
        conversationsView.setLayoutManager(new LinearLayoutManager(this));
        conversationsView.setAdapter(conversationAdapter);

        findViewById(R.id.new_chat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startNewChat();
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (generating) stopGeneration();
                else sendMessage();
            }
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshSendButton();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        if (savedInstanceState != null) {
            conversationId = savedInstanceState.getLong("conversationId");
        }
        if (conversationId != 0) {
            messageAdapter.setMessages(database.messages(conversationId));
        }
        refreshEmpty();
        refreshSendButton();
        refreshHeader();
        loadConversations();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("conversationId", conversationId);
    }

    @Override
    protected void onDestroy() {
        requestSerial++;
        client.cancel();
        if (pendingAssistantId != 0) {
            String content = pendingContent.trim().length() == 0 ? "Generation stopped." : pendingContent;
            database.updateMessage(pendingAssistantId, content);
            pendingAssistantId = 0;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem theme = menu.findItem(R.id.action_theme);
        if (theme != null) {
            boolean night = isNight();
            theme.setTitle(night ? R.string.switch_to_light : R.string.switch_to_dark);
            theme.setIcon(night ? R.drawable.ic_theme : R.drawable.ic_moon);
        }
        MenuItem delete = menu.findItem(R.id.action_delete);
        if (delete != null) {
            delete.setVisible(conversationId != 0);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_theme) {
            prefs.edit().putString("theme", isNight() ? "light" : "dark").apply();
            ChatApp.applySavedTheme(this);
            return true;
        }
        if (item.getItemId() == R.id.action_delete) {
            deleteOpenChat();
            return true;
        }
        if (item.getItemId() == R.id.action_settings) {
            showSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onOpen(Conversation conversation) {
        if (generating) stopGeneration();
        conversationId = conversation.id;
        messageAdapter.setMessages(database.messages(conversation.id));
        refreshEmpty();
        refreshHeader();
        loadConversations();
        drawer.closeDrawer(GravityCompat.START);
        if (messageAdapter.getItemCount() > 0) {
            messagesView.scrollToPosition(messageAdapter.getItemCount() - 1);
        }
    }

    @Override
    public void onDelete(Conversation conversation) {
        deleteConversation(conversation);
    }

    @Override
    public void onLongPress(Conversation conversation, View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, R.string.rename);
        menu.getMenu().add(0, 2, 1, R.string.delete);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == 1) renameConversation(conversation);
                else deleteConversation(conversation);
                return true;
            }
        });
        menu.show();
    }

    private void startNewChat() {
        if (generating) stopGeneration();
        conversationId = 0;
        messageAdapter.setMessages(new ArrayList<Message>());
        refreshEmpty();
        refreshHeader();
        loadConversations();
        drawer.closeDrawer(GravityCompat.START);
    }

    private void sendMessage() {
        String text = input.getText().toString().trim();
        if (text.length() == 0 || generating) return;

        if (conversationId == 0) {
            conversationId = database.createConversation(titleFrom(text));
        }
        Message user = database.insertMessage(conversationId, "user", text);
        Message assistant = database.insertMessage(conversationId, "assistant", "");
        messageAdapter.add(user);
        messageAdapter.add(assistant);
        input.setText("");
        refreshEmpty();
        refreshHeader();
        loadConversations();

        pendingAssistantId = assistant.id;
        pendingContent = "";
        tokenQueued = false;
        final int request = ++requestSerial;
        final long assistantId = assistant.id;
        generating = true;
        stickToBottom = true;
        refreshSendButton();
        pinToBottom();

        List<Message> history = database.messages(conversationId);
        if (!history.isEmpty()) {
            Message last = history.get(history.size() - 1);
            if ("assistant".equals(last.role) && last.content.trim().length() == 0) {
                history.remove(history.size() - 1);
            }
        }

        client.stream(serverUrl(), modelName(), history, new OllamaClient.Listener() {
            @Override
            public void onToken(final String snapshot) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (request != requestSerial) return;
                        pendingContent = snapshot;
                        if (tokenQueued) return;
                        tokenQueued = true;
                        messagesView.post(new Runnable() {
                            @Override
                            public void run() {
                                tokenQueued = false;
                                if (request != requestSerial) return;
                                messageAdapter.setLastContent(pendingContent);
                                pinToBottom();
                            }
                        });
                    }
                });
            }

            @Override
            public void onComplete() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (request != requestSerial) return;
                        String content = pendingContent.trim().length() == 0 ? "Generation stopped." : pendingContent;
                        finishGeneration(assistantId, content);
                    }
                });
            }

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (request != requestSerial) return;
                        String detail = displayError(message);
                        String content = pendingContent.length() == 0 ? detail : pendingContent + "\n\n" + detail;
                        finishGeneration(assistantId, content);
                    }
                });
            }
        });
    }

    private void stopGeneration() {
        requestSerial++;
        client.cancel();
        if (pendingAssistantId != 0) {
            String content = pendingContent.trim().length() == 0 ? "Generation stopped." : pendingContent;
            database.updateMessage(pendingAssistantId, content);
            messageAdapter.setLastContent(content);
            pendingAssistantId = 0;
        }
        generating = false;
        refreshSendButton();
    }

    private void finishGeneration(long assistantId, String content) {
        database.updateMessage(assistantId, content);
        messageAdapter.setLastContent(content);
        pendingAssistantId = 0;
        pendingContent = content;
        generating = false;
        refreshSendButton();
        loadConversations();
    }

    private void deleteOpenChat() {
        if (conversationId == 0) return;
        String title = database.conversationTitle(conversationId);
        if (title == null || title.length() == 0) title = getString(R.string.app_name);
        deleteConversation(new Conversation(conversationId, title, 0));
    }

    private void renameConversation(Conversation conversation) {
        final EditText field = new EditText(this);
        field.setText(conversation.title);
        field.setSelection(field.getText().length());
        field.setTextColor(getColor(R.color.assistant_text));
        field.setHintTextColor(getColor(R.color.muted_text));
        field.setBackgroundResource(R.drawable.bg_input);
        int pad = (int) (14 * getResources().getDisplayMetrics().density);
        field.setPadding(pad, pad, pad, pad);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(pad, pad / 2, pad, 0);
        wrap.addView(field, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(this)
                .setTitle(R.string.rename)
                .setView(wrap)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String next = field.getText().toString().trim();
                    if (next.length() == 0) return;
                    database.renameConversation(conversation.id, next);
                    refreshHeader();
                    loadConversations();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteConversation(Conversation conversation) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.delete_chat)
                .setMessage(getString(R.string.delete_chat_message, conversation.title))
                .setPositiveButton(R.string.delete, (shown, which) -> {
                    if (conversation.id == conversationId && generating) stopGeneration();
                    database.deleteConversation(conversation.id);
                    if (conversation.id == conversationId) {
                        conversationId = 0;
                        messageAdapter.setMessages(new ArrayList<Message>());
                        refreshEmpty();
                        refreshHeader();
                    }
                    loadConversations();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(getColor(R.color.danger)));
        dialog.show();
    }

    private void showSettings() {
        View form = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        EditText modelField = form.findViewById(R.id.model_name);
        EditText serverField = form.findViewById(R.id.server_url);
        modelField.setText(modelName());
        serverField.setText(serverUrl());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.settings)
                .setView(form)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(shown -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String model = modelField.getText().toString().trim();
                String server = serverField.getText().toString().trim();
                if (!isValidServer(server)) {
                    Toast.makeText(this, R.string.invalid_server, Toast.LENGTH_LONG).show();
                    return;
                }
                if (model.length() == 0) model = ChatApp.DEFAULT_MODEL;
                prefs.edit().putString("model", model).putString("server", server).apply();
                refreshHeader();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void loadConversations() {
        List<Conversation> items = database.conversations();
        conversationAdapter.submit(items, conversationId);
        noChatsView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void refreshHeader() {
        String title = conversationId == 0
                ? getString(R.string.app_name)
                : database.conversationTitle(conversationId);
        if (title == null || title.length() == 0) title = getString(R.string.app_name);
        toolbar.setTitle(title);
        toolbar.setSubtitle(null);
        invalidateOptionsMenu();
    }

    private void pinToBottom() {
        if (!stickToBottom) return;
        final int last = messageAdapter.getItemCount() - 1;
        if (last < 0) return;
        messagesView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                        int oldLeft, int oldTop, int oldRight, int oldBottom) {
                messagesView.removeOnLayoutChangeListener(this);
                messagesView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!stickToBottom) return;
                        LinearLayoutManager layout = (LinearLayoutManager) messagesView.getLayoutManager();
                        if (layout == null) return;
                        View child = layout.findViewByPosition(last);
                        if (child == null) {
                            messagesView.scrollToPosition(last);
                            return;
                        }
                        int overflow = child.getBottom() - (messagesView.getHeight() - messagesView.getPaddingBottom());
                        if (overflow > 0) messagesView.scrollBy(0, overflow);
                    }
                });
            }
        });
    }

    private void refreshEmpty() {
        emptyView.setVisibility(messageAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void refreshSendButton() {
        if (generating) {
            sendButton.setImageResource(R.drawable.ic_stop);
            sendButton.setEnabled(true);
            sendButton.setAlpha(1f);
            sendButton.setContentDescription(getString(R.string.stop));
            return;
        }
        sendButton.setImageResource(R.drawable.ic_send);
        boolean ready = input.getText().toString().trim().length() > 0;
        sendButton.setEnabled(ready);
        sendButton.setAlpha(ready ? 1f : 0.4f);
        sendButton.setContentDescription(getString(R.string.send));
    }

    private String displayError(String raw) {
        String lower = raw.toLowerCase();
        if (lower.contains("failed to connect")
                || lower.contains("unable to resolve")
                || lower.contains("connection refused")
                || lower.contains("network")
                || lower.contains("timed out")
                || lower.contains("cleartext")
                || lower.contains("econnrefused")) {
            return getString(R.string.model_missing, modelName(), serverUrl());
        }
        return raw;
    }

    private boolean isNight() {
        int night = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return night == Configuration.UI_MODE_NIGHT_YES;
    }

    private boolean isValidServer(String server) {
        if (!server.startsWith("http://") && !server.startsWith("https://")) return false;
        try {
            java.net.URI uri = new java.net.URI(server);
            return uri.getHost() != null && uri.getPort() > 0;
        } catch (Exception error) {
            return false;
        }
    }

    private String modelName() {
        String model = prefs.getString("model", ChatApp.DEFAULT_MODEL);
        return model == null || model.trim().length() == 0 ? ChatApp.DEFAULT_MODEL : model;
    }

    private String serverUrl() {
        String server = prefs.getString("server", ChatApp.DEFAULT_SERVER);
        return server == null || server.trim().length() == 0 ? ChatApp.DEFAULT_SERVER : server.trim();
    }

    private static String titleFrom(String text) {
        String clean = text.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        if (clean.length() <= 42) return clean;
        return clean.substring(0, 41).trim() + "…";
    }
}
