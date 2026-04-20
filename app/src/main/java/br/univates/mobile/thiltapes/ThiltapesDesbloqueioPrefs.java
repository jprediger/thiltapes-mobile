package br.univates.mobile.thiltapes;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Persiste os thiltapes desbloqueados por jogo (ids inteiros em {@link SharedPreferences}).
 */
public final class ThiltapesDesbloqueioPrefs {

    private static final String PREFS = "thiltapes_desbloqueios";
    private static final String PREFIXO = "jogo_";

    private ThiltapesDesbloqueioPrefs() {
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    private static String chave(int jogoId) {
        return PREFIXO + jogoId;
    }

    @NonNull
    public static Set<Integer> obterIds(@NonNull Context ctx, int jogoId) {
        Set<String> bruto = prefs(ctx).getStringSet(chave(jogoId), Collections.emptySet());
        Set<Integer> saida = new HashSet<>();
        for (String s : bruto) {
            try {
                saida.add(Integer.parseInt(s));
            } catch (NumberFormatException ignored) {
                // ignora entrada invalida
            }
        }
        return saida;
    }

    public static void adicionar(@NonNull Context ctx, int jogoId, int thiltapeId) {
        Set<Integer> atual = obterIds(ctx, jogoId);
        if (atual.add(thiltapeId)) {
            gravar(ctx, jogoId, atual);
        }
    }

    public static void adicionarVarios(@NonNull Context ctx, int jogoId, @NonNull Iterable<Integer> ids) {
        Set<Integer> atual = obterIds(ctx, jogoId);
        boolean mudou = false;
        for (int id : ids) {
            if (atual.add(id)) {
                mudou = true;
            }
        }
        if (mudou) {
            gravar(ctx, jogoId, atual);
        }
    }

    private static void gravar(@NonNull Context ctx, int jogoId, @NonNull Set<Integer> ids) {
        Set<String> comoString = new HashSet<>();
        for (Integer i : ids) {
            comoString.add(String.valueOf(i));
        }
        prefs(ctx).edit().putStringSet(chave(jogoId), comoString).apply();
    }
}
