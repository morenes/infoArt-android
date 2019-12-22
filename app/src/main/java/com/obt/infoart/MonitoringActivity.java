package com.obt.infoart;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;


public class MonitoringActivity extends Activity implements BeaconConsumer {
    protected static final String TAG = "MonitoringActivity";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
    public static boolean fondo = false;
    private boolean cambioIdioma;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring);
        SharedPreferences settings = getSharedPreferences("perfil", MODE_PRIVATE);
        //Museo actual
        settings.edit().putString("ultimo", Res.getInstance().getNombre()).apply();

        ProgressDialog mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getResources().getString(R.string.cargando));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        //BEACONS
        checkPermisions();
        verifyBluetooth();
    }

    //BOTONES
    public void onRangingClicked(View view) {
        Intent myIntent;
        switch (view.getId()) {
            case R.id.Button1:
                if (android.os.Build.VERSION.SDK_INT >= Res.VERSION) {
                    myIntent = new Intent(this, RangingActivity.class);
                    this.startActivity(myIntent);
                } else
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.noBluetooth),
                            Toast.LENGTH_SHORT).show();
                break;

            case R.id.button2:
                runOnUiThread(new Runnable() {
                    public void run() {
                        DialogoNombre();
                    }
                });
                break;

            case R.id.button3:
                myIntent = new Intent(this, ListViewActivity.class);
                myIntent.putExtra("opciones", true);
                this.startActivity(myIntent);
                break;

            case R.id.buttonLan:
                myIntent = new Intent(this, ListViewActivity.class);
                myIntent.putExtra("idioma", true);
                this.startActivity(myIntent);
                cambioIdioma = true;
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.setBackgroundMode(true);
        beaconManager.unbind(this);
        UDP.getInstance().stopLoop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cambioIdioma) cambioIdioma = false;
        //if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(false);

        Res res = Res.getInstance();
        String nombre = res.getNombre();
        if (res.settings == null) res.settings = getSharedPreferences("perfil", MODE_PRIVATE);
        if (!res.settings.getBoolean(nombre + "INFO" + res.getIdioma(), false))
            finish(); //No tenemos las imagenes y audios

        comprobarBuzon();
        cambiarBotones();

        //ESTABLECER FONDO, BUSCAR IMAGENES
        fondo = false;
        if (nombre != null) {
            establecerFondo();
        }
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    public void establecerFondo() {
        Res res = Res.getInstance();
        final String ruta = res.obtenerDir()
                + "/Museum/" + res.getNombre() + "/PORTADA"
                + Res.EX_IMAGEN;
        File archivo = new File(ruta);
        if (res.existeFichero(archivo)) {
            runOnUiThread(new Thread(new Runnable() {
                public void run() {
                    RelativeLayout fondos = (RelativeLayout) findViewById(R.id.fondo);
                    fondos.setBackground(Drawable.createFromPath(ruta));

                }
            }));
            fondo = true;
            res.obtenerListado();
        }
    }

    public void comprobarBuzon() {
        /////COMPROBAR BUZON
        final SharedPreferences settings = Res.getInstance().settings;
        final String texto = settings.getString("BUZON", null);
        if (texto != null) {
            new Thread(new Runnable() {
                public void run() {
                    int i = 0;
                    long res = UDP.getInstance().sendBuzon(texto);
                    while (i < UDP.MAX_INTENTOS && (res != 0)) {
                        res = UDP.getInstance().sendBuzon(texto);
                        i++;
                    }
                    if (res == 0) { //ENVIADO OK
                        settings.edit().remove("BUZON").apply();
                    }
                }
            }).start();
        }
    }

    public void cambiarBotones() {
        runOnUiThread(new Runnable() {
            public void run() {
                Button boton;
                boton = (Button) findViewById(R.id.Button1);
                boton.setText("  " + getResources().getString(R.string.comenzarVisita) + " »  ");
                boton.setVisibility(View.VISIBLE);
                //
                boton = (Button) findViewById(R.id.Button1 + 1);
                boton.setText("  " + getResources().getString(R.string.buscarPorNumero) + " »  ");
                boton.setVisibility(View.VISIBLE);
                //
                boton = (Button) findViewById(R.id.Button1 + 2);
                boton.setText("  " + getResources().getString(R.string.mapasInformacion) + " »  ");
                boton.setVisibility(View.VISIBLE);
                //
                boton = (Button) findViewById(R.id.buttonLan);
                boton.setBackground(Drawable.createFromPath(Res.getInstance().obtenerDir()
                        + "/Museum/IDIOMA"
                        + Res.getInstance().getIdioma() + Res.EX_IMAGEN));
            }
        });
    }

    public void llamarFoto(String id) {
        Intent myIntent = new Intent(this, ViewPicture.class);
        myIntent.putExtra("clave", id);
        myIntent.putExtra("tipo", Frame.SEARCH);
        this.startActivity(myIntent);
    }

    //DIALOGO BUSCAR CUADROS POR NUMERO
    @SuppressLint("InflateParams")
    private void DialogoNombre() {
        LayoutInflater li = LayoutInflater.from(this);
        View prompt = li.inflate(R.layout.prom, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(prompt);

        final EditText numeroCuadro = (EditText) prompt
                .findViewById(R.id.numeroCuadro);
        TextView titulo = (TextView) prompt.findViewById(R.id.titulo);
        titulo.setText(R.string.buscarPorNumero);

        alertDialogBuilder
                .setCancelable(true)
                .setPositiveButton(getResources().getString(R.string.si),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Rescatamos el nombre del EditText y lo mostramos por pantalla
                                String numero = numeroCuadro.getText()
                                        .toString();
                                HashMap<Byte, String> hash = Res.getInstance().mapaNumeros[0];
                                if (hash != null) {
                                    int number = Res.isNumeric(numero);
                                    String resultado = hash.get((byte) number);
                                    if (resultado != null) {
                                        llamarFoto(resultado);
                                    } else
                                        Toast.makeText(
                                                getApplicationContext(),
                                                getResources().getString(R.string.codigoErroneo),
                                                Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    //BEACONS

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                Frame frame=Frame.getInstance();
                for (Beacon beacon: beacons)
                {
                    frame.addBeacon((byte)beacon.getId2().toInt(),Res.getDistance(beacon));
                    Log.e("Beacon", beacon.getId2() + " is about " + (int)(Res.getDistance(beacon)) + " meters");
                }

                IRange range=App.getRangeNotifier();
                if(range!=null) range.didRangeBeaconsInRegion(beacons,region);
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
            }
        }
    }

    public void checkPermisions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons in the background.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @TargetApi(23)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    private void verifyBluetooth() {
        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                Res.setBluetooth(true);
                //LOOP
               /*
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Res.setBluetooth(true);
                        //finish();
                        //System.exit(0);
                    }
                });
                builder.show();
                */
            }

            beaconManager.setForegroundBetweenScanPeriod(500);
            beaconManager.bind(this);
            beaconManager.setBackgroundMode(false);
            UDP.getInstance().startLoop();
        } catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                }
            });
            builder.show();
        }

    }
}
