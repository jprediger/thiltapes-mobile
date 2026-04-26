package br.univates.mobile.thiltapes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.VolleyError;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONObject;

import com.google.gson.JsonParser;

import java.util.Optional;

import br.univates.mobile.thiltapes.dto.InventarioLinha;
import br.univates.mobile.thiltapes.dto.ScanItem;
import br.univates.mobile.thiltapes.dto.ScanResposta;
import br.univates.mobile.thiltapes.dto.ThiltapePublico;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mapa do jogo: localizacao, {@code GET /jogos/:jogoId/scan} periodico e lista de thiltapes proximos.
 */
public class MapaJogoActivity extends AppCompatActivity implements LocationListener {

    public static final String EXTRA_JOGO_ID = "jogo_id";

    private static final float DISTANCIA_MINIMA_METROS = 5f;
    private static final long INTERVALO_ANALISE_MS = 8000L;

    private static final String TAG = MapaJogoActivity.class.getSimpleName();

    private MapView mapView;
    private @Nullable MapLibreMap map;
    private @Nullable LocationComponent tracker;
    private LocationManager locationManager;

    private @Nullable Location localizacaoAtual;

    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());
    private long ultimaAnaliseMs = 0L;

    private int jogoId = -1;
    /** Thiltapes em qualquer jogo ({@code GET /inventario}): contorno dourado, imagem nitida e fallback de URL. */
    private final Map<Integer, ThiltapePublico> inventarioGlobalPorId = new HashMap<>();
    private RecyclerView recyclerThiltapes;
    private TextView textoVazio;
    private TextView textoPontuacaoJogo;
    private ImagemMapaAdapter adapterThiltapes;

    private final ActivityResultLauncher<String[]> permissaoLocalizacaoLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
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

        jogoId = getIntent().getIntExtra(EXTRA_JOGO_ID, -1);
        if (jogoId < 0) {
            Toast.makeText(this, R.string.msg_jogo_invalido, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        MapLibre.getInstance(this);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mapa_jogo);
        ThiltapesBarraSistema.aplicarPaddingEmView(findViewById(R.id.activity_mapa_jogo));

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        findViewById(R.id.btn_inventario_jogo_mapa).setOnClickListener(v -> abrirInventarioDoJogo());
        textoPontuacaoJogo = findViewById(R.id.texto_pontuacao_jogo_mapa);
        atualizarTextoPontuacao(0);

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this::aoMapaPronto);

        configurarPainelThiltapes();
        carregarIdsInventarioGlobalParaMapa();
    }

    private void carregarIdsInventarioGlobalParaMapa() {
        ThiltapesApi.getInventario(this, null, array -> {
            inventarioGlobalPorId.clear();
            for (InventarioLinha l : InventarioLinha.listFromJsonArrayLegacy(array)) {
                ThiltapePublico t = l.getThiltape();
                inventarioGlobalPorId.put(t.getId(), t);
            }
            if (localizacaoAtual != null) {
                executarAnaliseSePossivel();
            }
        }, e -> { /* inventario opcional para contorno */ });
    }

    private void abrirInventarioDoJogo() {
        Intent i = new Intent(this, InventarioActivity.class);
        i.putExtra(InventarioActivity.EXTRA_FILTRO_JOGO_ID, jogoId);
        startActivity(i);
    }

    private void configurarPainelThiltapes() {
        View sheet = findViewById(R.id.sheet_thiltapes);
        recyclerThiltapes = findViewById(R.id.recycler_thiltapes);
        textoVazio = findViewById(R.id.texto_lista_vazia_thiltapes);
        View handle = findViewById(R.id.handle_thiltapes);

        View containerInventario = findViewById(R.id.container_inventario_mapa);

        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
        behavior.setHideable(false);
        behavior.setFitToContents(false);
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                boolean colapsado = newState == BottomSheetBehavior.STATE_COLLAPSED;
                containerInventario.setVisibility(colapsado ? View.VISIBLE : View.GONE);
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

        recyclerThiltapes.setLayoutManager(new GridLayoutManager(this, 4));
        atualizarListaRecycler(new ArrayList<>());
    }

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
            agendarProximaAnalise(1500L);
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

    private void agendarProximaAnalise(long atrasoMs) {
        handlerPrincipal.removeCallbacks(tarefaAnalise);
        handlerPrincipal.postDelayed(tarefaAnalise, atrasoMs);
    }

    private final Runnable tarefaAnalise = this::executarAnaliseSePossivel;

    private void executarAnaliseSePossivel() {
        if (isFinishing() || jogoId < 0 || localizacaoAtual == null) {
            agendarProximaAnalise(INTERVALO_ANALISE_MS);
            return;
        }
        long agora = android.os.SystemClock.elapsedRealtime();
        if (agora - ultimaAnaliseMs < 2000L) {
            agendarProximaAnalise(INTERVALO_ANALISE_MS);
            return;
        }
        ultimaAnaliseMs = agora;
        ThiltapesApi.getScan(
                this,
                jogoId,
                localizacaoAtual.getLatitude(),
                localizacaoAtual.getLongitude(),
                this::aplicarRespostaScan,
                this::aoErroAnalise
        );
        agendarProximaAnalise(INTERVALO_ANALISE_MS);
    }

    private void aplicarRespostaScan(JSONObject corpo) {
        final ScanResposta r;
        try {
            Optional<ScanResposta> opt = ScanResposta.fromJson(
                    JsonParser.parseString(corpo.toString()).getAsJsonObject());
            if (opt.isEmpty()) {
                Log.w(TAG, "parse scan: JSON invalido");
                return;
            }
            r = opt.get();
        } catch (RuntimeException e) {
            Log.w(TAG, "parse scan", e);
            return;
        }
        Map<Integer, ThiltapePublico> publicoPorId = new HashMap<>();
        List<Integer> idsNovosDesbloqueio = new ArrayList<>();
        for (ScanItem item : r.getCapturados()) {
            ThiltapePublico t = item.getThiltape();
            publicoPorId.put(t.getId(), t);
            idsNovosDesbloqueio.add(t.getId());
        }
        ThiltapesDesbloqueioPrefs.adicionarVarios(this, jogoId, idsNovosDesbloqueio);

        Map<Integer, Float> distPorId = new HashMap<>();
        for (ScanItem p : r.getProximos()) {
            ThiltapePublico t = p.getThiltape();
            publicoPorId.putIfAbsent(t.getId(), t);
            distPorId.put(t.getId(), (float) p.getDistanciaMetros());
        }

        Set<Integer> desbloqueados = ThiltapesDesbloqueioPrefs.obterIds(this, jogoId);
        Set<Integer> uniao = new HashSet<>(desbloqueados);
        uniao.addAll(distPorId.keySet());

        List<ThiltapeProximo> lista = new ArrayList<>();
        for (int id : uniao) {
            float d = distPorId.containsKey(id)
                    ? distPorId.get(id)
                    : ThiltapesImagemConstantes.DISTANCIA_METROS_PIXELIZACAO_MAXIMA;
            boolean desbloqueadoVisual = desbloqueados.contains(id) || inventarioGlobalPorId.containsKey(id);
            ThiltapePublico publico = publicoPorId.get(id);
            if (publico == null) {
                publico = inventarioGlobalPorId.get(id);
            }
            String url = (publico != null) ? ThiltapesUrls.urlImagemAbsoluta(publico.getImagemUrl()) : "";
            lista.add(new ThiltapeProximo(
                    id,
                    url,
                    false,
                    d,
                    desbloqueadoVisual,
                    desbloqueadoVisual
            ));
        }

        atualizarListaRecycler(lista);
        if (!idsNovosDesbloqueio.isEmpty()) {
            carregarPontuacaoJogo();
        }
    }

    /**
     * Soma pontuacoes dos thiltapes coletados neste jogo ({@code GET /inventario?jogo_id=}).
     */
    private void carregarPontuacaoJogo() {
        ThiltapesApi.getInventario(this, jogoId, array -> {
            List<InventarioLinha> linhas = InventarioLinha.listFromJsonArrayLegacy(array);
            atualizarTextoPontuacao(InventarioLinha.somarPontuacaoPorThiltapeUnico(linhas));
        }, e -> atualizarTextoPontuacao(0));
    }

    private void atualizarTextoPontuacao(int total) {
        textoPontuacaoJogo.setText(getString(R.string.pontuacao_jogo_format, total));
    }

    private void aoErroAnalise(VolleyError e) {
        Log.w(TAG, "analise: " + e.toString());
    }

    private void atualizarListaRecycler(@NonNull List<ThiltapeProximo> lista) {
        if (lista.isEmpty()) {
            recyclerThiltapes.setVisibility(View.GONE);
            textoVazio.setVisibility(View.VISIBLE);
        } else {
            recyclerThiltapes.setVisibility(View.VISIBLE);
            textoVazio.setVisibility(View.GONE);
            adapterThiltapes = new ImagemMapaAdapter(lista,
                    (item, posicao) -> ThiltapeFullscreenDialog.mostrar(MapaJogoActivity.this, item, posicao));
            recyclerThiltapes.setAdapter(adapterThiltapes);
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

    private void onNovaLocalizacaoPlayer(@NonNull LatLng localizacaoPlayer) {
        Log.i(TAG, String.format(
                "Nova localizacao: lat=%f lon=%f",
                localizacaoPlayer.getLatitude(),
                localizacaoPlayer.getLongitude()
        ));
        executarAnaliseSePossivel();
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            Toast.makeText(this, R.string.msg_ativar_gps, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
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
        carregarPontuacaoJogo();
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
        handlerPrincipal.removeCallbacks(tarefaAnalise);
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
