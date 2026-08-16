package com.souvenir.messages;
import com.google.gson.annotations.SerializedName;
public class Message {
    @SerializedName("texte") public String texte = "";
    @SerializedName("horodatage") public long horodatage = 0;
    @SerializedName("envoye") public boolean envoye = false;
    @SerializedName("photoBase64") public String photoBase64 = null;
    @SerializedName("audioBase64") public String audioBase64 = null;
    @SerializedName("dureeMs") public long dureeMs = 0;
}
