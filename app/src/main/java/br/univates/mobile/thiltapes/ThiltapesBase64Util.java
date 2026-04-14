package br.univates.mobile.thiltapes;

import android.util.Base64;

import androidx.annotation.NonNull;

/**
 * Normaliza e valida payloads Base64 antes do decode para respeitar limites de tamanho.
 */
public final class ThiltapesBase64Util {

    private ThiltapesBase64Util() {
    }

    @NonNull
    public static String extrairPayloadBase64(@NonNull String fonte) {
        String s = fonte.trim();
        int idx = s.indexOf("base64,");
        if (idx >= 0) {
            return s.substring(idx + "base64,".length());
        }
        return s;
    }

    public static boolean excedeLimiteArquivo(@NonNull String fonte) {
        String payload = extrairPayloadBase64(fonte);
        long tamanhoAproximadoBytes = (long) (payload.length() * 0.75);
        return tamanhoAproximadoBytes > ThiltapesImagemConstantes.LIMITE_TAMANHO_ARQUIVO_BYTES;
    }

    @NonNull
    public static byte[] decodificar(@NonNull String fonte) {
        String payload = extrairPayloadBase64(fonte);
        byte[] bytes = Base64.decode(payload, Base64.DEFAULT);
        if (bytes.length > ThiltapesImagemConstantes.LIMITE_TAMANHO_ARQUIVO_BYTES) {
            throw new IllegalArgumentException("Payload decodificado excede o limite configurado.");
        }
        return bytes;
    }
}
