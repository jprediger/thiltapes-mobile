package br.univates.mobile.thiltapes;

import android.graphics.Bitmap;
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.List;
import java.util.Objects;

/**
 * Grade de miniaturas: carrega URL ou Base64, aplica limite de tamanho, saturação e overlay por distância.
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

    private static void carregarMiniatura(
            @NonNull View itemView,
            @NonNull ImageView imageView,
            @NonNull ThiltapeProximo item,
            int position,
            @NonNull String chaveCache) {

        // centerCrop quadrado: mesma região recortada que em ThiltapeFullscreenDialog + GeradorOverlayThiltape.
        RequestOptions opts = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(true)
                .override(
                        ThiltapesImagemConstantes.CARREGAMENTO_THILTAPE_LADO_PX,
                        ThiltapesImagemConstantes.CARREGAMENTO_THILTAPE_LADO_PX)
                .centerCrop();

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
        rb.apply(opts).into(new CustomTarget<Bitmap>(
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
                Bitmap composto = GeradorOverlayThiltape.criarMiniaturaComposta(
                        resource,
                        item.getDistanciaMetros());
                ThiltapesCacheBitmapLru.colocar(chaveCache, composto);
                imageView.setImageBitmap(composto);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        });
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView imagem;

        Holder(@NonNull View itemView) {
            super(itemView);
            imagem = itemView.findViewById(R.id.imagem_miniatura);
        }
    }
}
