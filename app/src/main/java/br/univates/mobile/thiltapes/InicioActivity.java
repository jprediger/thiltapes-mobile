package br.univates.mobile.thiltapes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tela inicial (design da antiga selecao de modo): nome de usuario, botoes; {@code GET/PATCH /me}.
 */
public class InicioActivity extends AppCompatActivity {

    private EditText etNomeUsuario;
    private Button btnEscolherJogo;
    private Button btnInventario;
    private Button btnGerenciar;

    private String ultimoNomeEnviado = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);
        ThiltapesBarraSistema.aplicarNaRaiz(this, R.id.activity_inicio);

        if (!BuildConfig.API_BASE_URL_DEFINIDA_EM_LOCAL_PROPERTIES) {
            Toast.makeText(
                    this,
                    R.string.msg_api_url_padrao,
                    Toast.LENGTH_LONG
            ).show();
        }

        etNomeUsuario = findViewById(R.id.et_nome_usuario);
        btnEscolherJogo = findViewById(R.id.btn_abrir_mapa);
        btnInventario = findViewById(R.id.btn_inventario);
        btnGerenciar = findViewById(R.id.btn_gerenciar_jogos);

        ThiltapesSessao sessao = ThiltapesSessao.de(this);
        etNomeUsuario.setText(sessao.obterNomeLocal());
        ultimoNomeEnviado = sessao.obterNomeLocal();

        btnEscolherJogo.setOnClickListener(v ->
                navegarAposSincronizarNome(new Intent(this, ListaJogosActivity.class)));
        btnInventario.setOnClickListener(v ->
                navegarAposSincronizarNome(new Intent(this, InventarioActivity.class)));
        btnGerenciar.setOnClickListener(v ->
                navegarAposSincronizarNome(new Intent(this, GerenciarJogosActivity.class)));

        btnGerenciar.setVisibility(sessao.obterEhAdminCache() ? View.VISIBLE : View.GONE);

        sincronizarPerfilServidor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThiltapesCachesImagem.limpar(this);
        sincronizarPerfilServidor();
    }

    /**
     * Envia nome novo ao backend (se mudou) e navega apos sucesso; se igual, so navega.
     */
    private void navegarAposSincronizarNome(@NonNull Intent destino) {
        String nome = etNomeUsuario.getText().toString().trim();
        if (nome.equals(ultimoNomeEnviado)) {
            startActivity(destino);
            return;
        }
        ThiltapesSessao.de(this).salvarNomeLocal(nome);
        if (nome.isEmpty()) {
            ultimoNomeEnviado = nome;
            startActivity(destino);
            return;
        }
        try {
            ThiltapesNomeSync.patchNome(this, nome, () -> {
                ultimoNomeEnviado = nome;
                sincronizarPerfilServidor();
                startActivity(destino);
            }, e -> Toast.makeText(InicioActivity.this, R.string.msg_erro_rede, Toast.LENGTH_SHORT).show());
        } catch (JSONException e) {
            Toast.makeText(this, R.string.msg_erro_parse, Toast.LENGTH_SHORT).show();
        }
    }

    private void sincronizarPerfilServidor() {
        ThiltapesApi.getMe(this, resposta -> {
            try {
                boolean admin = resposta.optBoolean("eh_admin", false);
                ThiltapesSessao.de(InicioActivity.this).salvarEhAdmin(admin);
                btnGerenciar.setVisibility(admin ? View.VISIBLE : View.GONE);
                if (resposta.has("nome") && !resposta.isNull("nome")) {
                    String nome = resposta.getString("nome");
                    etNomeUsuario.setText(nome);
                    ThiltapesSessao.de(InicioActivity.this).salvarNomeLocal(nome);
                    ultimoNomeEnviado = nome;
                }
            } catch (JSONException e) {
                Toast.makeText(this, R.string.msg_erro_parse, Toast.LENGTH_SHORT).show();
            }
        }, this::aoErroVolleyIgnoravel);
    }

    private void aoErroVolleyIgnoravel(VolleyError error) {
        // Perfil opcional na entrada; falha de rede nao bloqueia a UI.
    }
}
