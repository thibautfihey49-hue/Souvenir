package com.souvenir.messages;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationActivity extends Activity {
    private static final String TAG = "CONV";
    private static final String PREFS_MESSAGES = "MessagesStockes";
    private static final String PREFS_CONVERSATIONS = "Conversations";
    private static final String PREFS_CONTACTS = "ContactsConfig";

    private ListView listView;
    private EditText saisieMessage;
    private Button btnEnvoyer;
    private MessageAdapter adapter;
    private List<Message> messages = new ArrayList<>();
    private String numero;
    private String nomConversation;
    private boolean modeCache = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        listView = findViewById(R.id.list_messages);
        saisieMessage = findViewById(R.id.saisie_message);
        btnEnvoyer = findViewById(R.id.btn_envoyer);

        numero = getIntent().getStringExtra("numero");
        nomConversation = getIntent().getStringExtra("nom");
        modeCache = getIntent().getBooleanExtra("mode_cache", false);

        if (TextUtils.isEmpty(numero)) numero = "";
        if (TextUtils.isEmpty(nomConversation)) nomConversation = "Nouvelle conversation";
        setTitle(nomConversation);

        adapter = new MessageAdapter();
        listView.setAdapter(adapter);

        if (!modeCache && !TextUtils.isEmpty(numero)) {
            chargerMessages();
        }

        btnEnvoyer.setOnClickListener(v -> {
            String texte = saisieMessage.getText().toString().trim();
            if (TextUtils.isEmpty(texte)) {
                Toast.makeText(this, "Message vide", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(numero)) {
            Toast.makeText(this, "Entrez un numéro puis envoyez un message", Toast.LENGTH_SHORT).show();
                Toast.makeText(this, "Ajoutez d'abord un contact", Toast.LENGTH_SHORT).show();
                return;
            }
            envoyerSMS(texte);
            saisieMessage.setText("");
        });
    }

    private void chargerMessages() {
        if (TextUtils.isEmpty(numero) || adapter == null) return;
        
        SharedPreferences prefs = getSharedPreferences(PREFS_MESSAGES, MODE_PRIVATE);
        Gson gson = new Gson();
        String cle = "conv_" + numero;
        String json = prefs.getString(cle, "[]");
        messages.clear();
        try {
            Type type = new TypeToken<List<Message>>(){}.getType();
            List<Message> liste = gson.fromJson(json, type);
            if (liste != null) messages.addAll(liste);
        } catch (Exception e) {
            Log.e(TAG, "Erreur chargement: " + e.getMessage());
        }
        marquerCommeLu();
        adapter.notifyDataSetChanged();
        if (messages.size() > 0) listView.setSelection(messages.size() - 1);
    }

    private void marquerCommeLu() {
        SharedPreferences prefs = getSharedPreferences(PREFS_CONVERSATIONS, MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("liste", "[]");
        List<Conversation> liste = new ArrayList<>();
        try {
            Type type = new TypeToken<List<Conversation>>(){}.getType();
            liste = gson.fromJson(json, type);
        } catch (Exception e) {}
        if (liste != null) {
            for (Conversation c : liste) {
                if (c.numero.equals(numero)) { c.nonLu = 0; break; }
            }
            prefs.edit().putString("liste", gson.toJson(liste)).apply();
        }
    }

    private void envoyerSMS(String texte) {
        try {
            SmsManager.getDefault().sendTextMessage(numero, null, texte, null, null);
            Message msg = new Message(numero, texte, true);
            messages.add(msg);
            sauvegarderMessages();
            adapter.notifyDataSetChanged();
            listView.setSelection(messages.size() - 1);
        } catch (Exception e) {
            Toast.makeText(this, "Erreur envoi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Erreur envoi: " + e.getMessage());
        }
    }

    private void sauvegarderMessages() {
        SharedPreferences prefs = getSharedPreferences(PREFS_MESSAGES, MODE_PRIVATE);
        Gson gson = new Gson();
        String cle = "conv_" + numero;
        prefs.edit().putString(cle, gson.toJson(messages)).apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_conversation, menu);
        if (TextUtils.isEmpty(numero)) return true;
        
        SharedPreferences prefs = getSharedPreferences(PREFS_CONTACTS, MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("liste", "[]");
        List<ContactConfig> contacts = new ArrayList<>();
        try {
            Type type = new TypeToken<List<ContactConfig>>(){}.getType();
            contacts = gson.fromJson(json, type);
        } catch (Exception e) {}
        if (contacts != null) {
            for (ContactConfig c : contacts) {
                if (c.numero.equals(numero)) {
                    menu.findItem(R.id.action_cacher).setChecked(c.estCache);
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_cacher) {
            boolean nouvelEtat = !item.isChecked();
            item.setChecked(nouvelEtat);
            basculerModeCache(nouvelEtat);
            return true;
        }
        if (id == R.id.action_ajouter_contact) {
            ajouterContact();
            return true;
        }
        if (id == R.id.action_supprimer_conversation) {
            supprimerConversation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void basculerModeCache(boolean estCache) {
        if (TextUtils.isEmpty(numero)) return;
        SharedPreferences prefs = getSharedPreferences(PREFS_CONTACTS, MODE_PRIVATE);
        Gson gson = new Gson();
        List<ContactConfig> liste = new ArrayList<>();
        String json = prefs.getString("liste", "[]");
        try {
            Type type = new TypeToken<List<ContactConfig>>(){}.getType();
            liste = gson.fromJson(json, type);
        } catch (Exception e) {}
        if (liste == null) liste = new ArrayList<>();

        boolean trouve = false;
        for (ContactConfig c : liste) {
            if (c.numero.equals(numero)) {
                c.estCache = estCache;
                trouve = true;
                break;
            }
        }
        if (!trouve) {
            ContactConfig nc = new ContactConfig();
            nc.numero = numero;
            nc.nom = nomConversation;
            nc.estCache = estCache;
            nc.intercepterSms = false;
            liste.add(nc);
        }
        prefs.edit().putString("liste", gson.toJson(liste)).apply();
        Toast.makeText(this, estCache ? "Contact masqué" : "Contact visible", Toast.LENGTH_SHORT).show();
    }

    private void ajouterContact() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_ajouter_contact, null);
        final EditText champNom = form.findViewById(R.id.champ_nom);
        final EditText champNumero = form.findViewById(R.id.champ_numero);
        final ToggleButton btnIntercepter = form.findViewById(R.id.btn_intercepter);
        final ToggleButton btnCacher = form.findViewById(R.id.btn_cacher);

        champNom.setText(nomConversation);
        champNumero.setText(numero);

        builder.setTitle("Ajouter / Modifier un contact")
            .setView(form)
            .setPositiveButton("Enregistrer", (dialog, which) -> {
                String nom = champNom.getText().toString().trim();
                String num = champNumero.getText().toString().trim().replaceAll("\\s+", "").replaceAll("^\\+33", "0");
                boolean intercepter = btnIntercepter.isChecked();
                boolean cacher = btnCacher.isChecked();

                if (TextUtils.isEmpty(nom)) {
                    Toast.makeText(this, "Nom requis", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(num)) {
                    Toast.makeText(this, "Numéro requis", Toast.LENGTH_SHORT).show();
                    return;
                }

                SharedPreferences prefs = getSharedPreferences(PREFS_CONTACTS, MODE_PRIVATE);
                Gson gson = new Gson();
                List<ContactConfig> liste = new ArrayList<>();
                String json = prefs.getString("liste", "[]");
                try {
                    Type type = new TypeToken<List<ContactConfig>>(){}.getType();
                    liste = gson.fromJson(json, type);
                } catch (Exception e) {}
                if (liste == null) liste = new ArrayList<>();

                boolean existe = false;
                for (ContactConfig c : liste) {
                    if (c.numero.equals(num)) {
                        c.nom = nom;
                        c.intercepterSms = intercepter;
                        c.estCache = cacher;
                        existe = true;
                        break;
                    }
                }
                if (!existe) {
                    ContactConfig nc = new ContactConfig();
                    nc.nom = nom;
                    nc.numero = num;
                    nc.intercepterSms = intercepter;
                    nc.estCache = cacher;
                    liste.add(nc);
                }
                prefs.edit().putString("liste", gson.toJson(liste)).apply();
                
                numero = num;
                nomConversation = nom;
                setTitle(nom);
                
                Toast.makeText(this, existe ? "Contact modifié ✅" : "Contact ajouté ✅", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void supprimerConversation() {
        new AlertDialog.Builder(this)
            .setTitle("Supprimer la conversation")
            .setMessage("Êtes-vous sûr de vouloir supprimer tous les messages ?")
            .setPositiveButton("Supprimer", (d, w) -> {
                messages.clear();
                sauvegarderMessages();
                adapter.notifyDataSetChanged();
                Toast.makeText(this, "Conversation supprimée ✅", Toast.LENGTH_SHORT).show();
                finish();
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private class MessageAdapter extends BaseAdapter {
        @Override public int getCount() { return messages.size(); }
        @Override public Object getItem(int position) { return messages.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) v = LayoutInflater.from(ConversationActivity.this).inflate(R.layout.item_message, parent, false);
            Message msg = messages.get(position);

            TextView texte = v.findViewById(R.id.texte_message);
            TextView heure = v.findViewById(R.id.heure_message);
            View bulle = v.findViewById(R.id.bulle);

            texte.setText(msg.contenu);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.FRANCE);
            heure.setText(sdf.format(new Date(msg.horodatage)));

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bulle.getLayoutParams();
            if (msg.estEnvoye) {
                params.setMargins(60, 4, 8, 4);
                bulle.setBackgroundColor(0xFFE8F0FE);
            } else {
                params.setMargins(8, 4, 60, 4);
                bulle.setBackgroundColor(0xFFFFFFFF);
            }
            return v;
        }
    }
}
