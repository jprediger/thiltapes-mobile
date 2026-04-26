package br.univates.mobile.thiltapes;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Armazena android_id, token Bearer e cache leve de perfil; expoe cabecalho Authorization.
 */
public final class ThiltapesSessao {

    private static final String PREFS = "thiltapes_sessao";
    private static final String K_NOME = "nome_exibido";
    private static final String K_EH_ADMIN = "eh_admin";
    private static final String K_TOKEN = "token";

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

    public void salvarToken(@Nullable String token) {
        prefs.edit().putString(K_TOKEN, token != null ? token : "").apply();
    }

    @NonNull
    public String obterToken() {
        return prefs.getString(K_TOKEN, "");
    }

    public boolean temToken() {
        return !obterToken().isEmpty();
    }

    /**
     * Bearer Auth: token UUID emitido por {@code POST /register} ou {@code POST /login}.
     * Quando ainda nao houve bootstrap o valor vira "Bearer " (caller deve garantir token antes).
     */
    @NonNull
    public String obterCabeçalhoAuthorization() {
        return "Bearer " + obterToken();
    }
}
