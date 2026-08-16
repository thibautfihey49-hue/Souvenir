package com.souvenir.messages;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_CONTACTS = "ContactsConfig";
    private static final String PREFS_CONVERSATIONS = "Conversations";

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ContactAdapter contactAdapter;
    private ConversationAdapter convAdapter;
    private List<ContactConfig> contacts = new ArrayList<>();
    private List<Conversation> conversations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);
        setupViewPager();
        chargerContacts();
        chargerConversations();
        demanderPermissions();
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter();
        viewPager.setAdapter(adapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText("📇 Contacts");
            else tab.setText("💬 Conversations");
        }).attach();
    }

    private class ViewPagerAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<PageHolder> {
        @NonNull @Override public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int pos) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_onglet, parent, false);
            return new PageHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull PageHolder h, int pos) { h.bind(pos); }
        @Override public int getItemCount() { return 2; }
    }

    private class PageHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        final EditText champRecherche, champNom, champNumero;
        final Button btnAjouter;
        final LinearLayout zoneFormulaire;
        final TextView titreListe, videTexte;
        final androidx.recyclerview.widget.RecyclerView recyclerView;

        PageHolder(View v) {
            super(v);
            champRecherche = v.findViewById(R.id.champ_recherche);
            champNom = v.findViewById(R.id.nouveau_nom);
            champNumero = v.findViewById(R.id.nouveau_numero);
            btnAjouter = v.findViewById(R.id.btn_ajouter);
            zoneFormulaire = v.findViewById(R.id.zone_formulaire);
            titreListe = v.findViewById(R.id.titre_liste);
            videTexte = v.findViewById(R.id.texte_vide);
            recyclerView = v.findViewById(R.id.recycler_view);
        }

        void bind(int position) {
            recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(itemView.getContext()));

            if (position == 0) {
                zoneFormulaire.setVisibility(View.VISIBLE);
                titreListe.setText("📇 Mes Contacts");
                contactAdapter = new ContactAdapter();
                recyclerView.setAdapter(contactAdapter);
                contactAdapter.submitList(contacts);

                btnAjouter.setOnClickListener(v -> {
                    String nom = champNom.getText().toString().trim();
                    String num = champNumero.getText().toString().trim().replaceAll("\\s+", "").replaceAll("^\\+33", "0");
                    if (TextUtils.isEmpty(nom) || TextUtils.isEmpty(num)) return;
                    sauvegarderContact(nom, num);
                    champNom.setText(""); champNumero.setText("");
                });

                champRecherche.addTextChangedListener(new TextWatcher() {
                    public void onTextChanged(CharSequence s, int start, int before, int count) { filtrerContacts(s.toString()); }
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    public void afterTextChanged(Editable s) {}
                });

                videTexte.setVisibility(contacts.isEmpty() ? View.VISIBLE : View.GONE);
            } else {
                zoneFormulaire.setVisibility(View.GONE);
                titreListe.setText("💬 Mes Conversations");
                convAdapter = new ConversationAdapter();
                recyclerView.setAdapter(convAdapter);
                convAdapter.submitList(conversations);

                champRecherche.addTextChangedListener(new TextWatcher() {
                    public void onTextChanged(CharSequence s, int start, int before, int count) { filtrerConversations(s.toString()); }
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    public void afterTextChanged(Editable s) {}
                });

                videTexte.setVisibility(conversations.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void sauvegarderContact(String nom, String numero) {
        SharedPreferences prefs = getSharedPreferences(PREFS_CONTACTS, MODE_PRIVATE);
        Gson gson = new Gson();
        List<ContactConfig> liste = new ArrayList<>();
        try {
            Type type = new TypeToken<List<ContactConfig>>(){}.getType();
            liste = gson.fromJson(prefs.getString("liste", "[]"), type);
        } catch (Exception ignored) {}
        if (liste == null) liste = new ArrayList<>();

        boolean existe = false;
        for (ContactConfig c : liste) {
            if (c.numero.equals(numero)) { c.nom = nom; existe = true; break; }
        }
        if (!existe) {
            ContactConfig nc = new ContactConfig();
            nc.nom = nom; nc.numero = numero;
            liste.add(nc);
        }
        prefs.edit().putString("liste", gson.toJson(liste)).apply();
        chargerContacts();
        contactAdapter.submitList(contacts);
    }

    private void chargerContacts() {
        SharedPreferences prefs = getSharedPreferences(PREFS_CONTACTS, MODE_PRIVATE);
        Gson gson = new Gson();
        try {
            Type type = new TypeToken<List<ContactConfig>>(){}.getType();
            contacts = gson.fromJson(prefs.getString("liste", "[]"), type);
        } catch (Exception ignored) {}
        if (contacts == null) contacts = new ArrayList<>();
    }

    private void chargerConversations() {
        SharedPreferences prefs = getSharedPreferences(PREFS_CONVERSATIONS, MODE_PRIVATE);
        Gson gson = new Gson();
        try {
            Type type = new TypeToken<List<Conversation>>(){}.getType();
            conversations = gson.fromJson(prefs.getString("liste", "[]"), type);
        } catch (Exception ignored) {}
        if (conversations == null) conversations = new ArrayList<>();
        Collections.sort(conversations, (a, b) -> Long.compare(b.horodatage, a.horodatage));
    }

    private void filtrerContacts(String recherche) {
        String q = recherche.toLowerCase().trim();
        List<ContactConfig> filtre = new ArrayList<>();
        for (ContactConfig c : contacts) {
            if (c.nom.toLowerCase().contains(q) || c.numero.contains(q)) filtre.add(c);
        }
        contactAdapter.submitList(filtre);
    }

    private void filtrerConversations(String recherche) {
        String q = recherche.toLowerCase().trim();
        List<Conversation> filtre = new ArrayList<>();
        for (Conversation c : conversations) {
            if (c.nom.toLowerCase().contains(q) || c.numero.contains(q) || c.dernierMessage.toLowerCase().contains(q)) filtre.add(c);
        }
        convAdapter.submitList(filtre);
    }

    private void demanderPermissions() {
        String[] perms = {Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.CALL_PHONE, Manifest.permission.RECORD_AUDIO};
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, perms, 1001);
                break;
            }
        }
    }

    private void lancerAppel(String numero) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + numero)));
        }
    }

    private void ouvrirConversation(String numero, String nom) {
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra("numero", numero);
        intent.putExtra("nom", nom);
        startActivity(intent);
    }

    private class ContactAdapter extends androidx.recyclerview.widget.ListAdapter<ContactConfig, ContactHolder> {
        ContactAdapter() { super(ContactConfig.DIFF_CALLBACK); }
        @NonNull @Override public ContactHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new ContactHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull ContactHolder h, int pos) { h.bind(getItem(pos)); }
    }

    private class ContactHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        final TextView avatar, nom, numero;
        ContactHolder(View v) {
            super(v);
            avatar = v.findViewById(R.id.avatar);
            nom = v.findViewById(R.id.nom);
            numero = v.findViewById(R.id.numero);
        }
        void bind(ContactConfig c) {
            avatar.setText(c.nom.substring(0, 1).toUpperCase());
            nom.setText(c.nom);
            numero.setText(c.numero);
            itemView.setOnClickListener(v -> ouvrirConversation(c.numero, c.nom));
            itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(itemView.getContext())
                    .setTitle(c.nom)
                    .setItems(new String[]{"📞 Appeler", "💬 Conversation", "🗑️ Supprimer"}, (d, which) -> {
                        if (which == 0) lancerAppel(c.numero);
                        else if (which == 1) ouvrirConversation(c.numero, c.nom);
                        else supprimerContact(c);
                    }).show();
                return true;
            });
        }
        void supprimerContact(ContactConfig c) {
            contacts.remove(c);
            SharedPreferences prefs = getSharedPreferences(PREFS_CONTACTS, MODE_PRIVATE);
            prefs.edit().putString("liste", new Gson().toJson(contacts)).apply();
            contactAdapter.submitList(new ArrayList<>(contacts));
        }
    }

    private class ConversationAdapter extends androidx.recyclerview.widget.ListAdapter<Conversation, ConvHolder> {
        ConversationAdapter() { super(Conversation.DIFF_CALLBACK); }
        @NonNull @Override public ConvHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new ConvHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull ConvHolder h, int pos) { h.bind(getItem(pos)); }
    }

    private class ConvHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        final TextView avatar, nom, message, heure;
        ConvHolder(View v) {
            super(v);
            avatar = v.findViewById(R.id.avatar);
            nom = v.findViewById(R.id.nom);
            message = v.findViewById(R.id.dernier_message);
            heure = v.findViewById(R.id.heure);
        }
        void bind(Conversation c) {
            avatar.setText(c.nom.substring(0, 1).toUpperCase());
            nom.setText(c.nom);
            message.setText(c.dernierMessage.isEmpty() ? "Appuyer pour écrire" : c.dernierMessage);
            heure.setText(new java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE).format(new java.util.Date(c.horodatage)));
            itemView.setOnClickListener(v -> ouvrirConversation(c.numero, c.nom));
        }
    }
}
