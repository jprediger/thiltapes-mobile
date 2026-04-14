package br.univates.mobile.thiltapes;

/**
 * Representa um thiltape próximo ao jogador: fonte de imagem (URL ou Base64) e distância para efeitos.
 */
public final class ThiltapeProximo {

    private final String fonte;
    private final boolean ehBase64;
    private final float distanciaMetros;

    /**
     * @param fonte        URL http(s) ou string Base64 (com ou sem prefixo data:image)
     * @param ehBase64     {@code true} se {@code fonte} for Base64; {@code false} para URL
     * @param distanciaMetros distância em metros (mock ou futura API)
     */
    public ThiltapeProximo(String fonte, boolean ehBase64, float distanciaMetros) {
        this.fonte = fonte;
        this.ehBase64 = ehBase64;
        this.distanciaMetros = distanciaMetros;
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

    /**
     * Chave estável para cache de bitmap composto (lista), incluindo bucket de distância.
     */
    public String chaveCacheLista(int indiceLista) {
        long bucketDistancia = Math.round(distanciaMetros * 10f);
        return "L_" + indiceLista + "_" + ehBase64 + "_" + fonte.hashCode() + "_" + bucketDistancia;
    }
}
