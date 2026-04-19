package br.univates.mobile.thiltapes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SelecaoModoActivity extends AppCompatActivity {

    private Button btnJogar, btnCriar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selecao_modo);

        btnJogar = (Button) (findViewById(R.id.btn_jogar));
        btnCriar = (Button) (findViewById(R.id.btn_criar));

        btnJogar.setOnClickListener(v -> startActivity(new Intent(this, ListaJogosActivity.class)));
        btnCriar.setOnClickListener(v -> startActivity(new Intent(this, InicioActivity.class)));
    }
}