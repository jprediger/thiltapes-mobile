package br.univates.mobile.thiltapes.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Corpo {@code 200} de {@code GET /jogos/:jogoId/scan} — ver {@code ScanResponse} no backend Go.
 */
public final class ScanResposta {

    @NonNull
    private final List<ScanItem> proximos;
    @NonNull
    private final List<ScanItem> capturados;

    public ScanResposta(@NonNull List<ScanItem> proximos, @NonNull List<ScanItem> capturados) {
        this.proximos = proximos;
        this.capturados = capturados;
    }

    @NonNull
    public static Optional<ScanResposta> fromJson(@Nullable JsonObject json) {
        if (json == null) {
            return Optional.empty();
        }
        return Optional.of(new ScanResposta(
                ScanItem.listFromJsonArray(obterArrayOuVazio(json, "proximos")),
                ScanItem.listFromJsonArray(obterArrayOuVazio(json, "capturados"))
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
    public List<ScanItem> getProximos() {
        return Collections.unmodifiableList(proximos);
    }

    @NonNull
    public List<ScanItem> getCapturados() {
        return Collections.unmodifiableList(capturados);
    }
}
