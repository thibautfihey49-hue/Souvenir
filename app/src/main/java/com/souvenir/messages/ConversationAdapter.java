package com.souvenir.messages;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class ConversationAdapter extends ListAdapter<Conversation, ConversationAdapter.VH> {
    public ConversationAdapter() {
        super(Conversation.DIFF_CALLBACK);
    }

    public ConversationAdapter(java.util.List<Conversation> liste) {
        super(Conversation.DIFF_CALLBACK);
        submitList(liste);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Conversation c = getItem(position);
        holder.nom.setText(c.nom);
        holder.message.setText(c.dernierMessage);
        holder.nonLu.setVisibility(c.nonLu > 0 ? View.VISIBLE : View.GONE);
        holder.nonLu.setText(String.valueOf(c.nonLu));
        holder.itemView.setOnClickListener(v -> {
            Intent i = new Intent(v.getContext(), ConversationActivity.class);
            i.putExtra("numero", c.numero);
            i.putExtra("nom", c.nom);
            v.getContext().startActivity(i);
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView nom, message, nonLu;
        VH(View v) {
            super(v);
            nom = v.findViewById(R.id.tv_nom_conv);
            message = v.findViewById(R.id.tv_dernier_msg);
            nonLu = v.findViewById(R.id.tv_non_lu);
        }
    }
}
