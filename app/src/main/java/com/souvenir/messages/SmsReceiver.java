package com.souvenir.messages;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.telephony.SmsMessage;
import androidx.core.app.NotificationCompat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SmsReceiver extends BroadcastReceiver {
    private static final String PREFS_CONTACTS = "ContactsConfig";
    private static final String PREFS_CONVERSATIONS = "Conversations";
    private static final String CANAL_ID = "SMS_RECU";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) return;

        Object[] messages = (Object[]) intent.getExtras().get("pdus");
        if (messages == null) return;

        for (Object pdu : messages) {
            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
            String numero = sms.getOriginatingAddress();
            String contenu = sms.getMessageBody();

            if (numero.startsWith("+33")) numero = "0" + numero.substring(3);
            numero = numero.replaceAll("\\s+", "");

            ContactConfig config = trouverContact(context, numero);

            if (config != null && config.intercepterSms) {
                enregistrerMessage(context, numero, contenu);
                if (!config.estCache) {
                    creerNotification(context, numero, contenu, config);
                }
            } else {
                enregistrerMessage(context, numero, contenu);
                creerNotification(context, numero, contenu, null);
            }
        }
    }

    private ContactConfig trouverContact(Context ctx, String numero) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_CONTACTS, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        try {
            Type type = new TypeToken<List<ContactConfig>>(){}.getType();
            List<ContactConfig> liste = gson.fromJson(prefs.getString("liste", "[]"), type);
            if (liste != null) {
                for (ContactConfig c : liste) {
                    if (c.numero.equals(numero)) return c;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void enregistrerMessage(Context ctx, String numero, String contenu) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_CONVERSATIONS, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        
        List<Message> listeMsg = new ArrayList<>();
        try {
            Type typeMsg = new TypeToken<List<Message>>(){}.getType();
            listeMsg = gson.fromJson(prefs.getString(numero, "[]"), typeMsg);
        } catch (Exception ignored) {}
        if (listeMsg == null) listeMsg = new ArrayList<>();
        
        Message msg = new Message(contenu, null, false);
        listeMsg.add(msg);
        prefs.edit().putString(numero, gson.toJson(listeMsg)).apply();

        List<Conversation> listeConv = new ArrayList<>();
        try {
            Type typeConv = new TypeToken<List<Conversation>>(){}.getType();
            listeConv = gson.fromJson(prefs.getString("liste", "[]"), typeConv);
        } catch (Exception ignored) {}
        if (listeConv == null) listeConv = new ArrayList<>();

        boolean existe = false;
        for (Conversation c : listeConv) {
            if (c.numero.equals(numero)) {
                c.dernierMessage = contenu;
                c.horodatage = System.currentTimeMillis();
                c.nonLu++;
                existe = true;
                break;
            }
        }
        if (!existe) {
            Conversation conv = new Conversation(numero, numero);
            conv.dernierMessage = contenu;
            conv.nonLu = 1;
            listeConv.add(conv);
        }
        prefs.edit().putString("liste", gson.toJson(listeConv)).apply();
    }

    private void creerNotification(Context ctx, String numero, String contenu, ContactConfig config) {
        creerCanal(ctx);

        String titre = (config != null && config.titreNotif != null && !config.titreNotif.isEmpty()) ? config.titreNotif : numero;
        String texte = (config != null && config.texteNotif != null && !config.texteNotif.isEmpty()) ? config.texteNotif : contenu;

        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pending = PendingIntent.getActivity(ctx, numero.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CANAL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(titre)
                .setContentText(texte)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pending)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(numero.hashCode(), b.build());
    }

    private void creerCanal(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CANAL_ID, "SMS Reçus", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }
}
