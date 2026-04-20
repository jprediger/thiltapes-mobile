package br.univates.mobile.thiltapes;

import android.app.Dialog;
import android.view.View;
import android.view.Window;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Aplica padding com base nas barras do sistema e recorte (safe area) em telas edge-to-edge.
 */
public final class ThiltapesBarraSistema {

    private ThiltapesBarraSistema() {
    }

    private static int tiposBarrasErecorte() {
        return WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
    }

    /**
     * Activity com conteudo raiz que recebe o padding (nao chamar duas vezes no mesmo root sem necessidade).
     */
    public static void aplicarNaRaiz(@NonNull AppCompatActivity activity, @IdRes int rootId) {
        EdgeToEdge.enable(activity);
        View root = activity.findViewById(rootId);
        if (root != null) {
            aplicarPaddingEmView(root);
        }
    }

    /** Quando {@link EdgeToEdge#enable} ja foi chamado; apenas aplica padding no root. */
    public static void aplicarPaddingEmView(@NonNull View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets b = insets.getInsets(tiposBarrasErecorte());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return insets;
        });
    }

    /**
     * Dialog fullscreen: evita FAB ou conteudo sob status/nav.
     */
    public static void aplicarNoDialogo(@NonNull Dialog dialog, @IdRes int rootId) {
        Window w = dialog.getWindow();
        if (w != null) {
            WindowCompat.setDecorFitsSystemWindows(w, false);
        }
        View root = dialog.findViewById(rootId);
        if (root == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets b = insets.getInsets(tiposBarrasErecorte());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
