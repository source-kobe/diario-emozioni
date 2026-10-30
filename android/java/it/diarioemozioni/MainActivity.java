package it.diarioemozioni;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Diario delle Emozioni — guscio nativo attorno all'interfaccia in assets/index.html.
 * Tutto resta sul dispositivo; l'unica rete è la richiesta a GitHub per gli
 * aggiornamenti, fatta da qui e solo su richiesta.
 */
public class MainActivity extends Activity {

    private WebView web;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle stato) {
        super.onCreate(stato);

        web = new WebView(this);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        setContentView(web);
        aggiornaTask();

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);   // localStorage: qui vivono le voci del diario
        s.setAllowFileAccess(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setTextZoom(100);

        web.addJavascriptInterface(new Ponte(), "DiarioAndroid");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (u != null && u.getScheme() != null && u.getScheme().startsWith("http")) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, u));
                    } catch (Exception ignora) { /* nessun browser */ }
                    return true;
                }
                return false;
            }
        });

        // <input type="file"> dell'importazione: senza questo la WebView lo ignora
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> callback,
                                             FileChooserParams parametri) {
                if (attesaFile != null) attesaFile.onReceiveValue(null);
                attesaFile = callback;
                // "*/*": a seconda del telefono un .json può risultare di tipi diversi,
                // il contenuto lo controlla poi la pagina
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try {
                    startActivityForResult(i, SCEGLI_FILE);
                } catch (Exception e) {
                    attesaFile = null;
                    return false;
                }
                return true;
            }
        });

        web.loadUrl("file:///android_asset/index.html#" + tema());
    }

    private static final int SCEGLI_FILE = 1;
    private ValueCallback<Uri[]> attesaFile;

    @Override
    protected void onActivityResult(int richiesta, int esito, Intent dati) {
        if (richiesta != SCEGLI_FILE) {
            super.onActivityResult(richiesta, esito, dati);
            return;
        }
        if (attesaFile == null) return;
        Uri u = (esito == RESULT_OK && dati != null) ? dati.getData() : null;
        attesaFile.onReceiveValue(u != null ? new Uri[] { u } : null);
        attesaFile = null;
    }

    /** "dark" o "light", secondo l'impostazione di sistema. */
    private String tema() {
        int m = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return m == Configuration.UI_MODE_NIGHT_YES ? "dark" : "light";
    }

    @Override
    public void onConfigurationChanged(Configuration nuova) {
        super.onConfigurationChanged(nuova);
        web.evaluateJavascript(
                "document.documentElement.setAttribute('data-theme','" + tema() + "')", null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        chiama("window.DiarioSospeso && window.DiarioSospeso()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        chiama("window.DiarioRipreso && window.DiarioRipreso()");
    }

    private void chiama(String js) {
        if (web != null) web.evaluateJavascript(js, null);
    }

    /* ---- travestimento ---------------------------------------------------- */

    private static final String ALIAS_DIARIO = "it.diarioemozioni.Diario";
    private static final String ALIAS_SCONTI = "it.diarioemozioni.Sconti";

    /** Vero quando nella home l'app compare come "Sconti". */
    private boolean camuffato() {
        int stato = getPackageManager().getComponentEnabledSetting(
                new ComponentName(getPackageName(), ALIAS_SCONTI));
        return stato == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    private void scambiaIngressi(boolean sconti) {
        PackageManager pm = getPackageManager();
        // prima accendo il nuovo, poi spengo il vecchio: mai un istante senza icona
        pm.setComponentEnabledSetting(
                new ComponentName(getPackageName(), sconti ? ALIAS_SCONTI : ALIAS_DIARIO),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(
                new ComponentName(getPackageName(), sconti ? ALIAS_DIARIO : ALIAS_SCONTI),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    /** Nome e icona nella schermata delle app recenti, coerenti col travestimento. */
    @SuppressWarnings("deprecation")
    private void aggiornaTask() {
        boolean sconti = camuffato();
        String nome = getString(sconti ? R.string.nome_sconti : R.string.app_name);
        Bitmap icona = null;
        try {
            Drawable d = getDrawable(sconti ? R.mipmap.ic_sconti : R.mipmap.ic_launcher);
            if (d != null) {
                icona = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(icona);
                d.setBounds(0, 0, 192, 192);
                d.draw(c);
            }
        } catch (Exception ignora) { /* resta l'icona predefinita */ }
        setTaskDescription(new ActivityManager.TaskDescription(nome, icona));
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private void mostraBiometria() {
        BiometricPrompt prompt = new BiometricPrompt.Builder(this)
                .setTitle(camuffato() ? getString(R.string.nome_sconti) : "Diario delle Emozioni")
                .setSubtitle("Accedi con l’impronta")
                .setNegativeButton("Usa il PIN", getMainExecutor(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int quale) {
                                esitoBiometria(false, null);
                            }
                        })
                .build();

        prompt.authenticate(new CancellationSignal(), getMainExecutor(),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) {
                        esitoBiometria(true, null);
                    }

                    @Override
                    public void onAuthenticationError(int codice, CharSequence messaggio) {
                        esitoBiometria(false, null);
                    }
                });
    }

    private void esitoBiometria(final boolean riuscito, final String messaggio) {
        final String m = messaggio == null
                ? "null"
                : "\"" + messaggio.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chiama("window.DiarioBiometria && window.DiarioBiometria(" + riuscito + ", " + m + ")");
            }
        });
    }

    /* ---- aggiornamenti da GitHub ------------------------------------------ */

    /** L'ultima release: {versione, pagina, apk} oppure {errore: "nessuna" | "rete"}. */
    private String ultimaRelease() {
        JSONObject esito = new JSONObject();
        HttpURLConnection c = null;
        try {
            URL u = new URL("https://api.github.com/repos/"
                    + getString(R.string.repo_github) + "/releases/latest");
            c = (HttpURLConnection) u.openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(10000);
            c.setRequestProperty("Accept", "application/vnd.github+json");
            if (c.getResponseCode() != 200) {   // 404: nessuna release pubblicata
                esito.put("errore", "nessuna");
                return esito.toString();
            }
            InputStream in = c.getInputStream();
            ByteArrayOutputStream corpo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0; ) corpo.write(buf, 0, n);
            in.close();

            JSONObject r = new JSONObject(corpo.toString("UTF-8"));
            esito.put("versione", r.optString("tag_name"));
            esito.put("pagina", r.optString("html_url"));
            JSONArray allegati = r.optJSONArray("assets");
            for (int i = 0; allegati != null && i < allegati.length(); i++) {
                JSONObject a = allegati.getJSONObject(i);
                if (a.optString("name").toLowerCase().endsWith(".apk")) {
                    esito.put("apk", a.optString("browser_download_url"));
                    break;
                }
            }
            return esito.toString();
        } catch (Exception e) {
            return "{\"errore\":\"rete\"}";
        } finally {
            if (c != null) c.disconnect();
        }
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript(
                "!!(window.DiarioIndietro && window.DiarioIndietro())",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String gestito) {
                        if (!"true".equals(gestito)) {
                            finish();
                        }
                    }
                });
    }

    /** Ponte fra la pagina e Android: condivisione e salvataggio file. */
    private class Ponte {

        @JavascriptInterface
        public void condividi(final String testo) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_SUBJECT, "Diario delle emozioni");
                    i.putExtra(Intent.EXTRA_TEXT, testo);
                    try {
                        startActivity(Intent.createChooser(i, "Invia il diario"));
                    } catch (Exception ignora) { /* nessuna app di condivisione */ }
                }
            });
        }

        /** Durante la rilettura: barre di sistema via e schermo sempre acceso. */
        @JavascriptInterface
        public void schermoIntero(final boolean attivo) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View decor = getWindow().getDecorView();
                    if (attivo) {
                        decor.setSystemUiVisibility(
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                }
            });
        }

        @JavascriptInterface
        public boolean camuffamentoAttivo() {
            try { return camuffato(); } catch (Exception e) { return false; }
        }

        /** Scambia l'ingresso in home; restituisce lo stato effettivo dopo il cambio. */
        @JavascriptInterface
        public boolean camuffa(final boolean attiva) {
            try {
                scambiaIngressi(attiva);
            } catch (Exception e) {
                return camuffamentoAttivo();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() { aggiornaTask(); }
            });
            return camuffamentoAttivo();
        }

        /** Il diario non compare nelle app recenti e non si può fotografare. */
        @JavascriptInterface
        public void proteggiSchermo(final boolean attiva) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (attiva) {
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                    } else {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                    }
                }
            });
        }

        /** Impronta o volto: disponibili da Android 10 in su, se già registrati. */
        @JavascriptInterface
        public boolean biometriaDisponibile() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false;
            try {
                BiometricManager bm = getSystemService(BiometricManager.class);
                return bm != null && bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public void chiediBiometria() {
            if (!biometriaDisponibile()) return;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mostraBiometria();
                    } catch (Exception e) {
                        esitoBiometria(false, null);
                    }
                }
            });
        }

        /** La versione installata, da confrontare con l'ultima release. */
        @JavascriptInterface
        public String versione() {
            try {
                return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (Exception e) {
                return "";
            }
        }

        /** Chiede a GitHub l'ultima release; la risposta arriva in window.DiarioAggiornamento. */
        @JavascriptInterface
        public void cercaAggiornamento() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String esito = ultimaRelease();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            chiama("window.DiarioAggiornamento && window.DiarioAggiornamento(" + esito + ")");
                        }
                    });
                }
            }).start();
        }

        /** Apre nel browser il download dell'aggiornamento: solo indirizzi di GitHub. */
        @JavascriptInterface
        public void apriAggiornamento(final String indirizzo) {
            if (indirizzo == null || !indirizzo.startsWith("https://github.com/")) return;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(indirizzo)));
                    } catch (Exception ignora) { /* nessun browser */ }
                }
            });
        }

        @JavascriptInterface
        public String salvaFile(String nome, String contenuto) {
            byte[] dati = contenuto.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            String tipo = nome.endsWith(".json") ? "application/json" : "text/plain";
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Downloads.DISPLAY_NAME, nome);
                    cv.put(MediaStore.Downloads.MIME_TYPE, tipo);
                    cv.put(MediaStore.Downloads.IS_PENDING, 1);
                    Uri u = getContentResolver()
                            .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (u == null) return "Non sono riuscito a salvare";
                    OutputStream out = getContentResolver().openOutputStream(u);
                    out.write(dati);
                    out.close();
                    cv.clear();
                    cv.put(MediaStore.Downloads.IS_PENDING, 0);
                    getContentResolver().update(u, cv, null, null);
                    return "Salvato in Download";
                }
                File f = new File(getExternalFilesDir(null), nome);
                FileOutputStream out = new FileOutputStream(f);
                out.write(dati);
                out.close();
                return "Salvato in " + f.getAbsolutePath();
            } catch (Exception e) {
                return "Non sono riuscito a salvare";
            }
        }
    }
}
