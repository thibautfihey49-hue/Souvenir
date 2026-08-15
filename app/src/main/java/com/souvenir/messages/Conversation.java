package com.souvenir.messages;
public class Conversation {
    public String numero;
    public String nom;
    public String dernierMessage;
    public long horodatage;
    public int nonLu;
    public boolean estCache;
    public Conversation(String numero, String nom) {
        this.numero = numero;
        this.nom = nom;
        this.dernierMessage = "";
        this.horodatage = System.currentTimeMillis();
        this.nonLu = 0;
        this.estCache = false;
    }
}
