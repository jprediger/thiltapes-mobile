package br.univates.mobile.thiltapes;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

/**
 * Opcoes de decode Glide: itens desbloqueados (inventario, thiltape ja coletado no mapa) apenas
 * escalam proporcionalmente ao teto; bloqueados usam centerCrop para alinhar com overlay de proximidade.
 */
public final class ThiltapesGlideOpcoesCarregamento {

    private ThiltapesGlideOpcoesCarregamento() {
    }

    @NonNull
    public static RequestOptions opcoesListaOuFullscreen(@NonNull ThiltapeProximo item) {
        RequestOptions base = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(true)
                .override(
                        ThiltapesImagemConstantes.CARREGAMENTO_THILTAPE_LADO_PX,
                        ThiltapesImagemConstantes.CARREGAMENTO_THILTAPE_LADO_PX);
        if (item.isDesbloqueado()) {
            return base.fitCenter();
        }
        return base.centerCrop();
    }
}
