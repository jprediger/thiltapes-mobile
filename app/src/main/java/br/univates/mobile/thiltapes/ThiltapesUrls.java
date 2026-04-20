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

    /** GET autenticado para bytes da imagem do thiltape ({@code /thiltapes/{id}/imagem}). */
    @NonNull
    public static String urlImagemThiltape(int thiltapeId) {
        return caminho("thiltapes", String.valueOf(thiltapeId), "imagem");
    }
}
