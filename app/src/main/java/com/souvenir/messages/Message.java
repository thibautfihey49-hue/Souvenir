package com.souvenir.messages;

import com.google.gson.annotations.SerializedName;

public class Message {
    @SerializedName("texte")
    public String texte = "";
    
    @SerializedName("horodatage")
    public long horodatage = 0;
    
    @SerializedName("envoye")
    public boolean envoye = false;
    
    @SerializedName("photoBase64")
    public String photoBase64 = null;

    // ✅ Constructeur SANS paramètres requis pour Gson
    public Message() {}

    // Constructeur complet si besoin
    public Message(String texte, String photoBase64, boolean envoye) {
        this.texte = texte;
        this.photoBase64 = photoBase64;
        this.envoye = envoye;
        this.horodatage = System.currentTimeMillis();
    }
}
