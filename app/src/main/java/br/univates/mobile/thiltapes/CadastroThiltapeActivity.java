package br.univates.mobile.thiltapes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class CadastroThiltapeActivity extends AppCompatActivity implements LocationListener {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    public static ArrayList<String> listaThiltapes = new ArrayList<>();

    private ImageView ivPreview;
    private TextView tvLocalizacao;
    private EditText etNome, etPontos;
    private LocationManager locationManager;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private boolean fotoTirada = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_thiltape);

        ivPreview = findViewById(R.id.iv_preview);
        tvLocalizacao = findViewById(R.id.tv_localizacao);
        etNome = findViewById(R.id.et_nome_thiltape);
        etPontos = findViewById(R.id.et_pontuacao_thiltape);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        findViewById(R.id.btn_capturar_foto).setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        });

        findViewById(R.id.btn_atualizar_gps).setOnClickListener(v -> buscarGps());

        findViewById(R.id.btn_salvar_thiltape).setOnClickListener(v -> {
            String nome = etNome.getText().toString().trim();
            String pontos = etPontos.getText().toString().trim();

            if (nome.isEmpty() || pontos.isEmpty()) {
                Toast.makeText(this, "Preencha nome e pontuação!", Toast.LENGTH_SHORT).show();
            } else if (!fotoTirada) {
                Toast.makeText(this, "A foto é obrigatória!", Toast.LENGTH_SHORT).show();
            } else {
                String registro = "Nome: " + nome + " | Pts: " + pontos + " | Lat: " + String.format("%.4f", latitude);
                listaThiltapes.add(registro);
                Toast.makeText(this, "Sucesso: " + nome + " cadastrado!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        checarPermissoes();
    }

    private void checarPermissoes() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA}, 100);
        } else {
            buscarGps();
        }
    }

    @SuppressLint("MissingPermission")
    private void buscarGps() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1, this);
            tvLocalizacao.setText("Buscando localização...");
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        tvLocalizacao.setText(String.format("Localização: %.5f, %.5f", latitude, longitude));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            ivPreview.setImageBitmap(imageBitmap);
            fotoTirada = true; // Valida o requisito de foto obrigatória
            buscarGps(); // Atualiza GPS automaticamente após a foto
        }
    }
}