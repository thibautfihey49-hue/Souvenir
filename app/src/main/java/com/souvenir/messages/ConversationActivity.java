package com.souvenir.messages;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.core.content.FileProvider;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationActivity extends AppCompatActivity {
    private static final String PREFS_CONVERSATIONS = "Conversations";
    private static final int PERMISSION_CAMERA = 2002;
    private static final int PERMISSION_STORAGE = 2003;
    private static final int PERMISSION_AUDIO = 2004;
    private static final int CHOISIR_PHOTO = 3001;
    private static final int PRENDRE_PHOTO = 3002;

    private ListView listView;
    private MessageAdapter adapter;
    private List<Message> messages = new ArrayList<>();
    private EditText champMessage;
    private ImageButton btnEnvoyer, btnPhoto, btnEmoji, btnAppel, btnVisio, btnEnregistrerVocal;
    private LinearLayout clavierEmojis, zoneEnregistrement;
    private TextView titreNom, chronoEnregistrement;
    private View boutonStopEnregistrement;
    
    private MediaRecorder mediaRecorder;
    private File fichierAudioTemp;
    private long debutEnregistrement = 0;
    private Handler handlerChrono = new Handler(Looper.getMainLooper());
    private boolean estEnregistrementEnCours = false;
    private MediaPlayer lecteurAudio = null;

    private String numero, nom;
    private Bitmap photoSelectionnee;

    private static final String[] EMOJIS = {
        "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
        "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩",
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
        "👍", "👎", "👏", "🙌", "🤲", "🤝", "🙏", "🔥"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        numero = getIntent().getStringExtra("numero");
        nom = getIntent().getStringExtra("nom");
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
        btnEnregistrerVocal = findViewById(R.id.btn_vocal);
        clavierEmojis = findViewById(R.id.clavier_emojis);
        zoneEnregistrement = findViewById(R.id.zone_enregistrement);
        chronoEnregistrement = findViewById(R.id.chrono_enregistrement);
        boutonStopEnregistrement = findViewById(R.id.btn_stop_enregistrement);

        chargerMessages();
        adapter = new MessageAdapter();
        listView.setAdapter(adapter);
        defilerEnBas();

        btnEnvoyer.setOnClickListener(v -> envoyerMessage());
        btnPhoto.setOnClickListener(v -> choisirSourcePhoto());
        btnEmoji.setOnClickListener(v -> {
            clavierEmojis.setVisibility(clavierEmojis.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            zoneEnregistrement.setVisibility(View.GONE);
            chargerEmojis();
        });
        btnAppel.setOnClickListener(v -> lancerAppelVocal());
        btnVisio.setOnClickListener(v -> lancerAppelVisio());
        btnEnregistrerVocal.setOnClickListener(v -> demanderPermissionEtEnregistrer());
        boutonStopEnregistrement.setOnClickListener(v -> arreterEnregistrementEtEnvoyer());

        demanderPermissions();
    }

    private void demanderPermissionEtEnregistrer() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            commencerEnregistrement();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_AUDIO);
        }
    }

    private void commencerEnregistrement() {
        try {
            fichierAudioTemp = new File(getExternalCacheDir(), "msg_vocal_" + System.currentTimeMillis() + ".3gp");
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(fichierAudioTemp.getAbsolutePath());
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.prepare();
            mediaRecorder.start();

            estEnregistrementEnCours = true;
            debutEnregistrement = System.currentTimeMillis();
            
            zoneEnregistrement.setVisibility(View.VISIBLE);
            clavierEmojis.setVisibility(View.GONE);
            champMessage.setVisibility(View.GONE);
            btnEnvoyer.setVisibility(View.GONE);
            
            handlerChrono.postDelayed(new Runnable() {
                @Override public void run() {
                    if (!estEnregistrementEnCours) return;
                    long sec = (System.currentTimeMillis() - debutEnregistrement) / 1000;
                    chronoEnregistrement.setText(String.format(Locale.FRANCE, "⏱️ %02d:%02d", sec / 60, sec % 60));
                    handlerChrono.postDelayed(this, 500);
                }
            }, 0);

            Toast.makeText(this, "🎤 Enregistrement...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Erreur enregistrement : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void arreterEnregistrementEtEnvoyer() {
        if (!estEnregistrementEnCours || mediaRecorder == null) return;
        
        try { mediaRecorder.stop(); mediaRecorder.release(); } catch (Exception ignored) {}
        mediaRecorder = null;
        estEnregistrementEnCours = false;
        handlerChrono.removeCallbacksAndMessages(null);

        long duree = System.currentTimeMillis() - debutEnregistrement;

        zoneEnregistrement.setVisibility(View.GONE);
        champMessage.setVisibility(View.VISIBLE);
        btnEnvoyer.setVisibility(View.VISIBLE);

        String audioB64 = fichierVersBase64(fichierAudioTemp);
        if (audioB64 == null) {
            Toast.makeText(this, "Erreur préparation audio", Toast.LENGTH_SHORT).show();
            return;
        }

        Message msg = new Message();
        msg.texte = "🎤 Message vocal";
        msg.audioBase64 = audioB64;
        msg.dureeMs = duree;
        msg.envoye = true;
        msg.horodatage = System.currentTimeMillis();
        messages.add(msg);

        sauvegarderMessages();
        adapter.notifyDataSetChanged();
        defilerEnBas();

        try { fichierAudioTemp.delete(); } catch (Exception ignored) {}
        Toast.makeText(this, "✅ Message vocal envoyé", Toast.LENGTH_SHORT).show();
    }

    private String fichierVersBase64(File fichier) {
        try {
            FileInputStream fis = new FileInputStream(fichier);
            byte[] data = new byte[(int) fichier.length()];
            fis.read(data);
            fis.close();
            return Base64.encodeToString(data, Base64.NO_WRAP);
        } catch (Exception e) { return null; }
    }

    private void lireMessageVocal(String base64Audio) {
        if (lecteurAudio != null) { lecteurAudio.release(); lecteurAudio = null; }
        try {
            byte[] data = Base64.decode(base64Audio, Base64.NO_WRAP);
            File temp = File.createTempFile("lecture_", ".3gp", getExternalCacheDir());
            FileOutputStream fos = new FileOutputStream(temp);
            fos.write(data);
            fos.close();

            lecteurAudio = new MediaPlayer();
            lecteurAudio.setDataSource(temp.getAbsolutePath());
            lecteurAudio.prepare();
            lecteurAudio.start();
            lecteurAudio.setOnCompletionListener(mp -> {
                mp.release();
                temp.delete();
                lecteurAudio = null;
            });
            Toast.makeText(this, "▶️ Lecture...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Erreur lecture : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void choisirSourcePhoto() {
        String[] options = {"📷 Prendre une photo", "🖼️ Choisir dans la galerie"};
        new AlertDialog.Builder(this)
            .setTitle("Joindre une photo pour MMS")
            .setItems(options, (d, which) -> {
                if (which == 0) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) prendrePhoto();
                    else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) choisirPhotoGalerie();
                        else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_STORAGE);
                    } else {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) choisirPhotoGalerie();
                        else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
                    }
                }
            }).show();
    }

    private void prendrePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) startActivityForResult(intent, PRENDRE_PHOTO);
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
                try { bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData()); }
                catch (Exception e) { Toast.makeText(this, "Erreur chargement photo", Toast.LENGTH_SHORT).show(); return; }
            }
            if (bitmap != null) { photoSelectionnee = reduireImage(bitmap); afficherApercuPhoto(); }
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
        ((ImageView) apercu.findViewById(R.id.apercu_image)).setImageBitmap(photoSelectionnee);
        new AlertDialog.Builder(this)
            .setTitle("Envoyer cette photo par MMS ?")
            .setView(apercu)
            .setPositiveButton("📤 Envoyer MMS", (d, w) -> envoyerMMS())
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void envoyerMMS() {
        try {
            File fichier = new File(getExternalCacheDir(), "mms_photo_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(fichier);
            photoSelectionnee.compress(Bitmap.CompressFormat.JPEG, 70, fos);
            fos.flush();
            fos.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", fichier);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra("address", numero);
            intent.putExtra("sms_body", "[Photo]");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setType("image/jpeg");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Envoyer MMS avec"));

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
            Toast.makeText(this, "Erreur envoi MMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        if (photoSelectionnee != null) { envoyerMMS(); return; }

        Message msg = new Message();
        msg.texte = texte;
        msg.envoye = true;
        msg.horodatage = System.currentTimeMillis();
        messages.add(msg);

        try {
            android.telephony.SmsManager.getDefault().sendTextMessage(numero, null, texte, null, null);
        } catch (Exception e) {
            Toast.makeText(this, "SMS enregistré", Toast.LENGTH_SHORT).show();
        }

        champMessage.setText("");
        sauvegarderMessages();
        adapter.notifyDataSetChanged();
        defilerEnBas();
    }

    private void lancerAppelVocal() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + numero)));
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 101);
        }
    }

    private void lancerAppelVisio() {
        String[] options = {"📞 Google Meet", "📹 WhatsApp", "🎨 Signal", "🌐 Autre"};
        new AlertDialog.Builder(this)
            .setTitle("Appel Vidéo")
            .setItems(options, (d, which) -> {
                try {
                    Intent intent = null;
                    if (which == 0) intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://meet.google.com/"));
                    else if (which == 1) intent = new Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?phone=" + numero.replaceAll("^0", "+33").replaceAll("\\s+", "")));
                    else if (which == 2) intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://signal.app/send/" + numero));
                    else intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + numero));
                    startActivity(intent);
                } catch (Exception e) { Toast.makeText(this, "Application indisponible", Toast.LENGTH_SHORT).show(); }
            }).show();
    }

    private void chargerEmojis() {
        GridView grid = clavierEmojis.findViewById(R.id.grid_emojis);
        grid.setAdapter(new android.widget.BaseAdapter() {
            @Override public int getCount() { return EMOJIS.length; }
            @Override public Object getItem(int p) { return EMOJIS[p]; }
            @Override public long getItemId(int p) { return p; }
            @Override public View getView(int p, View v, ViewGroup parent) {
                TextView tv = new TextView(ConversationActivity.this);
                tv.setText(EMOJIS[p]);
                tv.setTextSize(24);
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setPadding(12, 12, 12, 12);
                tv.setBackgroundResource(android.R.drawable.list_selector_background);
                tv.setOnClickListener(c -> champMessage.getText().insert(champMessage.getSelectionStart(), EMOJIS[p]));
                return tv;
            }
        });
    }

    private void chargerMessages() {
        SharedPreferences prefs = getSharedPreferences(PREFS_CONVERSATIONS, MODE_PRIVATE);
        Gson gson = new Gson();
        try {
            Type type = new TypeToken<List<Message>>(){}.getType();
            messages = gson.fromJson(prefs.getString(numero, "[]"), type);
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.SEND_SMS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.READ_SMS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.RECEIVE_SMS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.CALL_PHONE);
        if (!perms.isEmpty()) ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), 1001);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] resultat) {
        super.onRequestPermissionsResult(code, perms, resultat);
        if (code == PERMISSION_AUDIO && resultat.length > 0 && resultat[0] == PackageManager.PERMISSION_GRANTED) {
            commencerEnregistrement();
        }
    }

    private class MessageAdapter extends android.widget.BaseAdapter {
        @Override public int getCount() { return messages.size(); }
        @Override public Object getItem(int p) { return messages.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int p, View v, ViewGroup parent) {
            Message m = messages.get(p);
            if (v == null) v = getLayoutInflater().inflate(R.layout.item_message, parent, false);

            TextView tvTexte = v.findViewById(R.id.msg_texte);
            TextView tvHeure = v.findViewById(R.id.msg_heure);
            TextView tvDuree = v.findViewById(R.id.msg_duree_vocal);
            LinearLayout bulle = v.findViewById(R.id.msg_bulle);
            ImageView ivPhoto = v.findViewById(R.id.msg_photo);
            ImageView btnLireVocal = v.findViewById(R.id.btn_lire_vocal);

            tvHeure.setText(new SimpleDateFormat("HH:mm", Locale.FRANCE).format(new Date(m.horodatage)));

            if (m.envoye) {
                bulle.setBackgroundResource(R.drawable.bulle_envoyee);
                ((LinearLayout.LayoutParams) bulle.getLayoutParams()).gravity = android.view.Gravity.END;
                tvTexte.setTextColor(0xFFFFFFFF);
            } else {
                bulle.setBackgroundResource(R.drawable.bulle_recue);
                ((LinearLayout.LayoutParams) bulle.getLayoutParams()).gravity = android.view.Gravity.START;
                tvTexte.setTextColor(0xFF1F2937);
            }

            if (m.audioBase64 != null && !m.audioBase64.isEmpty()) {
                tvTexte.setVisibility(View.GONE);
                ivPhoto.setVisibility(View.GONE);
                btnLireVocal.setVisibility(View.VISIBLE);
                tvDuree.setVisibility(View.VISIBLE);
                long sec = m.dureeMs / 1000;
                tvDuree.setText(String.format(Locale.FRANCE, "%d:%02d", sec / 60, sec % 60));
                btnLireVocal.setOnClickListener(c -> lireMessageVocal(m.audioBase64));
            } else if (m.photoBase64 != null && !m.photoBase64.isEmpty()) {
                try {
                    byte[] bytes = Base64.decode(m.photoBase64, Base64.NO_WRAP);
                    ivPhoto.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                    ivPhoto.setVisibility(View.VISIBLE);
                    tvTexte.setVisibility(View.GONE);
                    btnLireVocal.setVisibility(View.GONE);
                    tvDuree.setVisibility(View.GONE);
                } catch (Exception e) { /* ignoré */ }
            } else {
                tvTexte.setText(m.texte);
                tvTexte.setVisibility(View.VISIBLE);
                ivPhoto.setVisibility(View.GONE);
                btnLireVocal.setVisibility(View.GONE);
                tvDuree.setVisibility(View.GONE);
            }
            return v;
        }
    }
}
