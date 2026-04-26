package br.univates.mobile.thiltapes;

import androidx.annotation.NonNull;

/**
 * Monta URLs a partir de {@link BuildConfig#API_BASE_URL} sem barras duplicadas.
 */
public final class ThiltapesUrls {

    private ThiltapesUrls() {
    }

    @NonNull
    public static String base() {
        return BuildConfig.API_BASE_URL;
    }

    @NonNull
    public static String caminho(@NonNull String... segmentos) {
        StringBuilder sb = new StringBuilder(BuildConfig.API_BASE_URL);
        for (String s : segmentos) {
            if (!sb.toString().endsWith("/")) {
                sb.append('/');
            }
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Monta URL absoluta a partir de um caminho relativo retornado pela API
     * (ex.: {@code /uploads/abcd.png}). Imagens em {@code /uploads/*} sao publicas
     * — nao precisam de header de autenticacao.
     */
    @NonNull
    public static String urlImagemAbsoluta(@NonNull String caminhoRelativo) {
        String base = BuildConfig.API_BASE_URL;
        if (base.endsWith("/") && caminhoRelativo.startsWith("/")) {
            return base + caminhoRelativo.substring(1);
        }
        if (!base.endsWith("/") && !caminhoRelativo.startsWith("/")) {
            return base + "/" + caminhoRelativo;
        }
        return base + caminhoRelativo;
    }
}
