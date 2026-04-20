package br.univates.mobile.thiltapes;

import android.app.Application;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Mantém a fila Volley única e centraliza avisos de configuração (URL da API).
 */
public class ThiltapesApplication extends Application {

    private static final String TAG = ThiltapesApplication.class.getSimpleName();

    private RequestQueue filaVolley;

    @Override
    public void onCreate() {
        super.onCreate();
        filaVolley = Volley.newRequestQueue(this);
        if (!BuildConfig.API_BASE_URL_DEFINIDA_EM_LOCAL_PROPERTIES) {
            Log.w(TAG, "Defina THILTAPES_API_BASE_URL em local.properties na raiz do projeto; "
                    + "o build usa valor padrao de desenvolvimento.");
        }
    }

    public RequestQueue obterFilaVolley() {
        return filaVolley;
    }
}
