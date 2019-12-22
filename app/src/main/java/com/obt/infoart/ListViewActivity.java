package com.obt.infoart;

import java.io.File;
import java.io.FileFilter;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ListViewActivity extends ListActivity {
    public static final int MAPA_MUSEO = 9;
    public static final int CUADROS_DESTACADOS = 10;
    public static final int MIS_FAVORITOS = 11;
    public static final int INFORMACION_MUSEO = 12;
    public static final int MUSEOS_VISITADOS = 13;
    public static final int MAPA_IGUIDES = 14;
    public static final int SUGERENCIAS = 15;
    public static final int SOBRE_IGUIDES = 16;

    private String[] museos;
    private String[] cadenasOp;
    private String[] idiomas;

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if (Res.getInstance().getIdioma() != -1) finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        museos = extras.getStringArray("museos");
        final boolean opciones = extras.getBoolean("opciones");
        final boolean favoritos = extras.getBoolean("favoritos");
        final String destacados = extras.getString("destacados");
        final Res res = Res.getInstance();
        if (opciones) {
            cadenasOp = new String[8];
            cadenasOp[0] = getResources().getString(R.string.mapaMuseo);
            cadenasOp[1] = getResources().getString(R.string.destacados);
            cadenasOp[2] = getResources().getString(R.string.misFavoritos);
            cadenasOp[3] = getResources().getString(R.string.informacionMuseo);
            cadenasOp[4] = getResources().getString(R.string.museosVisitados);
            cadenasOp[5] = getResources().getString(R.string.mapaInfoart);
            cadenasOp[6] = getResources().getString(R.string.buzonSugerencias);
            cadenasOp[7] = getResources().getString(R.string.sobreLaApp);

        } else if (favoritos) { //FAVORITOS
            cadenasOp = new String[res.getFavoritos().size()];
            int i = 0;
            for (Byte elem : res.getFavoritos()) {
                cadenasOp[i] = res.mapaNumeros[res.getIdioma()].get(elem);
                i++;
            }
        } else if (destacados != null) { //DESTACADOS
            cadenasOp = destacados.split(">");
            for (int i = 0; i < cadenasOp.length; i++) {
                String subcadenas[] = cadenasOp[i].split(" ");
                //TODO
                Byte id = Byte.parseByte(subcadenas[0]);
                String aux = res.mapaNumeros[res.getIdioma()].get(id);
                cadenasOp[i] = aux + " " + subcadenas[1] + "♥";
            }
        }
        /* OBTENER LAS CADENAS */

        if (museos != null)
            setListAdapter(new ArrayAdapter<>(this, R.layout.list_item,
                    museos));
        else if (opciones || favoritos || destacados != null) {
            if (cadenasOp == null) finish();
            setListAdapter(new ArrayAdapter<>(this, R.layout.list_item,
                    cadenasOp));
        } else {
            if (res.idiomas != null) {
                String[] idiomasPermitidos = res.mapaMusIdiomas.get(res.getNombre()).split("<");
                idiomas = new String[idiomasPermitidos.length];
                for (int i = 0; i < idiomasPermitidos.length; i++)
                    idiomas[i] = res.idiomas[Integer.parseInt(idiomasPermitidos[i])];
                ArrayAdapter<String> array = new ArrayAdapter<>(this,
                        R.layout.list_item, idiomas);
                setListAdapter(array);

            } else
                setListAdapter(new ArrayAdapter<>(this,
                        R.layout.list_item, new String[0]));
        }
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (destacados == null)
                    Toast.makeText(getApplicationContext(),
                            ((TextView) view).getText(), Toast.LENGTH_SHORT)
                            .show();

                if (museos != null) {
                    elegirMuseo(position);
                } else if (opciones)
                    elegirOpcion(position + MAPA_MUSEO);
                else if (favoritos || destacados != null) {
                    elegirCuadro(position, favoritos);
                } else {
                    String idiomaName = idiomas[position];
                    int i = 0;
                    while (i < res.idiomas.length) {
                        if (idiomaName.equals(res.idiomas[i])) {
                            res.setIdioma(i);
                            break;
                        }
                        i++;
                    }
                    res.settings.edit().putInt("IDIOMA", res.idioma).apply();
                }
            }
        });
    }

    private void elegirMuseo(int i) {
        String nombre = museos[i];
        Res res = Res.getInstance();
        res.setNombre(nombre);
        res.settings.edit().putString("ultimo", nombre).apply();
        //res.main.visibilizar(R.id.Button1Main,true);
        finish();
    }

    private void elegirCuadro(int i, boolean favoritos) {
        Intent myIntent = new Intent(this, ViewPicture.class);
        String res = cadenasOp[i];
        Res r = Res.getInstance();
        Byte key = r.mapaNumerosInv[r.getIdioma()].get(res);

        if (!favoritos) {//DESTACADOS
            String subcadenas[] = cadenasOp[i].split(" ");
            res = "";
            for (int k = 0; k < (subcadenas.length - 1); k++) {
                if (k != 0)
                    res += " ";
                res += subcadenas[k];
            }
            key = r.mapaNumerosInv[r.getIdioma()].get(res);
            myIntent.putExtra("tipo", Frame.HIGH);
        } else
            myIntent.putExtra("tipo", Frame.LIKE);

        Toast.makeText(getApplicationContext(), res, Toast.LENGTH_SHORT).show();

        myIntent.putExtra("clave", r.mapaNumeros[0].get(key));
        this.startActivity(myIntent);
    }

    private void elegirOpcion(int i) {
        Intent myIntent;
        switch (i) {
            case MAPA_MUSEO:
                myIntent = new Intent(this, ViewPagerActivity.class);
                this.startActivity(myIntent);
                break;
            case CUADROS_DESTACADOS:
                UDP.getInstance().setCallback(new IUDPCallback() {
                    @Override
                    public void received(final RxMensaje rx) {
                        if (rx != null) {
                            Intent myIntent = new Intent(App.context, ListViewActivity.class);
                            SparseIntArray map = rx.getMap();
                            String destacados = "";
                            for (int i = 0; i < map.size(); i++) {
                                destacados += map.keyAt(i) + " " + map.valueAt(i) + ">";
                            }
                            if (destacados.length() > 1) { //Si no hay destacados
                                destacados = destacados.substring(0, destacados.length() - 1);
                                System.out.println("DESTACADOS: " + destacados);
                                myIntent.putExtra("destacados", destacados);
                                myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                App.context.startActivity(myIntent);
                            }
                        }
                    }
                });
                UDP.getInstance().sendHigh();

                break;
            case MIS_FAVORITOS:
                myIntent = new Intent(this, ListViewActivity.class);
                myIntent.putExtra("favoritos", true);
                this.startActivity(myIntent);
                break;
            case INFORMACION_MUSEO:
                myIntent = new Intent(this, UtilActivity.class);
                myIntent.putExtra("info", true);
                myIntent.putExtra("nombre", Res.getInstance().getNombre());
                this.startActivity(myIntent);
                break;
            case MUSEOS_VISITADOS:
                String nombresFicheros[];
                myIntent = new Intent(this, ListViewActivity.class);
                File folder = new File(Res.getInstance().obtenerDir(),
                        "Museum");
                File ficheros[] = folder.listFiles(new FileFilter() {
                    public boolean accept(File file) {
                        return file.isDirectory();
                    }
                });
                int numMuseos = ficheros.length;
                nombresFicheros = new String[numMuseos];
                for (int j = 0; j < numMuseos; j++) {
                    nombresFicheros[j] = ficheros[j].getName();
                }
                myIntent.putExtra("museos", nombresFicheros);
                this.startActivity(myIntent);
                break;
            case SUGERENCIAS:
                myIntent = new Intent(this, UtilActivity.class);
                myIntent.putExtra("sugerencias", true);
                this.startActivity(myIntent);
                break;
            case MAPA_IGUIDES:
                myIntent = new Intent(this, MapActivity.class);
                this.startActivity(myIntent);
                break;
            case SOBRE_IGUIDES:
                myIntent = new Intent(this, UtilActivity.class);
                myIntent.putExtra("iguides", true);
                this.startActivity(myIntent);
                break;
        }
    }
}