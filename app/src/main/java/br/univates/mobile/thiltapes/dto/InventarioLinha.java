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
 * Um item de {@code GET /inventario}: o backend Go retorna {@code []ThiltapeResponse} direto,
 * cada elemento e um {@link ThiltapePublico}.
 */
public final class InventarioLinha {

    @NonNull
    private final ThiltapePublico thiltape;

    public InventarioLinha(@NonNull ThiltapePublico thiltape) {
        this.thiltape = thiltape;
    }

    @NonNull
    public static Optional<InventarioLinha> fromJson(@Nullable JsonObject json) {
        if (json == null) {
            return Optional.empty();
        }
        return ThiltapePublico.fromJson(json).map(InventarioLinha::new);
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

    @NonNull
    public ThiltapePublico getThiltape() {
        return thiltape;
    }
}
