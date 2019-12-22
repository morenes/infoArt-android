package com.obt.infoart;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;


public class App extends Application implements BootstrapNotifier {
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    private boolean haveDetectedBeaconsSinceBoot = false;
    private static IRange rangeNotifier;
	public static Context context;

    /////
    public void onCreate() {
        super.onCreate();
        context=getApplicationContext();
        BeaconManager beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        Region region = new Region("backgroundRegion",
                null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);
        backgroundPowerSaver = new BackgroundPowerSaver(this);
    }

    @Override
    public void didEnterRegion(Region arg0) {
    }

    @Override
    public void didExitRegion(Region region) {
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
    }

    public static IRange getRangeNotifier() {
        return rangeNotifier;
    }

    public static void setRangeNotifier(IRange rangeNotifier) {
        App.rangeNotifier = rangeNotifier;
    }

    public static void sendToken(){
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.e("token", "Refreshed token: " + refreshedToken);
        //context.getSharedPreferences("perfil",MODE_PRIVATE).edit().putString("token",refreshedToken).putBoolean("tokenRecibido",false).apply();
        //
        long res;
        res=UDP.getInstance().sendBuzon("token>"+refreshedToken);
        int i=0;
        while(i<UDP.MAX_INTENTOS&&(res==-1)){
            res=UDP.getInstance().sendBuzon("token>"+refreshedToken);
            i++;
        }
    }
}
