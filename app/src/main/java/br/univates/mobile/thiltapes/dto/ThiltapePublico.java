package br.univates.mobile.thiltapes.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonObject;

import java.util.Optional;

/**
 * Thiltape exposto pela API publica (sem bytes da imagem; ver {@code ThiltapeResponse} no backend Go).
 */
public final class ThiltapePublico {

    private final int id;
    private final int jogoId;
    @NonNull
    private final String nome;
    private final int pontuacao;
    private final double lat;
    private final double lng;
    @NonNull
    private final String imagemUrl;

    public ThiltapePublico(
            int id,
            int jogoId,
            @NonNull String nome,
            int pontuacao,
            double lat,
            double lng,
            @NonNull String imagemUrl
    ) {
        this.id = id;
        this.jogoId = jogoId;
        this.nome = nome;
        this.pontuacao = pontuacao;
        this.lat = lat;
        this.lng = lng;
        this.imagemUrl = imagemUrl;
    }

    /**
     * Constroi a partir do JSON do servidor; campos obrigatorios ausentes ou invalidos retornam empty.
     */
    @NonNull
    public static Optional<ThiltapePublico> fromJson(@Nullable JsonObject json) {
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new ThiltapePublico(
                    json.get("id").getAsInt(),
                    json.get("game_id").getAsInt(),
                    json.get("name").getAsString(),
                    json.get("score").getAsInt(),
                    json.get("lat").getAsDouble(),
                    json.get("lng").getAsDouble(),
                    json.get("image_url").getAsString()
            ));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public int getId() {
        return id;
    }

    public int getJogoId() {
        return jogoId;
    }

    @NonNull
    public String getNome() {
        return nome;
    }

    public int getPontuacao() {
        return pontuacao;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    @NonNull
    public String getImagemUrl() {
        return imagemUrl;
    }
}
