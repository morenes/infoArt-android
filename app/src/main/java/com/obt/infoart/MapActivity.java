package com.obt.infoart;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends Activity implements OnMapReadyCallback {
    public static final String TAG = "MapActivity";

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
    //DO WHATEVER YOU WANT WITH GOOGLEMAP
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        //map.setMyLocationEnabled(true);
        map.setTrafficEnabled(true);
        map.setIndoorEnabled(true);
        map.setBuildingsEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        //

        String[] result = Res.getInstance().obtenerText("/Museum/MAP").split(">");
        for (String rec : result) {
            // Add a new marker for this earthquake
            String[] subcadenas = rec.split(",");
            Marker mark = map.addMarker(new MarkerOptions()
                    // Set the Marker's position
                    .position(new LatLng(Double.parseDouble(subcadenas[1]), Double.parseDouble(subcadenas[2])))
                    // Set the title of the Marker's information window
                    .title(String.valueOf(subcadenas[0]))
                    // Set the color for the Marker
                    .icon(BitmapDescriptorFactory
                            .defaultMarker()));
            if (subcadenas[0].equals(Res.getInstance().getNombre())) {
                map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(
                        Double.parseDouble(subcadenas[1]), Double.parseDouble(subcadenas[2]))));
                mark.showInfoWindow();
            }
            map.setOnMarkerClickListener(new OnMarkerClickListener() {

                @Override
                public boolean onMarkerClick(Marker mark) {
                    mark.showInfoWindow();
                    iniciar(mark.getTitle());
                    return true;
                }
            });
        }
    }

    public void iniciar(String museo) {
        Intent myIntent = null;
        myIntent = new Intent(this, UtilActivity.class);
        myIntent.putExtra("info", true);
        myIntent.putExtra("nombre", museo);
        this.startActivity(myIntent);
    }
}