package br.univates.mobile.thiltapes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.VolleyError;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import br.univates.mobile.thiltapes.dto.InventarioLinha;

/**
 * Inventario global ou filtrado por {@code jogo_id} ({@code GET /inventario?jogo_id=}).
 */
public class InventarioActivity extends AppCompatActivity {

    public static final String EXTRA_FILTRO_JOGO_ID = "filtro_jogo_id";

    private RecyclerView recycler;
    private TextView vazio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventario);
        ThiltapesBarraSistema.aplicarNaRaiz(this, R.id.root_inventario);

        recycler = findViewById(R.id.recycler_inventario);
        vazio = findViewById(R.id.texto_inventario_vazio);

        Integer filtro = null;
        if (getIntent().hasExtra(EXTRA_FILTRO_JOGO_ID)) {
            int v = getIntent().getIntExtra(EXTRA_FILTRO_JOGO_ID, -1);
            if (v >= 0) {
                filtro = v;
            }
        }

        ThiltapesApi.getInventario(this, filtro, this::montarGrade, this::aoErro);
    }

    private void montarGrade(JSONArray array) {
        List<InventarioLinha> linhas = InventarioLinha.listFromJsonArrayLegacy(array);
        List<Integer> ids = new ArrayList<>();
        for (InventarioLinha l : linhas) {
            int tid = l.getThiltape().getId();
            if (tid >= 0 && !ids.contains(tid)) {
                ids.add(tid);
            }
        }

        List<ThiltapeProximo> itens = new ArrayList<>();
        for (int tid : ids) {
            itens.add(new ThiltapeProximo(
                    tid,
                    ThiltapesUrls.urlImagemThiltape(tid),
                    false,
                    0f,
                    true,
                    true,
                    false
            ));
        }

        if (itens.isEmpty()) {
            recycler.setVisibility(View.GONE);
            vazio.setVisibility(View.VISIBLE);
            return;
        }
        recycler.setVisibility(View.VISIBLE);
        vazio.setVisibility(View.GONE);

        recycler.setLayoutManager(new GridLayoutManager(this, 2));
        recycler.setAdapter(new InventarioGradeAdapter(this, itens));
    }

    private void aoErro(VolleyError e) {
        Toast.makeText(this, R.string.msg_erro_rede, Toast.LENGTH_LONG).show();
    }

    static final class InventarioGradeAdapter extends RecyclerView.Adapter<InventarioGradeAdapter.HolderMini> {

        private final InventarioActivity activity;
        private final List<ThiltapeProximo> itens;

        InventarioGradeAdapter(InventarioActivity activity, List<ThiltapeProximo> itens) {
            this.activity = activity;
            this.itens = itens;
        }

        @NonNull
        @Override
        public HolderMini onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_imagem_mapa, parent, false);
            return new HolderMini(v);
        }

        @Override
        public void onBindViewHolder(@NonNull HolderMini h, int position) {
            ThiltapeProximo item = itens.get(position);
            h.container.setBackgroundResource(0);
            ImagemMapaAdapter.carregarMiniaturaPublica(
                    activity,
                    h.itemView,
                    h.imagem,
                    item,
                    position,
                    item.chaveCacheLista(position)
            );
            h.imagem.setOnClickListener(v ->
                    ThiltapeFullscreenDialog.mostrar(activity, item, position)
            );
        }

        @Override
        public int getItemCount() {
            return itens.size();
        }

        static final class HolderMini extends RecyclerView.ViewHolder {
            final ImageView imagem;
            final View container;

            HolderMini(@NonNull View itemView) {
                super(itemView);
                imagem = itemView.findViewById(R.id.imagem_miniatura);
                container = itemView.findViewById(R.id.container_miniatura_thiltape);
            }
        }
    }
}
