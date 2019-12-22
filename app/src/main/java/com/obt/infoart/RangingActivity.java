package com.obt.infoart;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Region;

public class RangingActivity extends Activity implements IRange {
    private HashMap<String, int[]> mapList = new HashMap<>();
    private HashMap<String, Integer> mapPunt = new HashMap<>();
    private String[] imagenesOcupadas;
    private static final int TAM = 5;
    private static final int IMAGENES = 8;
    private static final int MAX = 5000;
    private HashSet<Beacon> ultimosBeacons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.ranging);
        findViewById(R.id.frame).setBackground(Drawable.createFromPath(Res.getInstance().obtenerDir()
                + "/Museum/" + Res.getInstance().getNombre() + "/FONDO"
                + Res.EX_IMAGEN));
        inicializacion();
        Res.setBluetooth(true);
        App.setRangeNotifier(this);
        ultimosBeacons = new HashSet<>();
    }

    @Override
    protected void onDestroy() {
        App.setRangeNotifier(null);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    private void inicializacion() {
        HashMap mapa = Res.getInstance().mapaCuadros;
        if (mapa == null) {
            finish();
        } else {
            Set conjunto = mapa.keySet();
            int[] aux;
            for (Object o : conjunto) {
                String string=o.toString();
                aux = new int[TAM];
                for (int i = 0; i < TAM; i++) {
                    aux[i] = MAX;
                }
                mapList.put(string, aux);
                mapPunt.put(string, 0);
            }
            imagenesOcupadas = new String[IMAGENES];

            ////DEMO
            if (Res.getInstance().isDemo()) {
                simulateBeacons();
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.simulando), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onImageClicked(View view) {
        String clave = imagenesOcupadas[view.getId() - R.id.im1];
        if (clave != null) {
            Intent myIntent = new Intent(this, ViewPicture.class);
            myIntent.putExtra("id", view.getId());
            myIntent.putExtra("clave", clave);
            myIntent.putExtra("tipo", Frame.BEACON);
            this.startActivity(myIntent);
        }
    }

    public int tratarBeacon(Beacon beacon) {
        if (Res.getInstance().isDemo())
            return MAX / 20;
        String id = Res.getInfo(beacon);
        Integer punt = mapPunt.get(id);

        int distanciaActual = Res.getDistance(beacon);
        int[] lista = mapList.get(id);
        if (distanciaActual != -1) {
            lista[punt] = distanciaActual;
            mapPunt.put(id, (punt + 1) % TAM);
        }
        int distancia = 0;
        for (int i = 0; i < TAM; i++)
            distancia += lista[i];

        return distancia / TAM;
    }

    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        if (Res.getInstance().isDemo()) return;
        int i = 0;
        Byte idBea;
        Frame frame = Frame.getInstance();
        frame.clearList();
        if (beacons == null || (ultimosBeacons == null)) return;

        ultimosBeacons.addAll(beacons);
        int size = ultimosBeacons.size();
        if (size > 0) {
            int[] distancias = new int[size];
            String[] claves = new String[size];
            int index = 0;
            for (Beacon beacon : ultimosBeacons) {
                claves[index] = Res.getInstance().mapaCuadros.get(Res.getInfo(beacon));
                if (claves[index] != null) {
                    distancias[index] = tratarBeacon(beacon);
                    idBea = Res.getInstance().mapaNumerosInv[0].get(claves[index]);
                    frame.addBeacon(idBea, (byte) distancias[index]);
                    //Log.e("dis-"+claves[index],String.valueOf(distancias[index]));
                    index++;
                }
            }
            int menor;
            int indiceMenor = -1;
            LinkedList<String> aux = new LinkedList<>();
            while (i < IMAGENES) {
                menor = MAX;
                for (int j = 0; j < index; j++)
                    if (distancias[j] < menor) {
                        menor = distancias[j];
                        indiceMenor = j;
                    }
                if (menor == MAX) break;
                if (!aux.contains(claves[indiceMenor])) {
                    imagenesOcupadas[i] = claves[indiceMenor];
                    i++;
                    aux.add(claves[indiceMenor]);
                }
                distancias[indiceMenor] = MAX;
            }
            showInDisplay();
        }
        while (i < IMAGENES) {
            imagenesOcupadas[i] = null;
            i++;
        }
        ultimosBeacons = new HashSet<>(beacons);
    }

    private void showInDisplay() {
        runOnUiThread(new Runnable() {
            public void run() {
                ImageView image;
                for (int i = 0; i < IMAGENES; i++) {
                    if (imagenesOcupadas[i] != null) {
                        image = (ImageView) RangingActivity.this.findViewById(R.id.im1 + i);
                        image.setImageBitmap(Res.getInstance().getRoundedCornerBitmap(
                                Res.getInstance().obtenerImageWeb(imagenesOcupadas[i]), true, 50));
                    } else {
                        image = (ImageView) RangingActivity.this.findViewById(R.id.im1 + i);
                        image.setImageResource(R.drawable.trans);
                    }
                }
            }
        });
    }

    private void simulateBeacons() {
        HashMap<String,String> map=Res.getInstance().mapaCuadros;
        for (int i = 0; i < IMAGENES; i++) {
            String s="0,"+String.valueOf(i+1);
            imagenesOcupadas[i] = map.get(s);
            System.out.println("Imagen "+imagenesOcupadas[i]);
        }
        showInDisplay();
    }
}
