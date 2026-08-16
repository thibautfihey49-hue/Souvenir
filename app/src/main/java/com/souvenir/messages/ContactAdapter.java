package com.souvenir.messages;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class ContactAdapter extends ListAdapter<ContactConfig, ContactAdapter.VH> {
    public ContactAdapter() {
        super(ContactConfig.DIFF_CALLBACK);
    }

    public ContactAdapter(java.util.List<ContactConfig> liste) {
        super(ContactConfig.DIFF_CALLBACK);
        submitList(liste);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ContactConfig c = getItem(position);
        holder.nom.setText(c.nom);
        holder.numero.setText(c.numero);
        holder.itemView.setOnClickListener(v -> {
            Intent i = new Intent(v.getContext(), ConversationActivity.class);
            i.putExtra("numero", c.numero);
            i.putExtra("nom", c.nom);
            v.getContext().startActivity(i);
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView nom, numero;
        VH(View v) {
            super(v);
            nom = v.findViewById(R.id.tv_nom);
            numero = v.findViewById(R.id.tv_numero);
        }
    }
}
