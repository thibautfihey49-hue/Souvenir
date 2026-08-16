package com.souvenir.messages;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static List<ContactConfig> listeContacts = new ArrayList<>();
    public static List<Conversation> listeConversations = new ArrayList<>();
    public static ContactAdapter adapterContacts;
    public static ConversationAdapter adapterConversations;
    
    private EditText etNom, etNumero;
    private LinearLayout listeVide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        demanderPermissions();
        chargerDonnees();
        initialiserUI();
    }

    private void demanderPermissions() {
        String[] permissions = {
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO
        };
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, 100);
                break;
            }
        }
    }

    private void chargerDonnees() {
        SharedPreferences p1 = getSharedPreferences("ContactsConfig", MODE_PRIVATE);
        SharedPreferences p2 = getSharedPreferences("Conversations", MODE_PRIVATE);
        Gson gson = new Gson();
        
        Type tContacts = new TypeToken<List<ContactConfig>>(){}.getType();
        String jsonContacts = p1.getString("liste", "[]");
        listeContacts = gson.fromJson(jsonContacts, tContacts);
        if (listeContacts == null) listeContacts = new ArrayList<>();

        Type tConv = new TypeToken<List<Conversation>>(){}.getType();
        String jsonConv = p2.getString("liste", "[]");
        listeConversations = gson.fromJson(jsonConv, tConv);
        if (listeConversations == null) listeConversations = new ArrayList<>();
    }

    private void sauvegarderContacts() {
        SharedPreferences prefs = getSharedPreferences("ContactsConfig", MODE_PRIVATE);
        Gson gson = new Gson();
        prefs.edit().putString("liste", gson.toJson(listeContacts)).apply();
    }

    private void initialiserUI() {
        TabLayout tabs = findViewById(R.id.tabs);
        ViewPager2 pager = findViewById(R.id.view_pager);
        etNom = findViewById(R.id.et_nom);
        etNumero = findViewById(R.id.et_numero);
        listeVide = findViewById(R.id.liste_vide);

        SectionsPagerAdapter adapter = new SectionsPagerAdapter(this);
        pager.setAdapter(adapter);

        new TabLayoutMediator(tabs, pager, (tab, pos) -> {
            tab.setText(pos == 0 ? "📇 Contacts" : "💬 Conversations");
        }).attach();

        adapterContacts = new ContactAdapter();
        adapterConversations = new ConversationAdapter();

        mettreAJourAffichage();

        findViewById(R.id.btn_ajouter).setOnClickListener(v -> ajouterContact());
    }

    private void ajouterContact() {
        String nom = etNom.getText().toString().trim();
        String numero = etNumero.getText().toString().trim();

        if (nom.isEmpty() || numero.isEmpty()) return;

        for (ContactConfig c : listeContacts) {
            if (c.numero.equals(numero)) return;
        }

        ContactConfig nc = new ContactConfig();
        nc.nom = nom;
        nc.numero = numero;
        listeContacts.add(nc);

        sauvegarderContacts();
        mettreAJourAffichage();
        etNom.setText("");
        etNumero.setText("");
    }

    public void mettreAJourAffichage() {
        chargerDonnees();
        
        if (adapterContacts != null) {
            adapterContacts.submitList(new ArrayList<>(listeContacts));
        }
        if (adapterConversations != null) {
            adapterConversations.submitList(new ArrayList<>(listeConversations));
        }
        
        if (listeVide != null) {
            listeVide.setVisibility(listeContacts.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mettreAJourAffichage();
    }
}
