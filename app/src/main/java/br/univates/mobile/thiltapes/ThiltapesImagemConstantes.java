package br.univates.mobile.thiltapes;

/**
 * Limites e parâmetros visuais para miniaturas de thiltapes no mapa e tela cheia.
 */
public final class ThiltapesImagemConstantes {

    private ThiltapesImagemConstantes() {
    }

    /** Distância em metros acima da qual a saturação chega a zero (escala de cinza). */
    public static final float DISTANCIA_METROS_MAXIMA_EFETO = 100f;

    /** Saturação na distância ~0 m (0 = cinza, 1 = cor original). */
    public static final float SATURACAO_EM_DISTANCIA_ZERO = 0.85f;

    /**
     * Até esta distância (m) a imagem permanece na resolução original — pixelização desligada.
     */
    public static final float DISTANCIA_METROS_PIXELIZACAO_ORIGINAL = 30f;

    /**
     * A partir desta distância (m) a pixelização é máxima: uma única célula (cor mais comum na imagem inteira).
     * Usada com {@link #DISTANCIA_METROS_PIXELIZACAO_ORIGINAL} na fórmula de progresso (ver {@code GeradorOverlayThiltape}).
     */
    public static final float DISTANCIA_METROS_PIXELIZACAO_MAXIMA = 650f;

    /** Rejeita decode se qualquer lado do bitmap exceder este valor (px). */
    public static final int LIMITE_DIMENSAO_MAXIMA_POR_LADO_PX = 2048;

    /** Limite de bytes do arquivo (URL após download ou payload Base64 decodificado). */
    public static final long LIMITE_TAMANHO_ARQUIVO_BYTES = 5 * 1024 * 1024L;

    /**
     * Meta aproximada de quantas imagens compostas distintas manter; o LRU principal é por bytes.
     */
    public static final int LIMITE_ENTRADAS_CACHE_MEMORIA = 8;

    /** Teto de memória para bitmaps compostos em cache na camada da app (LRU próprio). */
    public static final long LIMITE_BYTES_TOTAL_CACHE_MEMORIA = 16 * 1024 * 1024L;

    /** Teto separado para o cache em memória do Glide (pool de Resources). */
    public static final long LIMITE_BYTES_TOTAL_CACHE_MEMORIA_GLIDE = 16 * 1024 * 1024L;

    /**
     * Teto (px) para decode em lista e tela cheia; itens {@link ThiltapeProximo#isDesbloqueado() desbloqueados}
     * usam {@code fitCenter} (sem recorte); bloqueados no mapa usam {@code centerCrop} com o mesmo teto.
     *
     * @see <a href="https://bumptech.github.io/glide/doc/resizing.html">Glide - Resizing</a>
     */
    public static final int CARREGAMENTO_THILTAPE_LADO_PX = 512;

    /** Offset vertical (px) entre o pin do marker e a borda inferior do popup de captura. */
    public static final float OFFSET_VERTICAL_POPUP_PX = 48f;

    /** Distância limite (m) para borda verde na lista (muito perto — imagem nítida). */
    public static final float DISTANCIA_BORDA_PROXIMA_METROS = 30f;

    /** Distância limite (m) para borda âmbar na lista; acima disso borda vermelha. */
    public static final float DISTANCIA_BORDA_MEDIA_METROS = 200f;
}
