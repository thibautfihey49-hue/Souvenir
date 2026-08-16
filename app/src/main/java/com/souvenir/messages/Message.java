package com.souvenir.messages;

import java.io.Serializable;

public class Message implements Serializable {
    public String texte;
    public String audioBase64;
    public String photoBase64;
    public long dureeMs = 0;
    public boolean envoye;
    public long horodatage;

    public Message() {}

    public Message(String texte, String audioBase64, boolean envoye) {
        this.texte = texte;
        this.audioBase64 = audioBase64;
        this.envoye = envoye;
        this.horodatage = System.currentTimeMillis();
    }
}
