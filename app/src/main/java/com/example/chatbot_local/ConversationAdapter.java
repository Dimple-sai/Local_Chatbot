package com.example.chatbot_local;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.Holder> {
    public interface Callback {
        void onOpen(Conversation conversation);

        void onLongPress(Conversation conversation, View anchor);

        void onDelete(Conversation conversation);
    }

    private final List<Conversation> items = new ArrayList<Conversation>();
    private final Callback callback;
    private long selectedId;

    public ConversationAdapter(Callback callback) {
        this.callback = callback;
    }

    public void submit(List<Conversation> next, long selectedId) {
        items.clear();
        items.addAll(next);
        this.selectedId = selectedId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Conversation conversation = items.get(position);
        holder.title.setText(conversation.title);
        holder.itemView.setActivated(conversation.id == selectedId);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.onOpen(conversation);
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                callback.onLongPress(conversation, view);
                return true;
            }
        });
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.onDelete(conversation);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final ImageButton delete;

        Holder(View view) {
            super(view);
            title = view.findViewById(R.id.conversation_title);
            delete = view.findViewById(R.id.conversation_delete);
        }
    }
}
