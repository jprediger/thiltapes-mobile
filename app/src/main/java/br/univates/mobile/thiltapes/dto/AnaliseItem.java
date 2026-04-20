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
 * Um elemento de {@code proximos} ou {@code desbloqueados} em {@code POST /analise}.
 */
public final class AnaliseItem {

    @NonNull
    private final ThiltapePublico thiltape;
    private final double distanciaMetros;

    public AnaliseItem(@NonNull ThiltapePublico thiltape, double distanciaMetros) {
        this.thiltape = thiltape;
        this.distanciaMetros = distanciaMetros;
    }

    @NonNull
    public static Optional<AnaliseItem> fromJson(@Nullable JsonObject json) {
        if (json == null) {
            return Optional.empty();
        }
        JsonElement th = json.get("thiltape");
        if (th == null || th.isJsonNull() || !th.isJsonObject()) {
            return Optional.empty();
        }
        Optional<ThiltapePublico> tp = ThiltapePublico.fromJson(th.getAsJsonObject());
        if (tp.isEmpty() || !json.has("distancia_metros") || json.get("distancia_metros").isJsonNull()) {
            return Optional.empty();
        }
        return Optional.of(new AnaliseItem(
                tp.get(),
                json.get("distancia_metros").getAsDouble()
        ));
    }

    @NonNull
    public static List<AnaliseItem> listFromJsonArray(@Nullable JsonArray array) {
        List<AnaliseItem> lista = new ArrayList<>();
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
