package br.univates.mobile.thiltapes;

import androidx.annotation.NonNull;

/**
 * Thiltape proximo no radar: fonte de imagem, distancia para efeitos e estado desbloqueado.
 * Imagens vem de {@code /uploads/*} (publicas), entao nao requerem cabecalho de autenticacao.
 */
public final class ThiltapeProximo {

    /** Id do servidor; {@code -1} em dados locais/mock. */
    private final int thiltapeId;
    private final String fonte;
    private final boolean ehBase64;
    private final float distanciaMetros;
    /** Sem saturacao/pixelizacao (ex.: ja desbloqueado). */
    private final boolean desbloqueado;
    /** Contorno dourado no radar para itens desbloqueados; nao usado no inventario. */
    private final boolean contornoDourado;

    /**
     * Mock ou dados locais sem id de API.
     */
    public ThiltapeProximo(String fonte, boolean ehBase64, float distanciaMetros) {
        this(-1, fonte, ehBase64, distanciaMetros, false, false);
    }

    public ThiltapeProximo(
            int thiltapeId,
            @NonNull String fonte,
            boolean ehBase64,
            float distanciaMetros,
            boolean desbloqueado,
            boolean contornoDourado) {
        this.thiltapeId = thiltapeId;
        this.fonte = fonte;
        this.ehBase64 = ehBase64;
        this.distanciaMetros = distanciaMetros;
        this.desbloqueado = desbloqueado;
        this.contornoDourado = contornoDourado;
    }

    public int getThiltapeId() {
        return thiltapeId;
    }

    public String getFonte() {
        return fonte;
    }

    public boolean isEhBase64() {
        return ehBase64;
    }

    public float getDistanciaMetros() {
        return distanciaMetros;
    }

    /** Se true, nao aplica pixelizacao/saturacao e usa imagem nitida. */
    public boolean isDesbloqueado() {
        return desbloqueado;
    }

    public boolean isContornoDourado() {
        return contornoDourado;
    }

    /**
     * Chave estavel para cache de bitmap composto (lista), inclui id e estado de desbloqueio.
     */
    public String chaveCacheLista(int indiceLista) {
        long bucketDistancia = Math.round(distanciaMetros * 10f);
        return "L_" + indiceLista + "_id" + thiltapeId + "_" + desbloqueado + "_"
                + ehBase64 + "_" + fonte.hashCode() + "_" + bucketDistancia;
    }
}
