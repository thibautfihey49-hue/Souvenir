package com.souvenir.messages;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationActivity extends AppCompatActivity {
    private static final String TAG = "CONV";
    private static final String PREFS_CONVERSATIONS = "Conversations";
    private static final int PERMISSION_MMS = 2001;
    private static final int PERMISSION_CAMERA = 2002;
    private static final int PERMISSION_STORAGE = 2003;
    private static final int CHOISIR_PHOTO = 3001;
    private static final int PRENDRE_PHOTO = 3002;

    private ListView listView;
    private MessageAdapter adapter;
    private List<Message> messages = new ArrayList<>();
    private EditText champMessage;
    private ImageButton btnEnvoyer, btnPhoto, btnEmoji, btnAppel, btnVisio;
    private LinearLayout clavierEmojis;
    private TextView titreNom;
    private String numero, nom;
    private boolean modeCache = false;
    private Bitmap photoSelectionnee;

    private static final String[] EMOJIS = {
        "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
        "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩",
        "😘", "😗", "😚", "😙", "🥲", "😋", "😛", "😜",
        "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐",
        "😐", "😑", "😶", "😏", "😒", "🙄", "😬", "🤥",
        "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕",
        "🤢", "🤮", "🤧", "🥵", "🥶", "🥴", "😵", "🤯",
        "👍", "👎", "👏", "🙌", "👐", "🤲", "🤝", "🙏",
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
        "🔥", "⭐", "✨", "💯", "✅", "❌", "⚠️", "❓",
        "☀️", "🌙", "🌈", "🌧️", "❄️", "⚡", "🍀", "🌸",
        "🎂", "🎁", "🎈", "🎉", "🎊", "🏠", "🚗", "✈️",
        "📞", "📱", "💬", "📷", "🎵", "🎬", "📚", "💻"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        numero = getIntent().getStringExtra("numero");
        nom = getIntent().getStringExtra("nom");
        modeCache = getIntent().getBooleanExtra("mode_cache", false);

        if (numero == null) numero = "";
        if (nom == null || nom.isEmpty()) nom = numero;

        titreNom = findViewById(R.id.titre_contact);
        titreNom.setText(nom);

        listView = findViewById(R.id.liste_messages);
        champMessage = findViewById(R.id.champ_message);
        btnEnvoyer = findViewById(R.id.btn_envoyer);
        btnPhoto = findViewById(R.id.btn_photo);
        btnEmoji = findViewById(R.id.btn_emoji);
        btnAppel = findViewById(R.id.btn_appel);
        btnVisio = findViewById(R.id.btn_visio);
        clavierEmojis = findViewById(R.id.clavier_emojis);

        chargerMessages();
        adapter = new MessageAdapter();
        listView.setAdapter(adapter);
        defilerEnBas();

        btnEnvoyer.setOnClickListener(v -> envoyerMessage());
        btnPhoto.setOnClickListener(v -> choisirSourcePhoto());
        btnEmoji.setOnClickListener(v -> {
            if (clavierEmojis.getVisibility() == View.VISIBLE) {
                clavierEmojis.setVisibility(View.GONE);
            } else {
                clavierEmojis.setVisibility(View.VISIBLE);
                chargerEmojis();
            }
        });
        btnAppel.setOnClickListener(v -> lancerAppelVocal());
        btnVisio.setOnClickListener(v -> lancerAppelVisio());

        demanderPermissions();
    }

    private void choisirSourcePhoto() {
        String[] options = {"📷 Prendre une photo", "🖼️ Choisir dans la galerie"};
        new AlertDialog.Builder(this)
            .setTitle("Joindre une photo")
            .setItems(options, (d, which) -> {
                if (which == 0) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                        prendrePhoto();
                    } else {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                            == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED) {
                        choisirPhotoGalerie();
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_STORAGE);
                        } else {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
                        }
                    }
                }
            })
            .show();
    }

    private void prendrePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, PRENDRE_PHOTO);
        }
    }

    private void choisirPhotoGalerie() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, CHOISIR_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Bitmap bitmap = null;
            if (requestCode == PRENDRE_PHOTO && data != null && data.getExtras() != null) {
                bitmap = (Bitmap) data.getExtras().get("data");
            } else if (requestCode == CHOISIR_PHOTO && data != null && data.getData() != null) {
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                } catch (Exception e) {
                    Toast.makeText(this, "Erreur chargement photo", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if (bitmap != null) {
                photoSelectionnee = reduireImage(bitmap);
                afficherApercuPhoto();
            }
        }
    }

    private Bitmap reduireImage(Bitmap bitmap) {
        int max = 800;
        if (bitmap.getWidth() > max || bitmap.getHeight() > max) {
            float ratio = (float) bitmap.getWidth() / bitmap.getHeight();
            int w, h;
            if (ratio > 1) { w = max; h = (int) (max / ratio); }
            else { h = max; w = (int) (max * ratio); }
            return Bitmap.createScaledBitmap(bitmap, w, h, true);
        }
        return bitmap;
    }

    private void afficherApercuPhoto() {
        View apercu = getLayoutInflater().inflate(R.layout.apercu_photo, null);
        ImageView iv = apercu.findViewById(R.id.apercu_image);
        iv.setImageBitmap(photoSelectionnee);

        new AlertDialog.Builder(this)
            .setTitle("Envoyer cette photo ?")
            .setView(apercu)
            .setPositiveButton("📤 Envoyer MMS", (d, w) -> envoyerMMS())
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void envoyerMMS() {
        try {
            File fichierImg = new File(getExternalCacheDir(), "mms_photo.jpg");
            FileOutputStream fos = new FileOutputStream(fichierImg);
            photoSelectionnee.compress(Bitmap.CompressFormat.JPEG, 70, fos);
            fos.flush();
            fos.close();

            Uri uri = Uri.fromFile(fichierImg);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra("address", numero);
            intent.putExtra("sms_body", "[Photo]");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setType("image/jpeg");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Envoyer MMS avec"));

            // ✅ Utilisation du constructeur sans paramètres
            Message msg = new Message();
            msg.texte = "📷 [Photo envoyée par MMS]";
            msg.envoye = true;
            msg.horodatage = System.currentTimeMillis();
            msg.photoBase64 = bitmapToBase64(photoSelectionnee);
            messages.add(msg);
            sauvegarderMessages();
            adapter.notifyDataSetChanged();
            defilerEnBas();
            photoSelectionnee = null;

        } catch (Exception e) {
            Toast.makeText(this, "Erreur envoi MMS", Toast.LENGTH_SHORT).show();
        }
    }

    private String bitmapToBase64(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    private void envoyerMessage() {
        String texte = champMessage.getText().toString().trim();
        if (TextUtils.isEmpty(texte) && photoSelectionnee == null) return;

        if (photoSelectionnee != null) {
            envoyerMMS();
            return;
        }

        // ✅ Utilisation du constructeur sans paramètres
        Message msg = new Message();
        msg.texte = texte;
        msg.envoye = true;
        msg.horodatage = System.currentTimeMillis();
        messages.add(msg);

        try {
            android.telephony.SmsManager.getDefault().sendTextMessage(numero, null, texte, null, null);
        } catch (Exception e) {
            Toast.makeText(this, "SMS envoyé", Toast.LENGTH_SHORT).show();
        }

        champMessage.setText("");
        sauvegarderMessages();
        adapter.notifyDataSetChanged();
        defilerEnBas();
    }

    private void lancerAppelVocal() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + numero));
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 101);
        }
    }

    private void lancerAppelVisio() {
        String[] options = {"📞 Google Duo / Meet", "📹 WhatsApp", "🎨 Signal", "🌐 Autre..."};
        new AlertDialog.Builder(this)
            .setTitle("Appel Visio")
            .setMessage("Choisissez une application pour l'appel vidéo")
            .setItems(options, (d, which) -> {
                try {
                    Intent intent = null;
                    if (which == 0) {
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse("duo://call/" + numero));
                        if (getPackageManager().resolveActivity(intent, 0) == null) {
                            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://meet.google.com/"));
                        }
                    } else if (which == 1) {
                        String num = numero.replaceAll("^0", "+33").replaceAll("\\s+", "");
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?phone=" + num));
                    } else if (which == 2) {
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://signal.app/send/" + numero));
                    } else {
                        intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + numero));
                    }
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Application non disponible", Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }

    private void chargerEmojis() {
        GridView grid = clavierEmojis.findViewById(R.id.grid_emojis);
        EmojiAdapter adapter = new EmojiAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, v, pos, id) -> {
            champMessage.getText().insert(champMessage.getSelectionStart(), EMOJIS[pos]);
        });
    }

    private void chargerMessages() {
        SharedPreferences prefs = getSharedPreferences(PREFS_CONVERSATIONS, MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(numero, "[]");
        try {
            Type type = new TypeToken<List<Message>>(){}.getType();
            messages = gson.fromJson(json, type);
        } catch (Exception e) { messages = new ArrayList<>(); }
        if (messages == null) messages = new ArrayList<>();
    }

    private void sauvegarderMessages() {
        SharedPreferences prefs = getSharedPreferences(PREFS_CONVERSATIONS, MODE_PRIVATE);
        Gson gson = new Gson();
        prefs.edit().putString(numero, gson.toJson(messages)).apply();
    }

    private void defilerEnBas() {
        listView.post(() -> listView.setSelection(adapter.getCount() - 1));
    }

    private void demanderPermissions() {
        List<String> perms = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.SEND_SMS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.READ_SMS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.RECEIVE_SMS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.CALL_PHONE);
        if (!perms.isEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), 1001);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_conversation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class MessageAdapter extends BaseAdapter {
        @Override public int getCount() { return messages.size(); }
        @Override public Object getItem(int p) { return messages.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int p, View v, ViewGroup parent) {
            Message m = messages.get(p);
            if (v == null) v = getLayoutInflater().inflate(R.layout.item_message, parent, false);

            // ✅ Utilisation des bons IDs du layout
            TextView tvTexte = v.findViewById(R.id.msg_texte);
            TextView tvHeure = v.findViewById(R.id.msg_heure);
            LinearLayout bulle = v.findViewById(R.id.msg_bulle);
            ImageView ivPhoto = v.findViewById(R.id.msg_photo);

            tvTexte.setText(m.texte);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.FRANCE);
            tvHeure.setText(sdf.format(new Date(m.horodatage)));

            if (m.envoye) {
                bulle.setBackgroundResource(R.drawable.bulle_envoyee);
                ((LinearLayout.LayoutParams) bulle.getLayoutParams()).gravity = android.view.Gravity.END;
                tvTexte.setTextColor(0xFFFFFFFF);
            } else {
                bulle.setBackgroundResource(R.drawable.bulle_recue);
                ((LinearLayout.LayoutParams) bulle.getLayoutParams()).gravity = android.view.Gravity.START;
                tvTexte.setTextColor(0xFF000000);
            }

            if (m.photoBase64 != null && !m.photoBase64.isEmpty()) {
                try {
                    byte[] bytes = Base64.decode(m.photoBase64, Base64.NO_WRAP);
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    ivPhoto.setImageBitmap(bmp);
                    ivPhoto.setVisibility(View.VISIBLE);
                    tvTexte.setVisibility(View.GONE);
                } catch (Exception e) {
                    ivPhoto.setVisibility(View.GONE);
                    tvTexte.setVisibility(View.VISIBLE);
                }
            } else {
                ivPhoto.setVisibility(View.GONE);
                tvTexte.setVisibility(View.VISIBLE);
            }

            return v;
        }
    }

    private class EmojiAdapter extends BaseAdapter {
        @Override public int getCount() { return EMOJIS.length; }
        @Override public Object getItem(int p) { return EMOJIS[p]; }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int p, View v, ViewGroup parent) {
            TextView tv = new TextView(ConversationActivity.this);
            tv.setText(EMOJIS[p]);
            tv.setTextSize(24);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setPadding(8, 8, 8, 8);
            tv.setBackgroundResource(android.R.drawable.list_selector_background);
            return tv;
        }
    }
}
