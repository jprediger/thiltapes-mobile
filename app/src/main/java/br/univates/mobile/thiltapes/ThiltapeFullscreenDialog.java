package br.univates.mobile.thiltapes;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Dialogo em tela cheia com zoom/pan; apenas thiltapes ainda bloqueados no mapa recebem overlay no bitmap.
 */
public final class ThiltapeFullscreenDialog {

    private ThiltapeFullscreenDialog() {
    }

    /**
     * Exibe a imagem em tela cheia com PhotoView; recarrega via Glide respeitando limites de decode.
     */
    public static void mostrar(
            @NonNull AppCompatActivity activity,
            @NonNull ThiltapeProximo item,
            int posicao) {
        if (activity.isFinishing()) {
            return;
        }

        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_thiltape_fullscreen, null, false);
        dialog.setContentView(root);

        Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            w.setBackgroundDrawableResource(android.R.color.black);
        }

        PhotoView photoView = root.findViewById(R.id.photo_view_fullscreen);
        FloatingActionButton fechar = root.findViewById(R.id.botao_fechar_fullscreen);
        fechar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        ThiltapesBarraSistema.aplicarNoDialogo(dialog, R.id.root_thiltape_fullscreen);

        RequestBuilder<Bitmap> rb = Glide.with(activity).asBitmap();
        if (item.isEhBase64()) {
            if (ThiltapesBase64Util.excedeLimiteArquivo(item.getFonte())) {
                dialog.dismiss();
                return;
            }
            try {
                byte[] bytes = ThiltapesBase64Util.decodificar(item.getFonte());
                rb = rb.load(bytes);
            } catch (IllegalArgumentException e) {
                dialog.dismiss();
                return;
            }
        } else {
            rb = rb.load(item.getFonte());
        }

        rb.apply(ThiltapesGlideOpcoesCarregamento.opcoesListaOuFullscreen(item)).into(new CustomTarget<Bitmap>(
                ThiltapesImagemConstantes.CARREGAMENTO_THILTAPE_LADO_PX,
                ThiltapesImagemConstantes.CARREGAMENTO_THILTAPE_LADO_PX) {
            @Override
            public void onResourceReady(
                    @NonNull Bitmap resource,
                    @Nullable Transition<? super Bitmap> transition) {
                if (activity.isFinishing() || !dialog.isShowing()) {
                    return;
                }
                if (resource.getWidth() > ThiltapesImagemConstantes.LIMITE_DIMENSAO_MAXIMA_POR_LADO_PX
                        || resource.getHeight() > ThiltapesImagemConstantes.LIMITE_DIMENSAO_MAXIMA_POR_LADO_PX) {
                    return;
                }
                Bitmap composto;
                if (item.isDesbloqueado()) {
                    composto = Bitmap.createBitmap(
                            resource.getWidth(),
                            resource.getHeight(),
                            Bitmap.Config.ARGB_8888
                    );
                    android.graphics.Canvas canvas = new android.graphics.Canvas(composto);
                    canvas.drawBitmap(resource, 0f, 0f, null);
                } else {
                    composto = GeradorOverlayThiltape.criarFullscreenComposto(
                            resource,
                            item.getDistanciaMetros());
                }
                photoView.setImageBitmap(composto);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        });
    }
}
