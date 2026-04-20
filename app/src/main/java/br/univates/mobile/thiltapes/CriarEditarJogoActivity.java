package br.univates.mobile.thiltapes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;

/**
 * Cria ou edita jogo e thiltapes ({@code POST/PATCH /jogos}, {@code POST/PATCH/DELETE /thiltapes}).
 */
public class CriarEditarJogoActivity extends AppCompatActivity {

    public static final String EXTRA_JOGO_ID = "jogo_id";

    private static final int REQ_CAMERA = 71;
    private static final int REQ_PERM_CAM = 72;
    private static final int REQ_PERM_LOC = 73;

    private EditText etNomeJogo;
    private LinearLayout container;
    private Button btnSalvar;
    private Button btnExcluir;
    private View linhaFotoAlvo;

    /** Id do jogo no servidor; {@code -1} cria novo. */
    private int jogoId = -1;
    private final Set<Integer> idsServidorCarregados = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_criar_editar_jogo);
        ThiltapesBarraSistema.aplicarNaRaiz(this, R.id.root_criar_editar_jogo);

        etNomeJogo = findViewById(R.id.et_nome_jogo);
        container = findViewById(R.id.container_thiltapes_edicao);
        btnSalvar = findViewById(R.id.btn_salvar_jogo);
        btnExcluir = findViewById(R.id.btn_excluir_jogo);
        findViewById(R.id.btn_adicionar_thiltape).setOnClickListener(v -> adicionarLinhaVazia());

        jogoId = getIntent().getIntExtra(EXTRA_JOGO_ID, -1);
        btnExcluir.setVisibility(jogoId > 0 ? View.VISIBLE : View.GONE);

        btnSalvar.setOnClickListener(v -> salvar());
        btnExcluir.setOnClickListener(v -> excluirJogo());

        if (jogoId > 0) {
            carregarJogoExistente();
        } else {
            adicionarLinhaVazia();
        }
    }

    private void carregarJogoExistente() {
        ThiltapesApi.getJogo(this, jogoId,
                jogo -> {
                    try {
                        etNomeJogo.setText(jogo.getString("nome"));
                    } catch (JSONException e) {
                        Toast.makeText(this, R.string.msg_erro_parse, Toast.LENGTH_SHORT).show();
                    }
                },
                e -> Toast.makeText(this, R.string.msg_erro_rede, Toast.LENGTH_SHORT).show());

        ThiltapesApi.listarThiltapesDoJogo(this, jogoId,
                arr -> {
                    try {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);
                            adicionarLinhaPreenchida(o);
                        }
                        if (arr.length() == 0) {
                            adicionarLinhaVazia();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, R.string.msg_erro_parse, Toast.LENGTH_SHORT).show();
                    }
                },
                e -> Toast.makeText(this, R.string.msg_erro_rede, Toast.LENGTH_SHORT).show());
    }

    private void adicionarLinhaVazia() {
        View linha = inflarLinha();
        container.addView(linha);
    }

    private void adicionarLinhaPreenchida(@NonNull JSONObject o) throws JSONException {
        View linha = inflarLinha();
        int sid = o.getInt("id");
        idsServidorCarregados.add(sid);
        linha.setTag(R.id.tag_thiltape_server_id, sid);

        EditText nome = linha.findViewById(R.id.et_nome_thiltape_edicao);
        EditText pts = linha.findViewById(R.id.et_pontos_thiltape_edicao);
        EditText lat = linha.findViewById(R.id.et_lat_edicao);
        EditText lng = linha.findViewById(R.id.et_lng_edicao);

        nome.setText(o.optString("nome", ""));
        if (o.has("pontuacao") && !o.isNull("pontuacao")) {
            pts.setText(String.valueOf(o.getInt("pontuacao")));
        }
        lat.setText(formatarCoord(o.optDouble("lat")));
        lng.setText(formatarCoord(o.optDouble("lng")));

        ImageView iv = linha.findViewById(R.id.iv_preview_thiltape_edicao);
        linha.setTag(R.id.tag_thiltape_img_origem, Boolean.TRUE);
        container.addView(linha);

        ThiltapesImagemApiGlide.carregarImagemThiltape(iv, sid, linha);
    }

    @NonNull
    private String formatarCoord(double v) {
        return String.format(Locale.US, "%.6f", v);
    }

    private View inflarLinha() {
        LayoutInflater inf = LayoutInflater.from(this);
        View linha = inf.inflate(R.layout.item_edicao_thiltape, container, false);
        linha.findViewById(R.id.btn_foto_thiltape_edicao).setOnClickListener(v -> abrirCamera(linha));
        linha.findViewById(R.id.btn_gps_thiltape_edicao).setOnClickListener(v -> preencherGpsNaLinha(linha));
        linha.findViewById(R.id.btn_remover_thiltape_edicao).setOnClickListener(v -> {
            Object sidObj = linha.getTag(R.id.tag_thiltape_server_id);
            if (sidObj instanceof Integer) {
                idsServidorCarregados.remove(sidObj);
            }
            container.removeView(linha);
        });
        return linha;
    }

    private void abrirCamera(@NonNull View linha) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_PERM_CAM);
            linhaFotoAlvo = linha;
            return;
        }
        linhaFotoAlvo = linha;
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (camera.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(camera, REQ_CAMERA);
        }
    }

    @SuppressLint("MissingPermission")
    private void preencherGpsNaLinha(@NonNull View linha) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_PERM_LOC);
            return;
        }
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location loc = null;
        if (lm != null) {
            loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) {
                loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }
        if (loc == null) {
            Toast.makeText(this, R.string.msg_sem_gps, Toast.LENGTH_SHORT).show();
            return;
        }
        EditText etLat = linha.findViewById(R.id.et_lat_edicao);
        EditText etLng = linha.findViewById(R.id.et_lng_edicao);
        etLat.setText(formatarCoord(loc.getLatitude()));
        etLng.setText(formatarCoord(loc.getLongitude()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] res) {
        super.onRequestPermissionsResult(requestCode, perms, res);
        if (requestCode == REQ_PERM_CAM && linhaFotoAlvo != null
                && res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) {
            abrirCamera(linhaFotoAlvo);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CAMERA && resultCode == RESULT_OK && data != null && linhaFotoAlvo != null) {
            Bundle ext = data.getExtras();
            if (ext != null) {
                Bitmap bmp = (Bitmap) ext.get("data");
                if (bmp != null) {
                    ImageView iv = linhaFotoAlvo.findViewById(R.id.iv_preview_thiltape_edicao);
                    iv.setImageBitmap(bmp);
                    linhaFotoAlvo.setTag(R.id.tag_thiltape_bitmap, bmp);
                    linhaFotoAlvo.setTag(R.id.tag_thiltape_img_origem, Boolean.FALSE);
                }
            }
        }
    }

    private void salvar() {
        String nomeJogo = etNomeJogo.getText().toString().trim();
        if (nomeJogo.isEmpty()) {
            Toast.makeText(this, R.string.msg_preencha_nome_jogo, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!validarLinhasParaSalvar()) {
            return;
        }

        if (jogoId < 0) {
            criarJogo(nomeJogo, this::executarPipelineAposNomeJogo);
        } else {
            atualizarNomeJogo(nomeJogo, this::executarPipelineAposNomeJogo);
        }
    }

    private void executarPipelineAposNomeJogo() {
        sincronizarRemocoes(() -> {
            try {
                sincronizarCriacoesEAtualizacoes(() -> runOnUiThread(() -> {
                    Toast.makeText(this, R.string.msg_salvo, Toast.LENGTH_SHORT).show();
                    finish();
                }));
            } catch (JSONException e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.msg_erro_parse, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void criarJogo(@NonNull String nome, @NonNull Runnable depois) {
        try {
            ThiltapesApi.postJogo(this, nome,
                    res -> {
                        try {
                            jogoId = res.getInt("id");
                        } catch (JSONException e) {
                            runOnUiThread(() -> Toast.makeText(this, R.string.msg_erro_parse, Toast.LENGTH_SHORT).show());
                            return;
                        }
                        depois.run();
                    },
                    e -> runOnUiThread(() -> Toast.makeText(this, R.string.msg_erro_rede, Toast.LENGTH_SHORT).show())
            );
        } catch (JSONException e) {
            Toast.makeText(this, R.string.msg_erro_parse, Toast.LENGTH_SHORT).show();
        }
    }

    private void atualizarNomeJogo(@NonNull String nome, @NonNull Runnable depois) {
        try {
            ThiltapesApi.patchJogo(this, jogoId, nome,
                    res -> depois.run(),
                    e -> runOnUiThread(() -> Toast.makeText(this, R.string.msg_erro_rede, Toast.LENGTH_SHORT).show())
            );
        } catch (JSONException e) {
            Toast.makeText(this, R.string.msg_erro_parse, Toast.LENGTH_SHORT).show();
        }
    }

    private void sincronizarRemocoes(@NonNull Runnable depois) {
        Set<Integer> aindaPresentes = new HashSet<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View linha = container.getChildAt(i);
            Object sidObj = linha.getTag(R.id.tag_thiltape_server_id);
            if (sidObj instanceof Integer) {
                aindaPresentes.add((Integer) sidObj);
            }
        }
        Set<Integer> remover = new HashSet<>(idsServidorCarregados);
        remover.removeAll(aindaPresentes);
        if (remover.isEmpty()) {
            depois.run();
            return;
        }
        encadearDeletes(remover.iterator(), depois);
    }

    private void encadearDeletes(@NonNull java.util.Iterator<Integer> it, @NonNull Runnable fim) {
        if (!it.hasNext()) {
            fim.run();
            return;
        }
        int id = it.next();
        ThiltapesApi.deleteThiltape(this, id,
                r -> encadearDeletes(it, fim),
                e -> runOnUiThread(() -> Toast.makeText(this, R.string.msg_erro_rede, Toast.LENGTH_SHORT).show()));
    }

    private void sincronizarCriacoesEAtualizacoes(@NonNull Runnable fim) throws JSONException {
        Queue<View> fila = new ArrayDeque<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            fila.add(container.getChildAt(i));
        }
        processarFilaLinhas(fila, fim);
    }

    private void processarFilaLinhas(@NonNull Queue<View> fila, @NonNull Runnable fim) throws JSONException {
        if (fila.isEmpty()) {
            fim.run();
            return;
        }
        View linha = fila.poll();
        if (linha == null) {
            fim.run();
            return;
        }
        Object sidObj = linha.getTag(R.id.tag_thiltape_server_id);
        if (sidObj instanceof Integer) {
            int sid = (Integer) sidObj;
            JSONObject patch = montarPatchThiltape(linha);
            ThiltapesApi.patchThiltape(this, sid, patch,
                    r -> {
                        try {
                            processarFilaLinhas(fila, fim);
                        } catch (JSONException e) {
                            runOnUiThread(() -> Toast.makeText(this, R.string.msg_erro_parse, Toast.LENGTH_SHORT).show());
                        }
                    },
                    e -> runOnUiThread(() -> Toast.makeText(this, R.string.msg_erro_rede, Toast.LENGTH_SHORT).show()));
        } else {
            JSONObject post = montarPostThiltape(linha);
            if (post == null) {
                processarFilaLinhas(fila, fim);
                return;
            }
            ThiltapesApi.criarThiltapeNoJogo(this, jogoId, post,
                    res -> {
                        try {
                            processarFilaLinhas(fila, fim);
                        } catch (JSONException e) {
                            runOnUiThread(() -> Toast.makeText(this, R.string.msg_erro_parse, Toast.LENGTH_SHORT).show());
                        }
                    },
                    e -> runOnUiThread(() -> Toast.makeText(this, R.string.msg_erro_rede, Toast.LENGTH_SHORT).show()));
        }
    }

    @Nullable
    private JSONObject montarPostThiltape(@NonNull View linha) throws JSONException {
        EditText nome = linha.findViewById(R.id.et_nome_thiltape_edicao);
        EditText pts = linha.findViewById(R.id.et_pontos_thiltape_edicao);
        EditText lat = linha.findViewById(R.id.et_lat_edicao);
        EditText lng = linha.findViewById(R.id.et_lng_edicao);
        Bitmap bmp = obterBitmap(linha);
        if (bmp == null) {
            return null;
        }
        JSONObject o = new JSONObject();
        o.put("nome", nome.getText().toString().trim());
        int p = parseIntOuPadrao(pts.getText().toString().trim(), 1);
        o.put("pontuacao", Math.max(1, p));
        o.put("lat", Double.parseDouble(lat.getText().toString().trim()));
        o.put("lng", Double.parseDouble(lng.getText().toString().trim()));
        o.put("img_b64", jpegBase64(bmp));
        return o;
    }

    @NonNull
    private JSONObject montarPatchThiltape(@NonNull View linha) throws JSONException {
        EditText nome = linha.findViewById(R.id.et_nome_thiltape_edicao);
        EditText pts = linha.findViewById(R.id.et_pontos_thiltape_edicao);
        EditText lat = linha.findViewById(R.id.et_lat_edicao);
        EditText lng = linha.findViewById(R.id.et_lng_edicao);
        JSONObject o = new JSONObject();
        o.put("nome", nome.getText().toString().trim());
        int p = parseIntOuPadrao(pts.getText().toString().trim(), 1);
        o.put("pontuacao", Math.max(1, p));
        o.put("lat", Double.parseDouble(lat.getText().toString().trim()));
        o.put("lng", Double.parseDouble(lng.getText().toString().trim()));
        Bitmap bmp = obterBitmap(linha);
        Object origem = linha.getTag(R.id.tag_thiltape_img_origem);
        boolean veioDoServidorSemPreview = Boolean.TRUE.equals(origem);
        if (bmp != null && !veioDoServidorSemPreview) {
            o.put("img_b64", jpegBase64(bmp));
        }
        return o;
    }

    private static int parseIntOuPadrao(String s, int padrao) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return padrao;
        }
    }

    @Nullable
    private Bitmap obterBitmap(@NonNull View linha) {
        Object b = linha.getTag(R.id.tag_thiltape_bitmap);
        if (b instanceof Bitmap) {
            return (Bitmap) b;
        }
        return null;
    }

    @NonNull
    private static String jpegBase64(@NonNull Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    private boolean validarLinhasParaSalvar() {
        for (int i = 0; i < container.getChildCount(); i++) {
            View linha = container.getChildAt(i);
            EditText nome = linha.findViewById(R.id.et_nome_thiltape_edicao);
            EditText pts = linha.findViewById(R.id.et_pontos_thiltape_edicao);
            EditText lat = linha.findViewById(R.id.et_lat_edicao);
            EditText lng = linha.findViewById(R.id.et_lng_edicao);
            if (nome.getText().toString().trim().isEmpty()
                    || pts.getText().toString().trim().isEmpty()
                    || lat.getText().toString().trim().isEmpty()
                    || lng.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, R.string.msg_thiltape_incompleto, Toast.LENGTH_LONG).show();
                return false;
            }
            boolean novo = linha.getTag(R.id.tag_thiltape_server_id) == null;
            if (novo && obterBitmap(linha) == null) {
                Toast.makeText(this, R.string.msg_thiltape_incompleto, Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    private void excluirJogo() {
        if (jogoId < 0) {
            return;
        }
        ThiltapesApi.deleteJogo(this, jogoId,
                r -> {
                    Toast.makeText(this, R.string.msg_salvo, Toast.LENGTH_SHORT).show();
                    finish();
                },
                e -> Toast.makeText(this, R.string.msg_erro_rede, Toast.LENGTH_SHORT).show());
    }
}
