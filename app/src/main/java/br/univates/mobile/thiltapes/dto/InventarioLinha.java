package br.univates.mobile.thiltapes.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Um item de {@code GET /inventario}: {@code encontrado_em} + objeto {@code thiltape} publico.
 */
public final class InventarioLinha {

    @Nullable
    private final String encontradoEm;
    @NonNull
    private final ThiltapePublico thiltape;

    public InventarioLinha(@Nullable String encontradoEm, @NonNull ThiltapePublico thiltape) {
        this.encontradoEm = encontradoEm;
        this.thiltape = thiltape;
    }

    @NonNull
    public static Optional<InventarioLinha> fromJson(@Nullable JsonObject json) {
        if (json == null) {
            return Optional.empty();
        }
        JsonElement th = json.get("thiltape");
        if (th == null || th.isJsonNull() || !th.isJsonObject()) {
            return Optional.empty();
        }
        String encontrado = null;
        if (json.has("encontrado_em") && !json.get("encontrado_em").isJsonNull()) {
            encontrado = json.get("encontrado_em").getAsString();
        }
        final String encontradoFinal = encontrado;
        return ThiltapePublico.fromJson(th.getAsJsonObject())
                .map(t -> new InventarioLinha(encontradoFinal, t));
    }

    @NonNull
    public static List<InventarioLinha> listFromJsonArray(@Nullable JsonArray array) {
        List<InventarioLinha> lista = new ArrayList<>();
        if (array == null) {
            return lista;
        }
        for (JsonElement e : array) {
            if (e != null && e.isJsonObject()) {
                fromJson(e.getAsJsonObject()).ifPresent(lista::add);
            }
        }
        return lista;
    }

    /**
     * Conveniencia para respostas Volley/org.json.
     */
    @NonNull
    public static List<InventarioLinha> listFromJsonArrayLegacy(@NonNull JSONArray legacy) {
        return listFromJsonArray(JsonParser.parseString(legacy.toString()).getAsJsonArray());
    }

    /**
     * Soma {@link ThiltapePublico#getPontuacao()} contando cada {@code id} no maximo uma vez.
     */
    public static int somarPontuacaoPorThiltapeUnico(@NonNull List<InventarioLinha> linhas) {
        int total = 0;
        Set<Integer> vistos = new HashSet<>();
        for (InventarioLinha l : linhas) {
            ThiltapePublico t = l.getThiltape();
            if (vistos.contains(t.getId())) {
                continue;
            }
            vistos.add(t.getId());
            total += t.getPontuacao();
        }
        return total;
    }

    @Nullable
    public String getEncontradoEm() {
        return encontradoEm;
    }

    @NonNull
    public ThiltapePublico getThiltape() {
        return thiltape;
    }
}
