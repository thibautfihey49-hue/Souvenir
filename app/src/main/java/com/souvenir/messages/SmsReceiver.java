package com.souvenir.messages;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SMS_RECEIVER";
    private static final String CANAL_ID = "Messages";
    private static final String PREFS_MESSAGES = "MessagesStockes";
    private static final String PREFS_CONVERSATIONS = "Conversations";
    private static final String PREFS_CONTACTS = "ContactsConfig";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) return;
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;
        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null) return;

        for (Object pdu : pdus) {
            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
            String numero = normaliserNumero(sms.getOriginatingAddress());
            String contenu = sms.getMessageBody();
            long horodatage = sms.getTimestampMillis();
            Log.d(TAG, "SMS reçu de " + numero + " : " + contenu);

            sauvegarderMessage(context, numero, contenu, horodatage);
            mettreAJourConversation(context, numero, contenu, horodatage);
            ContactConfig config = getContactConfig(context, numero);

            if (config != null && config.intercepterSms) {
                abortBroadcast();
                Log.d(TAG, "SMS intercepté pour " + numero);
            }
            if (config == null || !config.estCache) {
                creerNotification(context, numero, contenu, config);
            }
        }
    }

    private String normaliserNumero(String num) {
        if (num == null) return "";
        return num.replaceAll("\\s+", "").replaceAll("^\\+33", "0");
    }

    private void sauvegarderMessage(Context context, String numero, String contenu, long horodatage) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_MESSAGES, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String cle = "conv_" + numero;
        List<Message> liste = new ArrayList<>();
        String ancien = prefs.getString(cle, "[]");
        try {
            Type type = new TypeToken<List<Message>>(){}.getType();
            liste = gson.fromJson(ancien, type);
        } catch (Exception e) {}
        liste.add(new Message(numero, contenu, false));
        prefs.edit().putString(cle, gson.toJson(liste)).apply();
    }

    private void mettreAJourConversation(Context context, String numero, String contenu, long horodatage) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_CONVERSATIONS, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        List<Conversation> liste = new ArrayList<>();
        String ancien = prefs.getString("liste", "[]");
        try {
            Type type = new TypeToken<List<Conversation>>(){}.getType();
            liste = gson.fromJson(ancien, type);
        } catch (Exception e) {}

        boolean trouve = false;
        for (Conversation c : liste) {
            if (c.numero.equals(numero)) {
                c.dernierMessage = contenu;
                c.horodatage = horodatage;
                c.nonLu++;
                trouve = true;
                break;
            }
        }
        if (!trouve) {
            Conversation conv = new Conversation(numero, numero);
            conv.dernierMessage = contenu;
            conv.horodatage = horodatage;
            conv.nonLu = 1;
            liste.add(conv);
        }
        prefs.edit().putString("liste", gson.toJson(liste)).apply();
    }

    private ContactConfig getContactConfig(Context context, String numero) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_CONTACTS, Context.MODE_PRIVATE);
        String json = prefs.getString("liste", "[]");
        Gson gson = new Gson();
        try {
            Type type = new TypeToken<List<ContactConfig>>(){}.getType();
            List<ContactConfig> liste = gson.fromJson(json, type);
            if (liste != null) for (ContactConfig c : liste)
                if (c.numero.equals(numero)) return c;
        } catch (Exception e) {}
        return null;
    }

    private void creerNotification(Context context, String numero, String contenu, ContactConfig config) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(CANAL_ID, "Messages", NotificationManager.IMPORTANCE_HIGH);
            canal.setShowBadge(true);
            manager.createNotificationChannel(canal);
        }
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, numero.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String titre = (config != null && config.titreNotif != null && !config.titreNotif.isEmpty()) ? config.titreNotif : numero;
        String texte = (config != null && config.texteNotif != null && !config.texteNotif.isEmpty()) ? config.texteNotif : contenu;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(titre)
            .setContentText(texte)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        manager.notify(numero.hashCode(), builder.build());
    }
}
