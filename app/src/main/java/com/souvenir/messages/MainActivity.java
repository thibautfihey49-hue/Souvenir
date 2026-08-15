package com.souvenir.messages;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MAIN";
    private static final String PREFS_CONVERSATIONS = "Conversations";
    private static final String PREFS_CONTACTS = "ContactsConfig";
    private static final String PREFS_PARAMS = "Parametres";
    
    private ListView listView;
    private LinearLayout listeVide;
    private ConversationAdapter adapter;
    private List<Conversation> toutesConversations = new ArrayList<>();
    private List<Conversation> conversationsVisibles = new ArrayList<>();
    private List<Conversation> conversationsFiltrees = new ArrayList<>();
    private ImageButton btnAjouter, btnNouveauContact;
    private TextView badgeCache;
    private EditText champRecherche;
    private int compteurClicBadge = 0;
    private long dernierClicBadge = 0;
    private static final int SEUIL_CLIC_BADGE = 3;
    private boolean modeSombre = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chargerParametres();
        appliquerTheme();
        setContentView(R.layout.activity_main);
        
        listView = findViewById(R.id.list_conversations);
        listeVide = findViewById(R.id.liste_vide);
        btnAjouter = findViewById(R.id.btn_ajouter);
        btnNouveauContact = findViewById(R.id.btn_nouveau_contact);
        badgeCache = findViewById(R.id.badge_cache);
        champRecherche = findViewById(R.id.champ_recherche);

        chargerConversations();
        conversationsFiltrees.addAll(conversationsVisibles);
        adapter = new ConversationAdapter();
        listView.setAdapter(adapter);
        mettreAJourListeVide();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Conversation conv = conversationsFiltrees.get(position);
            Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
            intent.putExtra("numero", conv.numero);
            intent.putExtra("nom", conv.nom);
            startActivity(intent);
            conv.nonLu = 0;
            adapter.notifyDataSetChanged();
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            Conversation conv = conversationsFiltrees.get(position);
            afficherMenuActions(conv);
            return true;
        });

        // 📞 NOUVEAU : Bouton créer contact directement
        btnNouveauContact.setOnClickListener(v -> afficherDialogNouveauContact());

        // 💬 Bouton nouvelle conversation
        btnAjouter.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
            startActivity(intent);
        });

        badgeCache.setOnClickListener(v -> {
            long maintenant = System.currentTimeMillis();
            if (maintenant - dernierClicBadge > 2000) {
                compteurClicBadge = 1;
            } else {
                compteurClicBadge++;
            }
            dernierClicBadge = maintenant;
            if (compteurClicBadge >= SEUIL_CLIC_BADGE) {
                compteurClicBadge = 0;
                Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
                intent.putExtra("mode_cache", true);
                startActivity(intent);
            } else {
                badgeCache.setText(String.valueOf(SEUIL_CLIC_BADGE - compteurClicBadge));
            }
        });

        champRecherche.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrerConversations(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        mettreAJourBadgeCache();
    }

    // 📞 Créer un contact directement depuis l'accueil
    private void afficherDialogNouveauContact() {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_ajouter_contact, null);
        final EditText champNom = form.findViewById(R.id.champ_nom);
        final EditText champNumero = form.findViewById(R.id.champ_numero);
        final android.widget.ToggleButton btnIntercepter = form.findViewById(R.id.btn_intercepter);
        final android.widget.ToggleButton btnCacher = form.findViewById(R.id.btn_cacher);

        champNumero.setHint("06.. ou +33..");

        new AlertDialog.Builder(this)
            .setTitle("Nouveau Contact")
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
                Toast.makeText(this, existe ? "Contact modifié ✅" : "Contact ajouté ✅", Toast.LENGTH_SHORT).show();
                
                // Ouvre directement la conversation avec ce contact
                Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
                intent.putExtra("numero", num);
                intent.putExtra("nom", nom);
                startActivity(intent);
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        chargerConversations();
        filtrerConversations(champRecherche.getText().toString());
        adapter.notifyDataSetChanged();
        mettreAJourListeVide();
        mettreAJourBadgeCache();
    }

    private void chargerParametres() {
        SharedPreferences prefs = getSharedPreferences(PREFS_PARAMS, MODE_PRIVATE);
        modeSombre = prefs.getBoolean("mode_sombre", false);
    }

    private void appliquerTheme() {
        if (modeSombre) {
            setTheme(R.style.Theme_Messages_Dark);
        } else {
            setTheme(R.style.Theme_Messages);
        }
    }

    private void chargerConversations() {
        SharedPreferences prefs = getSharedPreferences(PREFS_CONVERSATIONS, MODE_PRIVATE);
        SharedPreferences prefsContacts = getSharedPreferences(PREFS_CONTACTS, MODE_PRIVATE);
        Gson gson = new Gson();

        String jsonConv = prefs.getString("liste", "[]");
        toutesConversations.clear();
        try {
            Type type = new TypeToken<List<Conversation>>(){}.getType();
            toutesConversations = gson.fromJson(jsonConv, type);
        } catch (Exception e) { Log.e(TAG, "Erreur chargement: " + e.getMessage()); }
        if (toutesConversations == null) toutesConversations = new ArrayList<>();

        String jsonContacts = prefsContacts.getString("liste", "[]");
        List<ContactConfig> contacts = new ArrayList<>();
        try {
            Type type = new TypeToken<List<ContactConfig>>(){}.getType();
            contacts = gson.fromJson(jsonContacts, type);
        } catch (Exception e) {}

        for (Conversation conv : toutesConversations) {
            for (ContactConfig c : contacts) {
                if (c.numero.equals(conv.numero)) {
                    conv.nom = c.nom;
                    conv.estCache = c.estCache;
                    break;
                }
            }
        }

        Collections.sort(toutesConversations, (a, b) -> {
            if (Boolean.TRUE.equals(a.epingle) != Boolean.TRUE.equals(b.epingle)) {
                return Boolean.TRUE.equals(a.epingle) ? -1 : 1;
            }
            return Long.compare(b.horodatage, a.horodatage);
        });

        conversationsVisibles.clear();
        for (Conversation c : toutesConversations) if (!c.estCache) conversationsVisibles.add(c);
    }

    private void filtrerConversations(String recherche) {
        recherche = recherche.toLowerCase().trim();
        conversationsFiltrees.clear();
        if (recherche.isEmpty()) {
            conversationsFiltrees.addAll(conversationsVisibles);
        } else {
            for (Conversation c : conversationsVisibles) {
                if (c.nom.toLowerCase().contains(recherche) || c.numero.contains(recherche) || c.dernierMessage.toLowerCase().contains(recherche)) {
                    conversationsFiltrees.add(c);
                }
            }
        }
        adapter.notifyDataSetChanged();
        mettreAJourListeVide();
    }

    private void mettreAJourListeVide() {
        if (conversationsFiltrees.isEmpty()) {
            listView.setVisibility(View.GONE);
            listeVide.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            listeVide.setVisibility(View.GONE);
        }
    }

    private void afficherMenuActions(Conversation conv) {
        String[] options = {"Épingler/Désépingler", "Supprimer", "Masquer"};
        new AlertDialog.Builder(this)
            .setTitle("Actions")
            .setItems(options, (d, which) -> {
                if (which == 0) {
                    conv.epingle = !Boolean.TRUE.equals(conv.epingle);
                    sauvegarderConversations();
                    adapter.notifyDataSetChanged();
                } else if (which == 1) {
                    toutesConversations.remove(conv);
                    sauvegarderConversations();
                    filtrerConversations(champRecherche.getText().toString());
                } else if (which == 2) {
                    conv.estCache = true;
                    sauvegarderConversations();
                    filtrerConversations(champRecherche.getText().toString());
                }
            })
            .show();
    }

    private void sauvegarderConversations() {
        SharedPreferences prefs = getSharedPreferences(PREFS_CONVERSATIONS, MODE_PRIVATE);
        Gson gson = new Gson();
        prefs.edit().putString("liste", gson.toJson(toutesConversations)).apply();
    }

    private void mettreAJourBadgeCache() {
        int compteCache = 0;
        for (Conversation c : toutesConversations) if (c.estCache) compteCache++;
        badgeCache.setVisibility(compteCache > 0 ? View.VISIBLE : View.GONE);
        badgeCache.setText("" + compteCache);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_mode_sombre).setChecked(modeSombre);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_mode_sombre) {
            modeSombre = !modeSombre;
            getSharedPreferences(PREFS_PARAMS, MODE_PRIVATE).edit().putBoolean("mode_sombre", modeSombre).apply();
            recreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ConversationAdapter extends BaseAdapter {
        @Override public int getCount() { return conversationsFiltrees.size(); }
        @Override public Object getItem(int position) { return conversationsFiltrees.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) v = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_conversation, parent, false);
            Conversation conv = conversationsFiltrees.get(position);

            TextView avatar = v.findViewById(R.id.avatar);
            TextView nom = v.findViewById(R.id.nom);
            TextView message = v.findViewById(R.id.dernier_message);
            TextView heure = v.findViewById(R.id.heure);
            TextView badgeLu = v.findViewById(R.id.badge_non_lu);
            View iconeEpingle = v.findViewById(R.id.icone_epingle);

            avatar.setText(conv.nom.substring(0, 1).toUpperCase());
            nom.setText(conv.nom);
            message.setText(conv.dernierMessage);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.FRANCE);
            heure.setText(sdf.format(new Date(conv.horodatage)));

            iconeEpingle.setVisibility(Boolean.TRUE.equals(conv.epingle) ? View.VISIBLE : View.GONE);

            if (conv.nonLu > 0) {
                badgeLu.setVisibility(View.VISIBLE);
                badgeLu.setText(String.valueOf(conv.nonLu));
                nom.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                badgeLu.setVisibility(View.GONE);
                nom.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
            return v;
        }
    }
}
