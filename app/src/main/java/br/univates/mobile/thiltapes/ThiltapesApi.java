package br.univates.mobile.thiltapes;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Cliente REST Volley alinhado ao backend Go (Bearer token UUID emitido por {@code POST /register}).
 */
public final class ThiltapesApi {

    private ThiltapesApi() {
    }

    private static RequestQueue fila(@NonNull Context contexto) {
        return ((ThiltapesApplication) contexto.getApplicationContext()).obterFilaVolley();
    }

    private static void enfileirar(@NonNull Context contexto, @NonNull Request<?> req) {
        req.setShouldCache(false);
        fila(contexto).add(req);
    }

    @NonNull
    private static Map<String, String> cabeçalhosAuth(@NonNull Context contexto) {
        Map<String, String> h = new HashMap<>();
        h.put("Authorization", ThiltapesSessao.de(contexto).obterCabeçalhoAuthorization());
        h.put("Accept", "application/json");
        return h;
    }

    public static void postRegister(
            @NonNull Context ctx,
            @NonNull String androidId,
            @NonNull String name,
            @NonNull Response.Listener<JSONObject> ok,
            @NonNull Response.ErrorListener err) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("android_id", androidId);
        body.put("name", name);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                ThiltapesUrls.caminho("register"),
                body,
                ok,
                err
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Content-Type", "application/json");
                h.put("Accept", "application/json");
                return h;
            }
        };
        enfileirar(ctx, req);
    }

    public static void postLogin(
            @NonNull Context ctx,
            @NonNull String androidId,
            @NonNull Response.Listener<JSONObject> ok,
            @NonNull Response.ErrorListener err) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("android_id", androidId);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                ThiltapesUrls.caminho("login"),
                body,
                ok,
                err
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Content-Type", "application/json");
                h.put("Accept", "application/json");
                return h;
            }
        };
        enfileirar(ctx, req);
    }

    public static void getMe(
            @NonNull Context ctx,
            @NonNull Response.Listener<JSONObject> ok,
            @NonNull Response.ErrorListener err) {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                ThiltapesUrls.caminho("me"),
                null,
                ok,
                err
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return cabeçalhosAuth(ctx);
            }
        };
        enfileirar(ctx, req);
    }

    public static void patchNome(
            @NonNull Context ctx,
            @NonNull String nome,
            @NonNull Response.Listener<JSONObject> ok,
            @NonNull Response.ErrorListener err) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("name", nome);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.PATCH,
                ThiltapesUrls.caminho("me"),
                body,
                ok,
                err
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = cabeçalhosAuth(ctx);
                h.put("Content-Type", "application/json");
                return h;
            }
        };
        enfileirar(ctx, req);
    }

    /**
     * @param consultaQuery opcional, ex.: {@code status=active} para filtrar por status.
     */
    public static void getJogos(
            @NonNull Context ctx,
            @Nullable String consultaQuery,
            @NonNull Response.Listener<org.json.JSONArray> ok,
            @NonNull Response.ErrorListener err) {
        String url = ThiltapesUrls.caminho("jogos");
        if (consultaQuery != null && !consultaQuery.isEmpty()) {
            url = url + "?" + consultaQuery;
        }
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                ok,
                err
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return cabeçalhosAuth(ctx);
            }
        };
        enfileirar(ctx, req);
    }

    public static void postJogo(
            @NonNull Context ctx,
            @NonNull String nome,
            @NonNull Response.Listener<JSONObject> ok,
            @NonNull Response.ErrorListener err) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("name", nome);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                ThiltapesUrls.caminho("jogos"),
                body,
                ok,
                err
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = cabeçalhosAuth(ctx);
                h.put("Content-Type", "application/json");
                return h;
            }
        };
        enfileirar(ctx, req);
    }

    public static void getJogo(
            @NonNull Context ctx,
            int jogoId,
            @NonNull Response.Listener<JSONObject> ok,
            @NonNull Response.ErrorListener err) {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                ThiltapesUrls.caminho("jogos", String.valueOf(jogoId)),
                null,
                ok,
                err
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return cabeçalhosAuth(ctx);
            }
        };
        enfileirar(ctx, req);
    }

    public static void putJogo(
            @NonNull Context ctx,
            int jogoId,
            @NonNull String nome,
            @NonNull Response.Listener<JSONObject> ok,
            @NonNull Response.ErrorListener err) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("name", nome);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.PUT,
                ThiltapesUrls.caminho("jogos", String.valueOf(jogoId)),
                body,
                ok,
                err
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = cabeçalhosAuth(ctx);
                h.put("Content-Type", "application/json");
                return h;
            }
        };
        enfileirar(ctx, req);
    }

    public static void deleteJogo(
            @NonNull Context ctx,
            int jogoId,
            @NonNull Response.Listener<String> ok,
            @NonNull Response.ErrorListener err) {
        StringRequest req = new StringRequest(
                Request.Method.DELETE,
                ThiltapesUrls.caminho("jogos", String.valueOf(jogoId)),
                ok,
                err
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return cabeçalhosAuth(ctx);
            }
        };
        enfileirar(ctx, req);
    }

    public static void listarThiltapesDoJogo(
            @NonNull Context ctx,
            int jogoId,
            @NonNull Response.Listener<org.json.JSONArray> ok,
            @NonNull Response.ErrorListener err) {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                ThiltapesUrls.caminho("jogos", String.valueOf(jogoId), "thiltapes"),
                null,
                ok,
                err
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return cabeçalhosAuth(ctx);
            }
        };
        enfileirar(ctx, req);
    }

    public static void criarThiltapeNoJogo(
            @NonNull Context ctx,
            int jogoId,
            @NonNull JSONObject corpo,
            @NonNull Response.Listener<JSONObject> ok,
            @NonNull Response.ErrorListener err) {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                ThiltapesUrls.caminho("jogos", String.valueOf(jogoId), "thiltapes"),
                corpo,
                ok,
                err
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = cabeçalhosAuth(ctx);
                h.put("Content-Type", "application/json");
                return h;
            }
        };
        enfileirar(ctx, req);
    }

    public static void putThiltape(
            @NonNull Context ctx,
            int thiltapeId,
            @NonNull JSONObject corpo,
            @NonNull Response.Listener<JSONObject> ok,
            @NonNull Response.ErrorListener err) {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.PUT,
                ThiltapesUrls.caminho("thiltapes", String.valueOf(thiltapeId)),
                corpo,
                ok,
                err
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = cabeçalhosAuth(ctx);
                h.put("Content-Type", "application/json");
                return h;
            }
        };
        enfileirar(ctx, req);
    }

    public static void deleteThiltape(
            @NonNull Context ctx,
            int thiltapeId,
            @NonNull Response.Listener<String> ok,
            @NonNull Response.ErrorListener err) {
        StringRequest req = new StringRequest(
                Request.Method.DELETE,
                ThiltapesUrls.caminho("thiltapes", String.valueOf(thiltapeId)),
                ok,
                err
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return cabeçalhosAuth(ctx);
            }
        };
        enfileirar(ctx, req);
    }

    public static void getScan(
            @NonNull Context ctx,
            int jogoId,
            double lat,
            double lng,
            @NonNull Response.Listener<JSONObject> ok,
            @NonNull Response.ErrorListener err) {
        String url = ThiltapesUrls.caminho("jogos", String.valueOf(jogoId), "scan")
                + "?lat=" + String.format(Locale.US, "%.6f", lat)
                + "&lng=" + String.format(Locale.US, "%.6f", lng);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                ok,
                err
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return cabeçalhosAuth(ctx);
            }
        };
        enfileirar(ctx, req);
    }

    /**
     * {@code POST /jogos/:jogoId/thiltapes/:thiltapeId/capturar} com body {@code {lat, lng}}.
     * Sucesso: 200 com {@code ThiltapeResponse}. Erros relevantes: 422 fora de alcance, 409 ja capturado.
     */
    public static void postCapturar(
            @NonNull Context ctx,
            int jogoId,
            int thiltapeId,
            double lat,
            double lng,
            @NonNull Response.Listener<JSONObject> ok,
            @NonNull Response.ErrorListener err) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("lat", lat);
        body.put("lng", lng);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                ThiltapesUrls.caminho("jogos", String.valueOf(jogoId), "thiltapes", String.valueOf(thiltapeId), "capturar"),
                body,
                ok,
                err
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = cabeçalhosAuth(ctx);
                h.put("Content-Type", "application/json");
                return h;
            }
        };
        enfileirar(ctx, req);
    }

    public static void getInventario(
            @NonNull Context ctx,
            @Nullable Integer filtroJogoId,
            @NonNull Response.Listener<org.json.JSONArray> ok,
            @NonNull Response.ErrorListener err) {
        String url = ThiltapesUrls.caminho("inventario");
        if (filtroJogoId != null) {
            url = url + "?jogoId=" + filtroJogoId;
        }
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                ok,
                err
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return cabeçalhosAuth(ctx);
            }
        };
        enfileirar(ctx, req);
    }
}
