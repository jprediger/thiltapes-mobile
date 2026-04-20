package br.univates.mobile.thiltapes;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;

/**
 * Armazena android_id, nome exibido (senha Basic) e cache leve de perfil; expôe cabeçalho Authorization.
 */
public final class ThiltapesSessao {

    private static final String PREFS = "thiltapes_sessao";
    private static final String K_NOME = "nome_exibido";
    private static final String K_EH_ADMIN = "eh_admin";

    private final SharedPreferences prefs;
    private final String androidId;

    private ThiltapesSessao(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }

    public static ThiltapesSessao de(@NonNull Context context) {
        return new ThiltapesSessao(context);
    }

    @NonNull
    public String obterAndroidId() {
        return androidId != null ? androidId : "";
    }

    public void salvarNomeLocal(@Nullable String nome) {
        prefs.edit().putString(K_NOME, nome != null ? nome : "").apply();
    }

    @NonNull
    public String obterNomeLocal() {
        return prefs.getString(K_NOME, "");
    }

    public void salvarEhAdmin(boolean valor) {
        prefs.edit().putBoolean(K_EH_ADMIN, valor).apply();
    }

    public boolean obterEhAdminCache() {
        return prefs.getBoolean(K_EH_ADMIN, false);
    }

    /**
     * Basic Auth: usuario = android_id, senha = nome (pode ser vazio).
     */
    @NonNull
    public String obterCabeçalhoAuthorization() {
        String nome = obterNomeLocal();
        String credencial = obterAndroidId() + ":" + nome;
        String codificada = Base64.encodeToString(
                credencial.getBytes(StandardCharsets.UTF_8),
                Base64.NO_WRAP
        );
        return "Basic " + codificada;
    }
}
