package br.univates.mobile.thiltapes.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Um elemento de {@code proximos} ou {@code capturados} em {@code GET /jogos/:jogoId/scan}.
 * O backend Go inlinea o {@code ThiltapeResponse} no proprio objeto e adiciona {@code distance_metros}
 * apenas em {@code proximos} (capturados nao tem distance — fica {@link Double#NaN}).
 */
public final class ScanItem {

    @NonNull
    private final ThiltapePublico thiltape;
    private final double distanciaMetros;

    public ScanItem(@NonNull ThiltapePublico thiltape, double distanciaMetros) {
        this.thiltape = thiltape;
        this.distanciaMetros = distanciaMetros;
    }

    @NonNull
    public static Optional<ScanItem> fromJson(@Nullable JsonObject json) {
        if (json == null) {
            return Optional.empty();
        }
        return ThiltapePublico.fromJson(json).map(t -> {
            double dist = Double.NaN;
            if (json.has("distance_metros") && !json.get("distance_metros").isJsonNull()) {
                try {
                    dist = json.get("distance_metros").getAsDouble();
                } catch (RuntimeException ignored) {
                    dist = Double.NaN;
                }
            }
            return new ScanItem(t, dist);
        });
    }

    @NonNull
    public static List<ScanItem> listFromJsonArray(@Nullable JsonArray array) {
        List<ScanItem> lista = new ArrayList<>();
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

    @NonNull
    public ThiltapePublico getThiltape() {
        return thiltape;
    }

    public double getDistanciaMetros() {
        return distanciaMetros;
    }
}
