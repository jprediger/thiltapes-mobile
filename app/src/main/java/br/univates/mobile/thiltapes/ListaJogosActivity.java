package br.univates.mobile.thiltapes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class ListaJogosActivity extends AppCompatActivity {

    private ListView lista = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_jogos);


        lista = (ListView) (findViewById(R.id.list_jogos));
        ArrayList<ItemListaJogos> listaJogos = new ArrayList<>();

        listaJogos.add(new ItemListaJogos("Nome do Jogo", "Criador", "ID_abcde")); //Teste!

        ListaJogosAdapter adapter = new ListaJogosAdapter(this, listaJogos);
        lista.setAdapter(adapter);

        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ItemListaJogos itemSelecionado = (ItemListaJogos) parent.getItemAtPosition(position);

                Intent intent = new Intent(ListaJogosActivity.this, MapaJogoActivity.class);
                intent.putExtra("id", itemSelecionado.getId());
                startActivity(intent);
            }
        });
    }
}