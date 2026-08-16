package com.souvenir.messages;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import java.io.Serializable;

public class ContactConfig implements Serializable {
    public String nom;
    public String numero;
    public boolean intercepterSms = false;
    public boolean estCache = false;
    public String titreNotif;
    public String texteNotif;

    public static final DiffUtil.ItemCallback<ContactConfig> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ContactConfig>() {
                @Override
                public boolean areItemsTheSame(@NonNull ContactConfig oldItem, @NonNull ContactConfig newItem) {
                    return oldItem.numero.equals(newItem.numero);
                }
                @Override
                public boolean areContentsTheSame(@NonNull ContactConfig oldItem, @NonNull ContactConfig newItem) {
                    return oldItem.nom.equals(newItem.nom) && 
                           oldItem.numero.equals(newItem.numero) &&
                           oldItem.intercepterSms == newItem.intercepterSms &&
                           oldItem.estCache == newItem.estCache &&
                           egal(oldItem.titreNotif, newItem.titreNotif) &&
                           egal(oldItem.texteNotif, newItem.texteNotif);
                }
                private boolean egal(String a, String b) {
                    return a == null ? b == null : a.equals(b);
                }
            };
}
