package com.souvenir.messages;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import java.io.Serializable;

public class Conversation implements Serializable {
    public String numero;
    public String nom;
    public String dernierMessage;
    public long horodatage;
    public int nonLu = 0;

    public Conversation() {}

    public Conversation(String numero, String nom) {
        this.numero = numero;
        this.nom = nom;
        this.dernierMessage = "";
        this.horodatage = System.currentTimeMillis();
    }

    public static final DiffUtil.ItemCallback<Conversation> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Conversation>() {
                @Override
                public boolean areItemsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                    return oldItem.numero.equals(newItem.numero);
                }
                @Override
                public boolean areContentsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                    return oldItem.nom.equals(newItem.nom) &&
                           oldItem.dernierMessage.equals(newItem.dernierMessage) &&
                           oldItem.horodatage == newItem.horodatage &&
                           oldItem.nonLu == newItem.nonLu;
                }
            };
}
