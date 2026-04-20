package br.univates.mobile.thiltapes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Lista jogos ({@code GET /jogos}) para iniciar o mapa com o jogo escolhido.
 */
public class ListaJogosActivity extends AppCompatActivity {

    private ListView lista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_jogos);
        ThiltapesBarraSistema.aplicarNaRaiz(this, R.id.root_lista_jogos);

        lista = findViewById(R.id.list_jogos);
        lista.setOnItemClickListener(this::aoClicarItem);

        carregarJogos();
    }

    private void aoClicarItem(AdapterView<?> parent, View view, int position, long id) {
        ItemListaJogos item = (ItemListaJogos) parent.getItemAtPosition(position);
        Intent intent = new Intent(this, MapaJogoActivity.class);
        intent.putExtra(MapaJogoActivity.EXTRA_JOGO_ID, item.getId());
        startActivity(intent);
    }

    private void carregarJogos() {
        ThiltapesApi.getJogos(this, null,
                this::preencherLista,
                this::aoErroVolley
        );
    }

    private void preencherLista(JSONArray array) {
        ArrayList<ItemListaJogos> jogos = new ArrayList<>();
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                int jid = o.optInt("id", -1);
                String nome = o.optString("nome", "?");
                if (jid >= 0) {
                    jogos.add(new ItemListaJogos(jid, nome));
                }
            }
        } catch (JSONException e) {
            Toast.makeText(this, R.string.msg_erro_parse, Toast.LENGTH_SHORT).show();
            return;
        }
        lista.setAdapter(new ListaJogosAdapter(this, jogos));
    }

    private void aoErroVolley(VolleyError error) {
        Toast.makeText(this, R.string.msg_erro_rede, Toast.LENGTH_LONG).show();
    }
}
