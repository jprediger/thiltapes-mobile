package br.univates.mobile.thiltapes.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Corpo {@code 200} de {@code POST /analise} (ver {@code analise.ts} no backend).
 */
public final class AnaliseResposta {

    @NonNull
    private final List<AnaliseItem> proximos;
    @NonNull
    private final List<AnaliseItem> desbloqueados;

    public AnaliseResposta(@NonNull List<AnaliseItem> proximos, @NonNull List<AnaliseItem> desbloqueados) {
        this.proximos = proximos;
        this.desbloqueados = desbloqueados;
    }

    @NonNull
    public static Optional<AnaliseResposta> fromJson(@Nullable JsonObject json) {
        if (json == null) {
            return Optional.empty();
        }
        JsonArray prox = obterArrayOuVazio(json, "proximos");
        JsonArray desb = obterArrayOuVazio(json, "desbloqueados");
        return Optional.of(new AnaliseResposta(
                AnaliseItem.listFromJsonArray(prox),
                AnaliseItem.listFromJsonArray(desb)
        ));
    }

    @NonNull
    private static JsonArray obterArrayOuVazio(@NonNull JsonObject json, @NonNull String chave) {
        if (!json.has(chave) || json.get(chave).isJsonNull() || !json.get(chave).isJsonArray()) {
            return new JsonArray();
        }
        return json.getAsJsonArray(chave);
    }

    @NonNull
    public List<AnaliseItem> getProximos() {
        return Collections.unmodifiableList(proximos);
    }

    @NonNull
    public List<AnaliseItem> getDesbloqueados() {
        return Collections.unmodifiableList(desbloqueados);
    }
}
