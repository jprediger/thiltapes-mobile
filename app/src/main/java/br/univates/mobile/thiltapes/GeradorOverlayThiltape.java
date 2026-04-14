package br.univates.mobile.thiltapes;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Pixelização por distância: entre {@link ThiltapesImagemConstantes#DISTANCIA_METROS_PIXELIZACAO_ORIGINAL}
 * e {@link ThiltapesImagemConstantes#DISTANCIA_METROS_PIXELIZACAO_MAXIMA} a resolução efetiva cai de original até 1×1 (cor modal por bloco);
 * depois reescala com vizinho mais próximo. O progresso usa {@code normalizada = 1 - clamp((d-DST_MIN)/DST_RANGE)}
 * e {@code pixelWidth = w * pixelSize} com {@code pixelSize} interpolado em cada eixo. Saturação e pixelização usam
 * progresso linear normalizado elevado ao quadrado (queda quadrática da distância mínima à máxima). A saturação aplica-se antes na miniatura e na tela cheia.
 */
public final class GeradorOverlayThiltape {

    private GeradorOverlayThiltape() {
    }

    /**
     * Miniatura: saturação por distância + pixelização; não recicla {@code origemGlide}.
     */
    @NonNull
    public static Bitmap criarMiniaturaComposta(
            @NonNull Bitmap origemGlide,
            float distanciaMetros) {
        Bitmap base = aplicarSaturacaoPorDistancia(origemGlide, distanciaMetros);
        Bitmap resultado = aplicarPixelizacaoSeNecessario(base, distanciaMetros);
        if (resultado != base && base != origemGlide && !base.isRecycled()) {
            base.recycle();
        }
        return resultado;
    }

    /**
     * Tela cheia: mesma saturação por distância e pixelização que {@link #criarMiniaturaComposta}; não recicla {@code origemGlide}.
     */
    @NonNull
    public static Bitmap criarFullscreenComposto(
            @NonNull Bitmap origemGlide,
            float distanciaMetros) {
        Bitmap base = aplicarSaturacaoPorDistancia(origemGlide, distanciaMetros);
        Bitmap resultado = aplicarPixelizacaoSeNecessario(base, distanciaMetros);
        if (resultado != base && base != origemGlide && !base.isRecycled()) {
            base.recycle();
        }
        return resultado;
    }

    /**
     * Progresso u ∈ [0,1] para pixelização: linear em distância, depois {@code u = u_lin²} (queda quadrática).
     */
    private static float progressoPixelizacao(float distanciaMetros) {
        float dstMin = ThiltapesImagemConstantes.DISTANCIA_METROS_PIXELIZACAO_ORIGINAL;
        float dstRange = ThiltapesImagemConstantes.DISTANCIA_METROS_PIXELIZACAO_MAXIMA - dstMin;
        if (dstRange <= 0f) {
            return 0f;
        }
        float uLin = Math.max(0f, Math.min(1f, (distanciaMetros - dstMin) / dstRange));
        return progressoQuadratico01(uLin);
    }

    /** Progresso linear em [0,1] elevado ao quadrado (queda quadrática ao longo da distância). */
    private static float progressoQuadratico01(float linear01) {
        if (linear01 <= 0f) {
            return 0f;
        }
        if (linear01 >= 1f) {
            return 1f;
        }
        return linear01 * linear01;
    }

    /**
     * Reduz a imagem a {@code wSmall×hSmall} (cor modal por bloco) e amplia de volta com filtro desligado (blocos nítidos).
     */
    @NonNull
    private static Bitmap aplicarPixelizacaoSeNecessario(@NonNull Bitmap base, float distanciaMetros) {
        float u = progressoPixelizacao(distanciaMetros);
        if (u <= 0f) {
            return base;
        }

        int w = base.getWidth();
        int h = base.getHeight();
        if (w <= 0 || h <= 0) {
            return base;
        }

        // pixelSize por eixo: lerp(1/lado, 1, u) ⇒ pixelWidth = w * pixelSize = 1 + u*(w-1) (idem altura).
        float pixelWidth = 1f + u * (w - 1f);
        float pixelHeight = 1f + u * (h - 1f);
        int wSmall = Math.max(1, Math.round(w / pixelWidth));
        int hSmall = Math.max(1, Math.round(h / pixelHeight));

        if (wSmall >= w && hSmall >= h) {
            return base;
        }

        Bitmap reduzida = Bitmap.createBitmap(wSmall, hSmall, Bitmap.Config.ARGB_8888);
        for (int j = 0; j < hSmall; j++) {
            int top = j * h / hSmall;
            int bottom = (j + 1 == hSmall) ? h : (j + 1) * h / hSmall;
            for (int i = 0; i < wSmall; i++) {
                int left = i * w / wSmall;
                int right = (i + 1 == wSmall) ? w : (i + 1) * w / wSmall;
                int rgb = corMaisComumNoRetangulo(base, left, top, right, bottom);
                reduzida.setPixel(i, j, rgb | 0xFF000000);
            }
        }

        Bitmap ampliada = Bitmap.createScaledBitmap(reduzida, w, h, false);
        if (!reduzida.isRecycled()) {
            reduzida.recycle();
        }
        return ampliada;
    }

    @NonNull
    private static Bitmap aplicarSaturacaoPorDistancia(@NonNull Bitmap origem, float distanciaMetros) {
        float saturacao = saturacaoInterpolada(distanciaMetros);
        Bitmap saida = Bitmap.createBitmap(origem.getWidth(), origem.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(saida);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        ColorMatrix matriz = new ColorMatrix();
        matriz.setSaturation(saturacao);
        paint.setColorFilter(new ColorMatrixColorFilter(matriz));
        canvas.drawBitmap(origem, 0, 0, paint);
        return saida;
    }

    /**
     * Saturação: perto de 0 m → cor quase plena; em {@link ThiltapesImagemConstantes#DISTANCIA_METROS_MAXIMA_EFETO} → cinza.
     * O avanço para cinza segue progresso linear na distância elevado ao quadrado (mesma ideia da pixelização).
     */
    public static float saturacaoInterpolada(float distanciaMetros) {
        float tLin = distanciaMetros / ThiltapesImagemConstantes.DISTANCIA_METROS_MAXIMA_EFETO;
        if (tLin < 0f) {
            tLin = 0f;
        }
        if (tLin > 1f) {
            tLin = 1f;
        }
        float t = progressoQuadratico01(tLin);
        return ThiltapesImagemConstantes.SATURACAO_EM_DISTANCIA_ZERO * (1f - t);
    }

    /** Cor RGB média do bucket modal (histograma 3×3×3 por canal) na região [left,right)×[top,bottom). */
    private static int corMaisComumNoRetangulo(@NonNull Bitmap bmp, int left, int top, int right, int bottom) {
        int bw = bmp.getWidth();
        int bh = bmp.getHeight();
        left = Math.max(0, left);
        top = Math.max(0, top);
        right = Math.min(bw, right);
        bottom = Math.min(bh, bottom);
        if (right <= left || bottom <= top) {
            return Color.BLACK;
        }
        HashMap<Integer, BucketStats> map = new HashMap<>();
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                int p = bmp.getPixel(x, y);
                int r = Color.red(p);
                int g = Color.green(p);
                int b = Color.blue(p);
                int key = ((r >> 5) << 6) | ((g >> 5) << 3) | (b >> 5);
                BucketStats s = map.get(key);
                if (s == null) {
                    s = new BucketStats();
                    map.put(key, s);
                }
                s.count++;
                s.sumR += r;
                s.sumG += g;
                s.sumB += b;
            }
        }
        int maxKey = 0;
        int maxCount = -1;
        for (Map.Entry<Integer, BucketStats> e : map.entrySet()) {
            int c = e.getValue().count;
            int k = e.getKey();
            if (c > maxCount || (c == maxCount && k < maxKey)) {
                maxCount = c;
                maxKey = k;
            }
        }
        if (maxCount < 0) {
            return Color.BLACK;
        }
        BucketStats best = map.get(maxKey);
        int r = best.sumR / best.count;
        int g = best.sumG / best.count;
        int b = best.sumB / best.count;
        return Color.rgb(r, g, b);
    }

    private static final class BucketStats {
        int count;
        int sumR;
        int sumG;
        int sumB;
    }
}
