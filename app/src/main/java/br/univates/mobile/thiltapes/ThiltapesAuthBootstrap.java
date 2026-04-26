package br.univates.mobile.thiltapes;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Garante que a sessao tenha um Bearer token antes do primeiro request autenticado.
 * Tenta {@code POST /register}; se {@code 409}, recupera via {@code POST /login}.
 */
public final class ThiltapesAuthBootstrap {

    private static final String TAG = ThiltapesAuthBootstrap.class.getSimpleName();
    private static final String NOME_INICIAL_PADRAO = "Jogador";

    private ThiltapesAuthBootstrap() {
    }

    public static void garantirToken(
            @NonNull Context ctx,
            @NonNull Runnable aoSucesso,
            @NonNull Response.ErrorListener aoErro) {
        ThiltapesSessao sessao = ThiltapesSessao.de(ctx);
        sessao.invalidarTokenSeBackendMudou();
        if (sessao.temToken()) {
            aoSucesso.run();
            return;
        }

        String androidId = sessao.obterAndroidId();
        String nomeLocal = sessao.obterNomeLocal();
        String nomeInicial = nomeLocal.isEmpty() ? NOME_INICIAL_PADRAO : nomeLocal;

        try {
            ThiltapesApi.postRegister(ctx, androidId, nomeInicial,
                    resp -> persistirTokenERodar(ctx, resp, aoSucesso, aoErro),
                    erroRegister -> tentarLoginAposConflito(ctx, androidId, erroRegister, aoSucesso, aoErro));
        } catch (JSONException e) {
            Log.w(TAG, "JSON register", e);
            aoErro.onErrorResponse(new VolleyError(e));
        }
    }

    private static void tentarLoginAposConflito(
            @NonNull Context ctx,
            @NonNull String androidId,
            @NonNull VolleyError erroRegister,
            @NonNull Runnable aoSucesso,
            @NonNull Response.ErrorListener aoErro) {
        NetworkResponse nr = erroRegister.networkResponse;
        if (nr == null || nr.statusCode != 409) {
            aoErro.onErrorResponse(erroRegister);
            return;
        }
        try {
            ThiltapesApi.postLogin(ctx, androidId,
                    resp -> persistirTokenERodar(ctx, resp, aoSucesso, aoErro),
                    aoErro);
        } catch (JSONException e) {
            Log.w(TAG, "JSON login", e);
            aoErro.onErrorResponse(new VolleyError(e));
        }
    }

    private static void persistirTokenERodar(
            @NonNull Context ctx,
            @NonNull JSONObject resposta,
            @NonNull Runnable aoSucesso,
            @NonNull Response.ErrorListener aoErro) {
        String token = resposta.optString("token", "");
        if (token.isEmpty()) {
            aoErro.onErrorResponse(new VolleyError("token ausente"));
            return;
        }
        ThiltapesSessao.de(ctx).salvarToken(token);
        aoSucesso.run();
    }
}
