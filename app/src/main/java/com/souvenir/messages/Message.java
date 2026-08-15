package com.souvenir.messages;
public class Message {
    public String id;
    public String numero;
    public String contenu;
    public long horodatage;
    public boolean estEnvoye;
    public boolean lu;
    public Message(String numero, String contenu, boolean estEnvoye) {
        this.numero = numero;
        this.contenu = contenu;
        this.estEnvoye = estEnvoye;
        this.horodatage = System.currentTimeMillis();
        this.lu = false;
        this.id = String.valueOf(horodatage);
    }
}
