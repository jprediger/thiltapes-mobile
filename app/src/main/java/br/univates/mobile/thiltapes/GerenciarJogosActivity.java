package br.univates.mobile.thiltapes;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.VolleyError;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Lista jogos ({@code GET /jogos}) para administradores abrirem edicao ou criacao.
 */
public class GerenciarJogosActivity extends AppCompatActivity {

    private ListView lista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gerenciar_jogos);
        ThiltapesBarraSistema.aplicarNaRaiz(this, R.id.root_gerenciar_jogos);

        lista = findViewById(R.id.list_gerenciar_jogos);
        FloatingActionButton fab = findViewById(R.id.fab_novo_jogo);
        fab.setOnClickListener(v -> {
            Intent i = new Intent(this, CriarEditarJogoActivity.class);
            i.putExtra(CriarEditarJogoActivity.EXTRA_JOGO_ID, -1);
            startActivity(i);
        });

        lista.setOnItemClickListener((p, v, pos, id) -> {
            ItemListaJogos item = (ItemListaJogos) lista.getAdapter().getItem(pos);
            Intent i = new Intent(this, CriarEditarJogoActivity.class);
            i.putExtra(CriarEditarJogoActivity.EXTRA_JOGO_ID, item.getId());
            startActivity(i);
        });

        carregar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        ThiltapesApi.getJogos(this, "dono=eu", this::preencher, this::aoErro);
    }

    private void preencher(JSONArray array) {
        ArrayList<ItemListaJogos> jogos = new ArrayList<>();
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                jogos.add(new ItemListaJogos(o.getInt("id"), o.getString("nome")));
            }
        } catch (JSONException e) {
            Toast.makeText(this, R.string.msg_erro_parse, Toast.LENGTH_SHORT).show();
            return;
        }
        lista.setAdapter(new ListaJogosAdapter(this, jogos));
    }

    private void aoErro(VolleyError e) {
        Toast.makeText(this, R.string.msg_erro_rede, Toast.LENGTH_LONG).show();
    }
}
