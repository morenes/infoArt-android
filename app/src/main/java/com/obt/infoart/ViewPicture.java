package com.obt.infoart;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import android.media.MediaPlayer.OnPreparedListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnticipateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class ViewPicture extends Activity implements OnPreparedListener,
        MediaController.MediaPlayerControl {
    private static final String TAG = "AudioPlayer";
    private static final int SECOND = 1000;
    private MediaPlayer mediaPlayer;
    private MediaController mediaController;
    private Handler handler = new Handler();
    private String clave;
    private Byte key;
    public Context context;
    public static boolean OnView = false;
    private boolean started = false;
    private int pos = -1;
    private View ant;
    private View sig;
    private int indice = 0;
    private File archivoImageVarias;
    private boolean parado;
    private String nombre;
    private int idioma;
    private Res res;

    @Override
    protected void onDestroy() {
        //FRAME
        Frame.getInstance().setIdEsc((byte)0);
        super.onDestroy();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        res=Res.getInstance();
        idioma=res.getIdioma();

        OnView = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.picture_layout);
        ant = findViewById(R.id.button1);
        sig = findViewById(R.id.button2);

        context = this;

        //ID DEL CUADRO
        Bundle extras = getIntent().getExtras();
        clave = extras.getString("clave");
        key=res.mapaNumerosInv[0].get(clave);

        //FRAME
        Frame frame=Frame.getInstance();
        frame.setTipo(extras.getByte("tipo"));
        frame.setIdEsc(key);
        frame.setInicio();

        EditText text1 = (EditText) ViewPicture.this.findViewById(R.id.text2);
        res.obtenerListado();
        //OBTENER IMAGEN, TEXTO Y AUDIO
        indice = 1;
        nombre=res.getNombre();
        archivoImageVarias = new File(res.obtenerDir()
                + "/Museum/" + nombre + "/" + clave + "-" + indice
                + Res.EX_IMAGEN);
        File archivoImageUnica = new File(res.obtenerDir()
                + "/Museum/" + nombre + "/" + clave
                + Res.EX_IMAGEN);

        String texto;
        if (res.existeFichero(archivoImageVarias)) {
            texto = res.obtenerText("/Museum/" + nombre + "/INFO" + idioma + "/IDIOMA" + idioma + "-" + clave + "-1");
            establecerTexto(texto);
            establecerImage(clave + "-1");
            establecerAudio(clave + "-1", true);
            sig.setVisibility(View.VISIBLE);
            ant.setVisibility(View.INVISIBLE);
        } else if (res.existeFichero(archivoImageUnica)) {
            texto = res.obtenerText("/Museum/" + nombre + "/INFO" + idioma + "/IDIOMA" + idioma + "-" + clave);
            establecerTexto(texto);
            establecerAudio(clave, true);
            establecerImage(clave);
            sig.setVisibility(View.INVISIBLE);
            ant.setVisibility(View.INVISIBLE);
        } else {
            texto = getResources().getString(R.string.cargando);
            text1.setText(texto);
        }
        comprobarLikes();
        text1.setKeyListener(null);
    }

    public void comprobarLikes() {
        if (res.getFavoritos().contains(key)) {
            findViewById(R.id.buttonLike).animate().setDuration(0)
                    .setInterpolator(new AnticipateInterpolator())
                    .scaleXBy(0.5f).scaleYBy(0.5f);
        }
    }

    //BOTON DE ME GUSTA, ANTERIOR Y POSTERIOR
    public void onRangingClickedView(View view){
        if (view == ant) { //foto anterior
            indice--;
            sig.setVisibility(View.VISIBLE);
            if (indice == 1) {
                ant.setVisibility(View.INVISIBLE);
            }

            String texto = res.obtenerText("/Museum/" + nombre + "/INFO" + idioma + "/IDIOMA" + idioma + "-" + clave + "-" + indice);
            establecerTexto(texto);
            establecerImage(clave + "-" + indice);
            pararAudio();
            establecerAudio(clave + "-" + indice, true);
        } else if (view == sig) { //foto siguiente
            indice++;
            ant.setVisibility(View.VISIBLE);
            archivoImageVarias = new File(res.obtenerDir()
                    + "/Museum/" + nombre + "/" + clave + "-" + (indice + 1)
                    + Res.EX_IMAGEN);
            if (!res.existeFichero(archivoImageVarias)) {
                sig.setVisibility(View.INVISIBLE);
            }
            String texto = res.obtenerText("/Museum/" + nombre + "/INFO" + idioma + "/IDIOMA" + idioma + "-" + clave + "-" + indice);
            establecerTexto(texto);
            establecerImage(clave + "-" + indice);
            pararAudio();
            establecerAudio(clave + "-" + indice, true);

        } else { //like
            int DURACION=500;
            if (!res.getFavoritos().contains(key)) {
                UDP.getInstance().sendLike(key,(byte)1);
                res.addFavorito(key);
                Toast.makeText(
                        getApplicationContext(),
                        getResources().getString(R.string.like),
                        Toast.LENGTH_SHORT).show();
                findViewById(R.id.buttonLike).animate().setDuration(DURACION)
                        .setInterpolator(new AnticipateInterpolator())
                        .scaleXBy(0.5f).scaleYBy(0.5f);
            } else {
                UDP.getInstance().sendLike(key,(byte)0);
                res.removeFavorito(key);
                Toast.makeText(
                        getApplicationContext(),
                        getResources().getString(R.string.dislike),
                        Toast.LENGTH_SHORT).show();
                findViewById(R.id.buttonLike).animate().setDuration(DURACION)
                        .setInterpolator(new AnticipateInterpolator())
                        .scaleXBy(-0.5f).scaleYBy(-0.5f);
            }
        }
    }

    public void establecerTexto(final String text) {
        String[] cadenas = text.split(">");
        TextView text3 = (TextView) ViewPicture.this
                .findViewById(R.id.text3);
        if (cadenas[1].equals("") || cadenas[1].equals(" ")) text3.setText(cadenas[0]);
        else text3.setText(cadenas[0] + ", " + cadenas[1]);
        cadenas[2] = cadenas[2].replace('<', '\n');
        EditText text2 = (EditText) ViewPicture.this
                .findViewById(R.id.text2);
        text2.setText(cadenas[2]);
    }

    public void establecerImage(final String clave) {
        File imageFile = new File(res.obtenerDir() + "/Museum/"
                + nombre + "/" + clave
                + Res.EX_IMAGEN);
        if (res.existeFichero(imageFile)) {
            Bitmap bitmap = res.obtenerImageWeb(clave);
            ImageView image = (ImageView) ViewPicture.this
                    .findViewById(R.id.imageZoom);
            image.setImageBitmap(res.getRoundedCornerBitmap(bitmap, false, 40));
        }
    }

    public void establecerAudio(final String clave, final boolean bool) {

        started = true;
        String audioFile = res.obtenerDir() + "/Museum/"
                + nombre + "/INFO" + idioma + "/IDIOMA" + idioma + "-" + clave
                + Res.EX_AUDIO;
        if (res.existeFichero(new File(audioFile))) {
            ViewPicture context = this;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(context);
            mediaController = new MediaController(context);

            try {
                mediaPlayer.setDataSource(audioFile);
                mediaPlayer.prepare();
                if (bool) mediaPlayer.start();
                parado = false;
            } catch (IOException e) {
                if (mediaController != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }
                Log.e(TAG, "Could not open file " + audioFile
                        + " for playback.", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        File archivoAudio1 = new File(res.obtenerDir()
                + "/Museum/" + nombre + "/INFO" + idioma + "/IDIOMA" + idioma
                + "-" + clave + Res.EX_AUDIO);
        File archivoAudio2 = new File(res.obtenerDir()
                + "/Museum/" + nombre + "/INFO" + idioma + "/IDIOMA" + idioma
                + "-" + clave + "-" + indice + Res.EX_AUDIO);

        if (!started) {
            if (res.existeFichero(archivoAudio1)) {
                establecerAudio(clave, true);
                mediaPlayer.seekTo(pos);
            } else if (res.existeFichero(archivoAudio2)) {
                establecerAudio(clave + "-" + indice, true);
                mediaPlayer.seekTo(pos);
            }
        }
        OnView = true;
    }

    protected void pararAudio() {
        parado = true;
        if (mediaController != null) {
            pos = mediaPlayer.getCurrentPosition();
            mediaController.hide();
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaController = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        started = false;
        pararAudio();
        OnView = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (mediaController != null)
            if (!parado) mediaController.show(30 * SECOND);
        return false;
    }

    // --MediaPlayerControl
    // methods----------------------------------------------------
    public void start() {
        mediaPlayer.start();
    }

    public void pause() {
        mediaPlayer.pause();
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public void seekTo(int i) {
        mediaPlayer.seekTo(i);
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public int getBufferPercentage() {
        return 0;
    }

    public boolean canPause() {
        return true;
    }

    public boolean canSeekBackward() {
        return true;
    }

    public boolean canSeekForward() {
        return true;
    }

    // --------------------------------------------------------------------------------

    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onPrepared");
        mediaController.setMediaPlayer(this);
        mediaController.setAnchorView(findViewById(R.id.main_audio_view));

        handler.post(new Runnable() {
            public void run() {
                mediaController.setEnabled(true);
                mediaController.show(60 * SECOND);
            }
        });
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}