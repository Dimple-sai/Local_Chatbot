package com.example.chatbot_local;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.Holder> {
    private static final int USER = 1;
    private static final int ASSISTANT = 2;

    private final List<Message> messages = new ArrayList<Message>();

    public MessageAdapter() {
        setHasStableIds(true);
    }

    public void setMessages(List<Message> items) {
        messages.clear();
        messages.addAll(items);
        notifyDataSetChanged();
    }

    public void add(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void setLastContent(String content) {
        if (messages.isEmpty()) return;
        int index = messages.size() - 1;
        messages.get(index).content = content;
        notifyItemChanged(index, content);
    }

    @Override
    public long getItemId(int position) {
        return messages.get(position).id;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            holder.text.setText(display(messages.get(position).content));
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public int getItemViewType(int position) {
        return "user".equals(messages.get(position).role) ? USER : ASSISTANT;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == USER ? R.layout.item_message_user : R.layout.item_message_assistant;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.text.setText(display(messages.get(position).content));
    }

    private static String display(String content) {
        return content == null || content.length() == 0 ? "…" : content;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView text;

        Holder(View view) {
            super(view);
            text = view.findViewById(R.id.message_text);
        }
    }
}
