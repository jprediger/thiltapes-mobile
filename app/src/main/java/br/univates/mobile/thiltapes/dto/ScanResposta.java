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

    /** Default usado quando o servidor nao envia {@code raio_captura_metros} (clientes antigos / fallback). */
    public static final int RAIO_CAPTURA_METROS_DEFAULT = 15;

    @NonNull
    private final List<ScanItem> proximos;
    @NonNull
    private final List<ScanItem> capturados;
    private final int raioCapturaMetros;

    public ScanResposta(
            @NonNull List<ScanItem> proximos,
            @NonNull List<ScanItem> capturados,
            int raioCapturaMetros) {
        this.proximos = proximos;
        this.capturados = capturados;
        this.raioCapturaMetros = raioCapturaMetros;
    }

    @NonNull
    public static Optional<ScanResposta> fromJson(@Nullable JsonObject json) {
        if (json == null) {
            return Optional.empty();
        }
        return Optional.of(new ScanResposta(
                ScanItem.listFromJsonArray(obterArrayOuVazio(json, "proximos")),
                ScanItem.listFromJsonArray(obterArrayOuVazio(json, "capturados")),
                obterIntOuPadrao(json, "raio_captura_metros", RAIO_CAPTURA_METROS_DEFAULT)
        ));
    }

    @NonNull
    private static JsonArray obterArrayOuVazio(@NonNull JsonObject json, @NonNull String chave) {
        if (!json.has(chave) || json.get(chave).isJsonNull() || !json.get(chave).isJsonArray()) {
            return new JsonArray();
        }
        return json.getAsJsonArray(chave);
    }

    private static int obterIntOuPadrao(@NonNull JsonObject json, @NonNull String chave, int padrao) {
        if (!json.has(chave) || json.get(chave).isJsonNull()) {
            return padrao;
        }
        try {
            return json.get(chave).getAsInt();
        } catch (RuntimeException ignored) {
            return padrao;
        }
    }

    @NonNull
    public List<ScanItem> getProximos() {
        return Collections.unmodifiableList(proximos);
    }

    @NonNull
    public List<ScanItem> getCapturados() {
        return Collections.unmodifiableList(capturados);
    }

    public int getRaioCapturaMetros() {
        return raioCapturaMetros;
    }
}
