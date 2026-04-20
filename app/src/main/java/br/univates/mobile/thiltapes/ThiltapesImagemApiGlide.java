package br.univates.mobile.thiltapes;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

/**
 * Carrega bytes de {@code GET /thiltapes/{id}/imagem} (Basic Auth) para preview no CRUD.
 * Sem recorte central nem efeitos de jogo; apenas teto de dimensao e escala proporcional.
 */
public final class ThiltapesImagemApiGlide {

    private ThiltapesImagemApiGlide() {
    }

    /**
     * Preenche um {@link ImageView} com a imagem do thiltape no servidor (ex.: edicao de jogo).
     *
     * @param linhaEdicao linha do formulario; se o usuario ja trocou a foto ({@code tag_thiltape_img_origem}
     *                    false), o resultado assincrono da rede nao sobrescreve a miniatura local.
     */
    public static void carregarImagemThiltape(
            @NonNull ImageView imageView,
            int thiltapeId,
            @NonNull View linhaEdicao) {
        Glide.with(imageView).clear(imageView);
        String url = ThiltapesUrls.urlImagemThiltape(thiltapeId);
        LazyHeaders headers = new LazyHeaders.Builder()
                .addHeader("Authorization", ThiltapesSessao.de(imageView.getContext()).obterCabeçalhoAuthorization())
                .build();
        RequestOptions opts = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(
                        ThiltapesImagemConstantes.LIMITE_DIMENSAO_MAXIMA_POR_LADO_PX,
                        ThiltapesImagemConstantes.LIMITE_DIMENSAO_MAXIMA_POR_LADO_PX)
                .fitCenter()
                .placeholder(new ColorDrawable(0xFFEEEEEE));
        Glide.with(imageView)
                .load(new GlideUrl(url, headers))
                .apply(opts)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                            @NonNull Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model,
                            @NonNull Target<Drawable> target, @NonNull DataSource dataSource,
                            boolean isFirstResource) {
                        if (!Boolean.TRUE.equals(linhaEdicao.getTag(R.id.tag_thiltape_img_origem))) {
                            return true;
                        }
                        return false;
                    }
                })
                .into(imageView);
    }
}
