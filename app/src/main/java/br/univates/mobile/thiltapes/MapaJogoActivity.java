package br.univates.mobile.thiltapes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.maplibre.android.MapLibre;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.location.LocationComponent;
import org.maplibre.android.location.LocationComponentActivationOptions;
import org.maplibre.android.location.LocationComponentOptions;
import org.maplibre.android.location.modes.CameraMode;
import org.maplibre.android.location.modes.RenderMode;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.expressions.Expression;
import org.maplibre.android.style.layers.FillExtrusionLayer;
import org.maplibre.android.style.layers.PropertyFactory;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;

/**
 * Mapa em tela cheia com seguimento da posição do dispositivo e amostragem da localização
 * para lógica de jogo (limiar espacial antes de notificar).
 *
 * @see <a href="https://maplibre.org/maplibre-native/android/api/">MapLibre Android SDK</a>
 */
public class MapaJogoActivity extends AppCompatActivity implements LocationListener {

    /** Distância mínima (metros) entre amostras aceitas para atualizar {@link #localizacaoAtual} e notificar. */
    private static final float DISTANCIA_MINIMA_METROS = 5f;

    private static final String TAG = MapaJogoActivity.class.getSimpleName();

    /** Placeholder de rede para mock até integração com API. */
    private static final String URL_MOCK_THILTAPE = "https://unsplash.it/1024/1024";

    /** Distâncias fixas (m) para cada item da lista mock. */
    private static final float[] DISTANCIAS_MOCK_METROS = {
            5f, 10f, 20f, 50f, 70f, 90f, 110f, 150f, 210f, 300f, 400f
    };

    private MapView mapView;
    private @Nullable MapLibreMap map;
    private @Nullable LocationComponent tracker;
    private LocationManager locationManager;

    /** Última posição aceita pelo limiar; usada em {@link #setLocalizacaoAtual(Location)}. */
    private @Nullable Location localizacaoAtual;

