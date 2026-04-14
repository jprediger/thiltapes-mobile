package br.univates.mobile.thiltapes;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Tela inicial do app: gerencia identificação do usuário e navegação.
 */
public class InicioActivity extends AppCompatActivity {

    private EditText etNomeUsuario;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Força o modo claro para evitar problemas de visibilidade
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_inicio);

        // Configuração para preencher o espaço da barra de status (Edge-to-Edge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_inicio), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Inicializar componentes
        etNomeUsuario = findViewById(R.id.et_nome_usuario);
        Button btnMapa = findViewById(R.id.btn_abrir_mapa);
        Button btnCadastro = findViewById(R.id.btn_abrir_inventario); // Usando o ID do botão de inventário para o cadastro

        // 2. Configurar SharedPreferences (Persistência local)
        prefs = getSharedPreferences("Thiltapes_Prefs", Context.MODE_PRIVATE);

        // Autopreencher o nome se já foi salvo anteriormente
        String nomeSalvo = prefs.getString("user_name", "");
        if (etNomeUsuario != null) {
            etNomeUsuario.setText(nomeSalvo);
        }

        // 3. Capturar o ANDROID_ID (Identificador único do dispositivo)
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // 4. Clique para abrir o MAPA (Fluxo do Jogador)
        btnMapa.setOnClickListener(v -> {
            String nome = etNomeUsuario.getText().toString().trim();
            if (!nome.isEmpty()) {
                salvarNome(nome);

                Intent intent = new Intent(this, MapaJogoActivity.class);
                intent.putExtra("NOME_USUARIO", nome);
                intent.putExtra("ANDROID_ID", androidId);
                startActivity(intent);
            } else {
                etNomeUsuario.setError("Por favor, digite seu nome");
            }
        });

        // 5. Clique para abrir o CADASTRO (Sua implementação)
        btnCadastro.setOnClickListener(v -> {
            String nome = etNomeUsuario.getText().toString().trim();
            salvarNome(nome); // Salva mesmo se abrir o cadastro

            Intent intent = new Intent(this, CadastroThiltapeActivity.class);
            intent.putExtra("ANDROID_ID", androidId);
            startActivity(intent);
        });
    }

    private void salvarNome(String nome) {
        if (!nome.isEmpty()) {
            prefs.edit().putString("user_name", nome).apply();
        }
    }
}