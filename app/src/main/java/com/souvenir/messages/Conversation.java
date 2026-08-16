package com.souvenir.messages;
import com.google.gson.annotations.SerializedName;
import androidx.recyclerview.widget.DiffUtil;
public class Conversation {
    @SerializedName("nom") public String nom = "";
    @SerializedName("numero") public String numero = "";
    @SerializedName("dernierMessage") public String dernierMessage = "";
    @SerializedName("horodatage") public long horodatage = System.currentTimeMillis();
    public static final DiffUtil.ItemCallback<Conversation> DIFF_CALLBACK = new DiffUtil.ItemCallback<Conversation>() {
        @Override public boolean areItemsTheSame(Conversation a, Conversation b) { return a.numero.equals(b.numero); }
        @Override public boolean areContentsTheSame(Conversation a, Conversation b) { return a.dernierMessage.equals(b.dernierMessage); }
    };
}
