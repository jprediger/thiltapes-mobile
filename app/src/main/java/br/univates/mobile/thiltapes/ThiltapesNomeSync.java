package br.univates.mobile.thiltapes;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.volley.VolleyError;

import org.json.JSONException;

/**
 * Envia {@code PATCH /me} para alinhar o nome ao sair do campo.
 */
public final class ThiltapesNomeSync {

    private ThiltapesNomeSync() {
    }

    public static void patchNome(
            @NonNull Context ctx,
            @NonNull String nome,
            @NonNull Runnable aoOk,
            @NonNull Erro erro
    ) throws JSONException {
        ThiltapesApi.patchNome(ctx, nome,
                res -> aoOk.run(),
                e -> erro.onErro(e)
        );
    }

    public interface Erro {
        void onErro(VolleyError e);
    }
}
