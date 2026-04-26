package br.univates.mobile.thiltapes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONObject;

import com.google.gson.JsonParser;

import java.util.Optional;

import br.univates.mobile.thiltapes.dto.InventarioLinha;
import br.univates.mobile.thiltapes.dto.ScanItem;
import br.univates.mobile.thiltapes.dto.ScanResposta;
import br.univates.mobile.thiltapes.dto.ThiltapePublico;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private int raioCapturaMetros = ScanResposta.RAIO_CAPTURA_METROS_DEFAULT;
    /** Markers ativos no mapa (thiltapes dentro do raio de captura). */
    private final Map<Integer, Marker> markersCaptura = new HashMap<>();
    /** Dados do scan associados a cada marker (para preencher o popup ao clicar). */
    private final Map<Marker, ScanItem> dadosDoMarker = new HashMap<>();
    /** Ultima lista renderizada no recycler — usada para remover item ao capturar sem aguardar proximo scan. */
    private List<ThiltapeProximo> ultimaListaProximos = new ArrayList<>();
    private View popupCaptura;
    private @Nullable Integer idThiltapePopupAberto;
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
        popupCaptura = findViewById(R.id.popup_captura_thiltape);
        configurarBotaoCapturar();
    }

    private void configurarBotaoCapturar() {
        Button botao = popupCaptura.findViewById(R.id.btn_capturar_popup);
        botao.setOnClickListener(v -> {
            if (idThiltapePopupAberto != null) {
                capturarThiltape(idThiltapePopupAberto);
            }
        });
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
            configurarListenersDeMarker(mapLibreMap);
            verificarPedirPermissoes();
        });
    }

    private void configurarListenersDeMarker(@NonNull MapLibreMap mapLibreMap) {
        mapLibreMap.setOnMarkerClickListener(marker -> {
            ScanItem item = dadosDoMarker.get(marker);
            if (item != null) {
                mostrarPopup(item);
            }
            return true; // suprime InfoWindow padrao
        });
        mapLibreMap.addOnMapClickListener(point -> {
            fecharPopup();
            return false;
        });
        mapLibreMap.addOnCameraMoveListener(() -> {
            if (idThiltapePopupAberto != null) {
                atualizarPosicaoPopup();
            }
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

        raioCapturaMetros = r.getRaioCapturaMetros();
        sincronizarMarkers(r.getProximos());

        List<ThiltapeProximo> lista = new ArrayList<>(r.getProximos().size() + r.getCapturados().size());
        for (ScanItem p : r.getProximos()) {
            ThiltapePublico t = p.getThiltape();
            String url = ThiltapesUrls.urlImagemAbsoluta(t.getImagemUrl());
            lista.add(new ThiltapeProximo(
                    t.getId(),
                    url,
                    false,
                    (float) p.getDistanciaMetros(),
                    false,
                    false,
                    t.getNome(),
                    t.getPontuacao()
            ));
        }
        for (ScanItem c : r.getCapturados()) {
            ThiltapePublico t = c.getThiltape();
            String url = ThiltapesUrls.urlImagemAbsoluta(t.getImagemUrl());
            lista.add(new ThiltapeProximo(
                    t.getId(),
                    url,
                    false,
                    0f,
                    true,
                    true,
                    t.getNome(),
                    t.getPontuacao()
            ));
        }
        ultimaListaProximos = lista;
        atualizarListaRecycler(lista);
    }

    /**
     * Mantem em {@link #markersCaptura} apenas os thiltapes dentro do raio de captura informados pelo scan
     * mais recente: adiciona os novos, remove os que sairam.
     */
    private void sincronizarMarkers(@NonNull List<ScanItem> proximos) {
        if (map == null) {
            return;
        }
        Set<Integer> idsAgora = new HashSet<>();
        for (ScanItem p : proximos) {
            if (p.getDistanciaMetros() <= raioCapturaMetros) {
                int id = p.getThiltape().getId();
                idsAgora.add(id);
                if (!markersCaptura.containsKey(id)) {
                    adicionarMarker(p);
                }
            }
        }
        Iterator<Map.Entry<Integer, Marker>> it = markersCaptura.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Marker> entry = it.next();
            if (!idsAgora.contains(entry.getKey())) {
                Marker marker = entry.getValue();
                if (Objects.equals(idThiltapePopupAberto, entry.getKey())) {
                    fecharPopup();
                }
                map.removeMarker(marker);
                dadosDoMarker.remove(marker);
                it.remove();
            }
        }
    }

    private void adicionarMarker(@NonNull ScanItem item) {
        if (map == null) {
            return;
        }
        ThiltapePublico t = item.getThiltape();
        Marker marker = map.addMarker(new MarkerOptions()
                .position(new LatLng(t.getLat(), t.getLng()))
                .title(t.getNome()));
        markersCaptura.put(t.getId(), marker);
        dadosDoMarker.put(marker, item);
    }

    private void mostrarPopup(@NonNull ScanItem item) {
        ThiltapePublico t = item.getThiltape();
        idThiltapePopupAberto = t.getId();

        TextView nome = popupCaptura.findViewById(R.id.nome_popup_thiltape);
        ImageView imagem = popupCaptura.findViewById(R.id.imagem_popup_thiltape);
        Button botao = popupCaptura.findViewById(R.id.btn_capturar_popup);

        nome.setText(t.getNome());
        Glide.with(this)
                .load(ThiltapesUrls.urlImagemAbsoluta(t.getImagemUrl()))
                .into(imagem);
        botao.setEnabled(true);

        popupCaptura.setVisibility(View.VISIBLE);
        atualizarPosicaoPopup();
    }

    private void atualizarPosicaoPopup() {
        if (map == null || idThiltapePopupAberto == null) {
            return;
        }
        Marker marker = markersCaptura.get(idThiltapePopupAberto);
        if (marker == null) {
            return;
        }
        PointF tela = map.getProjection().toScreenLocation(marker.getPosition());
        Runnable aplicar = () -> {
            popupCaptura.setTranslationX(tela.x - popupCaptura.getWidth() / 2f);
            popupCaptura.setTranslationY(tela.y - popupCaptura.getHeight()
                    - ThiltapesImagemConstantes.OFFSET_VERTICAL_POPUP_PX);
        };
        if (popupCaptura.getWidth() == 0 || popupCaptura.getHeight() == 0) {
            popupCaptura.post(aplicar);
        } else {
            aplicar.run();
        }
    }

    private void fecharPopup() {
        if (idThiltapePopupAberto == null) {
            return;
        }
        idThiltapePopupAberto = null;
        popupCaptura.setVisibility(View.INVISIBLE);
        Button botao = popupCaptura.findViewById(R.id.btn_capturar_popup);
        botao.setEnabled(true);
    }

    private void capturarThiltape(int thiltapeId) {
        if (localizacaoAtual == null) {
            return;
        }
        Button botao = popupCaptura.findViewById(R.id.btn_capturar_popup);
        botao.setEnabled(false);
        try {
            ThiltapesApi.postCapturar(
                    this,
                    jogoId,
                    thiltapeId,
                    localizacaoAtual.getLatitude(),
                    localizacaoAtual.getLongitude(),
                    corpo -> aoCapturarSucesso(thiltapeId),
                    this::aoErroCaptura);
        } catch (org.json.JSONException e) {
            Log.w(TAG, "captura: build body", e);
            botao.setEnabled(true);
        }
    }

    private void aoCapturarSucesso(int thiltapeId) {
        fecharPopup();

        Marker marker = markersCaptura.remove(thiltapeId);
        if (marker != null) {
            dadosDoMarker.remove(marker);
            if (map != null) {
                map.removeMarker(marker);
            }
        }

        ThiltapesDesbloqueioPrefs.adicionarVarios(this, jogoId,
                Collections.singletonList(thiltapeId));

        List<ThiltapeProximo> filtrada = new ArrayList<>(ultimaListaProximos.size());
        for (ThiltapeProximo p : ultimaListaProximos) {
            if (p.getThiltapeId() != thiltapeId) {
                filtrada.add(p);
            }
        }
        ultimaListaProximos = filtrada;
        atualizarListaRecycler(filtrada);

        carregarPontuacaoJogo();
    }

    private void aoErroCaptura(@NonNull VolleyError e) {
        Button botao = popupCaptura.findViewById(R.id.btn_capturar_popup);
        botao.setEnabled(true);

        NetworkResponse resp = e.networkResponse;
        int statusCode = (resp != null) ? resp.statusCode : -1;
        int mensagemId;
        if (statusCode == 422) {
            mensagemId = R.string.msg_captura_fora_de_alcance;
        } else if (statusCode == 409) {
            mensagemId = R.string.msg_captura_ja_coletado;
        } else {
            mensagemId = R.string.msg_captura_falhou;
        }
        Toast.makeText(this, mensagemId, Toast.LENGTH_SHORT).show();
        Log.w(TAG, "captura erro status=" + statusCode + ": " + e);
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
        markersCaptura.clear();
        dadosDoMarker.clear();
        ThiltapesCacheBitmapLru.limpar();
        mapView.onDestroy();
    }
}
