package br.univates.mobile.thiltapes;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;

/**
 * Reinicia caches de imagem ao retornar ao menu principal: LRU de bitmaps compostos e cache Glide.
 */
public final class ThiltapesCachesImagem {

    private ThiltapesCachesImagem() {
    }

    /**
     * Deve ser chamado na UI thread; disco do Glide e limpo em thread separada.
     */
    public static void limpar(@NonNull Context context) {
        ThiltapesCacheBitmapLru.limpar();
        Context app = context.getApplicationContext();
        Glide.get(app).clearMemory();
        new Thread(() -> Glide.get(app).clearDiskCache(), "thiltapes-glide-disk-clear").start();
    }
}
