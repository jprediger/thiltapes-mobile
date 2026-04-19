package br.univates.mobile.thiltapes;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * LRU em memória para bitmaps já compostos (miniatura + overlay); limitado por bytes totais.
 */
public final class ThiltapesCacheBitmapLru {

    private static final android.util.LruCache<String, Bitmap> CACHE =
            new android.util.LruCache<String, Bitmap>(
                    (int) ThiltapesImagemConstantes.LIMITE_BYTES_TOTAL_CACHE_MEMORIA) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount();
                }
            };

    private ThiltapesCacheBitmapLru() {
    }

    @Nullable
    public static Bitmap obter(@Nullable String chave) {
        if (chave == null) {
            return null;
        }
        synchronized (CACHE) {
            return CACHE.get(chave);
        }
    }

    public static void colocar(@NonNull String chave, @NonNull Bitmap bitmap) {
        synchronized (CACHE) {
            CACHE.put(chave, bitmap);
        }
    }

    public static void limpar() {
        synchronized (CACHE) {
            CACHE.evictAll();
        }
    }
}
