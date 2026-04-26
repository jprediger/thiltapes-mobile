package br.univates.mobile.thiltapes;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.List;
import java.util.Objects;

/**
 * Grade de miniaturas: no mapa, itens ainda bloqueados recebem overlay por distancia; desbloqueados e
 * inventario sem crop nem efeitos (apenas escala ao teto em {@link ThiltapesGlideOpcoesCarregamento}).
 */
public final class ImagemMapaAdapter extends RecyclerView.Adapter<ImagemMapaAdapter.Holder> {

    public interface OnThiltapeClickListener {
        void onThiltapeClicado(@NonNull ThiltapeProximo item, int posicao);
    }

    private final List<ThiltapeProximo> itens;
    private final OnThiltapeClickListener listener;

    public ImagemMapaAdapter(
            @NonNull List<ThiltapeProximo> itens,
            @NonNull OnThiltapeClickListener listener) {
        this.itens = itens;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_imagem_mapa, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ThiltapeProximo item = itens.get(position);
        ImageView iv = holder.imagem;
        View container = holder.container;

        int bordaRes;
        if (item.getDistanciaMetros() <= ThiltapesImagemConstantes.DISTANCIA_BORDA_PROXIMA_METROS) {
            bordaRes = R.drawable.thiltape_outline_verde;
        } else if (item.getDistanciaMetros() <= ThiltapesImagemConstantes.DISTANCIA_BORDA_MEDIA_METROS) {
            bordaRes = R.drawable.thiltape_outline_ambar;
        } else {
            bordaRes = R.drawable.thiltape_outline_vermelho;
        }
        container.setBackgroundResource(bordaRes);

        Glide.with(holder.itemView).clear(iv);
        iv.setImageDrawable(new ColorDrawable(0xFF333333));
        iv.setTag(R.id.tag_bind_posicao_thiltape, position);

        String chave = item.chaveCacheLista(position);
        Bitmap emCache = ThiltapesCacheBitmapLru.obter(chave);
        if (emCache != null && !emCache.isRecycled()) {
            iv.setImageBitmap(emCache);
        } else {
            carregarMiniatura(holder.itemView, iv, item, position, chave);
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onThiltapeClicado(itens.get(pos), pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    @Override
    public void onViewRecycled(@NonNull Holder holder) {
        super.onViewRecycled(holder);
        Glide.with(holder.itemView).clear(holder.imagem);
    }

    /**
     * Permite reutilizar o carregamento (ex.: inventario em outro RecyclerView).
     */
    public static void carregarMiniaturaPublica(
            @NonNull View itemView,
            @NonNull ImageView imageView,
            @NonNull ThiltapeProximo item,
            int position,
            @NonNull String chaveCache) {
        carregarMiniatura(itemView, imageView, item, position, chaveCache);
    }

    private static void carregarMiniatura(
            @NonNull View itemView,
            @NonNull ImageView imageView,
            @NonNull ThiltapeProximo item,
            int position,
            @NonNull String chaveCache) {

        imageView.setTag(R.id.tag_bind_posicao_thiltape, position);

        RequestBuilder<Bitmap> rb = Glide.with(itemView).asBitmap();
        if (item.isEhBase64()) {
            if (ThiltapesBase64Util.excedeLimiteArquivo(item.getFonte())) {
                return;
            }
            try {
                byte[] bytes = ThiltapesBase64Util.decodificar(item.getFonte());
                rb = rb.load(bytes);
            } catch (IllegalArgumentException ignored) {
                return;
            }
        } else {
            rb = rb.load(item.getFonte());
        }

        final int posicaoEsperada = position;
        rb.apply(ThiltapesGlideOpcoesCarregamento.opcoesListaOuFullscreen(item)).into(new CustomTarget<Bitmap>(
                ThiltapesImagemConstantes.CARREGAMENTO_THILTAPE_LADO_PX,
                ThiltapesImagemConstantes.CARREGAMENTO_THILTAPE_LADO_PX) {
            @Override
            public void onResourceReady(
                    @NonNull Bitmap resource,
                    @Nullable Transition<? super Bitmap> transition) {
                Object tagAtual = imageView.getTag(R.id.tag_bind_posicao_thiltape);
                if (!Objects.equals(tagAtual, posicaoEsperada)) {
                    return;
                }
                if (resource.getWidth() > ThiltapesImagemConstantes.LIMITE_DIMENSAO_MAXIMA_POR_LADO_PX
                        || resource.getHeight() > ThiltapesImagemConstantes.LIMITE_DIMENSAO_MAXIMA_POR_LADO_PX) {
                    return;
                }
                Bitmap composto = montarMiniatura(resource, item);
                ThiltapesCacheBitmapLru.colocar(chaveCache, composto);
                imageView.setImageBitmap(composto);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        });
    }

    @NonNull
    private static Bitmap montarMiniatura(@NonNull Bitmap resource, @NonNull ThiltapeProximo item) {
        if (item.isDesbloqueado()) {
            Bitmap copia = Bitmap.createBitmap(
                    resource.getWidth(),
                    resource.getHeight(),
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(copia);
            canvas.drawBitmap(resource, 0f, 0f, null);
            return copia;
        }
        return GeradorOverlayThiltape.criarMiniaturaComposta(resource, item.getDistanciaMetros());
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView imagem;
        final View container;

        Holder(@NonNull View itemView) {
            super(itemView);
            imagem = itemView.findViewById(R.id.imagem_miniatura);
            container = itemView.findViewById(R.id.container_miniatura_thiltape);
        }
    }
}
