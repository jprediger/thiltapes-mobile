package br.univates.mobile.thiltapes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class ListaJogosAdapter extends ArrayAdapter<ItemListaJogos> {

    public ListaJogosAdapter(Context context, List<ItemListaJogos> itens) {
        super(context, 0, itens);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_lista_jogos, parent, false);
        }

        ItemListaJogos itemAtual = getItem(position);
        TextView txtTitulo = convertView.findViewById(R.id.txt_titulo_lista);
        TextView txtDescricao = convertView.findViewById(R.id.txt_descricao_lista);

        if (itemAtual != null) {
            txtTitulo.setText(itemAtual.getNome());
            txtDescricao.setText(itemAtual.getDescricao());
        }

        return convertView;
    }
}
