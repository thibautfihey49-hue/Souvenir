package com.souvenir.messages;
import com.google.gson.annotations.SerializedName;
import androidx.recyclerview.widget.DiffUtil;
public class ContactConfig {
    @SerializedName("nom") public String nom = "";
    @SerializedName("numero") public String numero = "";
    public static final DiffUtil.ItemCallback<ContactConfig> DIFF_CALLBACK = new DiffUtil.ItemCallback<ContactConfig>() {
        @Override public boolean areItemsTheSame(ContactConfig a, ContactConfig b) { return a.numero.equals(b.numero); }
        @Override public boolean areContentsTheSame(ContactConfig a, ContactConfig b) { return a.nom.equals(b.nom); }
    };
}
