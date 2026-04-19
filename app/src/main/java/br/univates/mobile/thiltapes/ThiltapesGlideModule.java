package br.univates.mobile.thiltapes;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.module.AppGlideModule;

/**
 * Limita o cache em memória do Glide conforme {@link ThiltapesImagemConstantes}; reduz picos de RAM.
 */
@GlideModule
public final class ThiltapesGlideModule extends AppGlideModule {

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        MemorySizeCalculator calculadora = new MemorySizeCalculator.Builder(context).build();
        long padrao = calculadora.getMemoryCacheSize();
        long teto = ThiltapesImagemConstantes.LIMITE_BYTES_TOTAL_CACHE_MEMORIA_GLIDE;
        long memoriaBytes = Math.min(padrao, teto);
        builder.setMemoryCache(new LruResourceCache(memoriaBytes));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