    private final ActivityResultLauncher<String[]> permissaoLocalizacaoLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fine = Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false));
                if (fine) {
                    if (map != null && map.getStyle() != null) {
                        iniciarRastreamentoNativo();
                    }
                } else {
                    Toast.makeText(this, R.string.msg_permissao_local_negada, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        MapLibre.getInstance(this);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mapa_jogo);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_mapa_jogo), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this::aoMapaPronto);

        configurarPainelThiltapes();
    }

    /**
     * Bottom sheet com grade de thiltapes próximos (mock), handle para expandir e lista vazia tratada.
     */
    private void configurarPainelThiltapes() {
        View sheet = findViewById(R.id.sheet_thiltapes);
        RecyclerView recycler = findViewById(R.id.recycler_thiltapes);
        TextView textoVazio = findViewById(R.id.texto_lista_vazia_thiltapes);
        View handle = findViewById(R.id.handle_thiltapes);

        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
        // Sempre visível: só colapsado (primeira linha) ou expandido (tela cheia); nunca arrastar para fora.
        behavior.setHideable(false);
        behavior.setFitToContents(false);
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        handle.setOnClickListener(v -> {
            int state = behavior.getState();
            if (state == BottomSheetBehavior.STATE_EXPANDED) {
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        ArrayList<ThiltapeProximo> lista = gerarListaMockThiltapes();
        if (lista.isEmpty()) {
            recycler.setVisibility(View.GONE);
            textoVazio.setVisibility(View.VISIBLE);
        } else {
            recycler.setVisibility(View.VISIBLE);
            textoVazio.setVisibility(View.GONE);
            recycler.setLayoutManager(new GridLayoutManager(this, 4));
            ImagemMapaAdapter adapter = new ImagemMapaAdapter(lista,
                    (item, posicao) -> ThiltapeFullscreenDialog.mostrar(MapaJogoActivity.this, item, posicao));
            recycler.setAdapter(adapter);
        }
    }

    /** Lista fixa de thiltapes mock (mesma URL) com distâncias em metros para testar máscara e saturação. */
    private ArrayList<ThiltapeProximo> gerarListaMockThiltapes() {
        ArrayList<ThiltapeProximo> lista = new ArrayList<>();
        for (float metros : DISTANCIAS_MOCK_METROS) {
            lista.add(new ThiltapeProximo(URL_MOCK_THILTAPE, false, metros));
        }
        return lista;
    }

    /** Configura estilo, prédios 3D e fluxo de permissão após o mapa estar pronto. */
    private void aoMapaPronto(@NonNull MapLibreMap mapLibreMap) {
        this.map = mapLibreMap;
        mapLibreMap.getUiSettings().setTiltGesturesEnabled(true);
        mapLibreMap.getUiSettings().setRotateGesturesEnabled(true);
        mapLibreMap.setCameraPosition(new org.maplibre.android.camera.CameraPosition.Builder()
                .tilt(50.0)
                .zoom(16.0)
                .build());

        mapLibreMap.setStyle("https://tiles.openfreemap.org/styles/liberty", style -> {
            configurarPredios3d(style);
            verificarPedirPermissoes();
        });
    }

    /** Camada de extrusão dos edifícios no estilo Liberty (openmaptiles). */
    private void configurarPredios3d(@NonNull Style style) {
        FillExtrusionLayer camada = new FillExtrusionLayer("3d-buildings", "openmaptiles");
        camada.setSourceLayer("building");
        camada.setMinZoom(15f);
        camada.setProperties(
                PropertyFactory.fillExtrusionColor(Color.parseColor("#d4d4d4")),
                PropertyFactory.fillExtrusionHeight(Expression.get("render_height")),
                PropertyFactory.fillExtrusionBase(Expression.get("render_min_height")),
                PropertyFactory.fillExtrusionOpacity(0.9f)
        );
        style.addLayer(camada);
    }

    private void verificarPedirPermissoes() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            iniciarRastreamentoNativo();
        } else {
            permissaoLocalizacaoLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void iniciarRastreamentoNativo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (map == null || map.getStyle() == null) {
            return;
        }

        tracker = map.getLocationComponent();
        tracker.activateLocationComponent(
                LocationComponentActivationOptions
                        .builder(this, map.getStyle())
                        .useDefaultLocationEngine(false)
                        .locationComponentOptions(
                                LocationComponentOptions.builder(this)
                                        .trackingAnimationDurationMultiplier(0)
                                        .build()
                        )
                        .build());
        tracker.setCameraMode(CameraMode.TRACKING);
        tracker.setRenderMode(RenderMode.COMPASS);
        tracker.setLocationComponentEnabled(true);

        if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, this);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (tracker != null) {
            tracker.forceLocationUpdate(location);
        }
        setLocalizacaoAtual(location);
    }

    /**
     * Compara {@code localizacao} com {@link #localizacaoAtual}; se passar no limiar (ou primeira fix),
     * atualiza o membro e notifica.
     */
    private void setLocalizacaoAtual(@NonNull Location localizacao) {
        if (localizacaoAtual == null) {
            localizacaoAtual = new Location(localizacao);
            onNovaLocalizacaoPlayer(new LatLng(localizacao.getLatitude(), localizacao.getLongitude()));
            return;
        }
        float[] distancia = new float[1];
        Location.distanceBetween(
                localizacaoAtual.getLatitude(),
                localizacaoAtual.getLongitude(),
                localizacao.getLatitude(),
                localizacao.getLongitude(),
                distancia
        );
        if (distancia[0] >= DISTANCIA_MINIMA_METROS) {
            localizacaoAtual.set(localizacao);
            onNovaLocalizacaoPlayer(new LatLng(localizacao.getLatitude(), localizacao.getLongitude()));
        }
    }

    /**
     * Chamado quando {@link #localizacaoAtual} avança pelo limiar; ponto de extensão (log, API, etc.).
     */
    private void onNovaLocalizacaoPlayer(@NonNull LatLng localizacaoPlayer) {
        Log.i(TAG, String.format(
                "Nova localização do jogador: lat=%f lon=%f",
                localizacaoPlayer.getLatitude(),
                localizacaoPlayer.getLongitude()
        ));
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            Toast.makeText(this, R.string.msg_ativar_gps, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        // Nada obrigatório: retomada ocorre em onResume.
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (map != null && map.getStyle() != null && locationManager != null) {
                locationManager.removeUpdates(this);
                iniciarRastreamentoNativo();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ThiltapesCacheBitmapLru.limpar();
        mapView.onDestroy();
    }
}
