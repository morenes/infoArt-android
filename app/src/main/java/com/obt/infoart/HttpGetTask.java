package com.obt.infoart;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URL;
import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class HttpGetTask extends AsyncTask<String,Integer, String[]> {
	private static final String TAG = "HttpGetTask";
	private Context context= App.context;
    private PowerManager.WakeLock mWakeLock;
    static boolean spinner=false;
    private static boolean bloqueado=false;
    private MainActivity main=MainActivity.instance;
	private Res res=Res.getInstance();
	@SuppressWarnings("resource")
	@Override
	protected String[] doInBackground(String... params) {
		spinner=true;
		res.setBluetooth(false);
		InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
        	File f=new File(res.obtenerDir()+params[1]);
			f.mkdirs();
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            int fileLength = connection.getContentLength();
            input = connection.getInputStream();
            output = new BufferedOutputStream(
					new FileOutputStream(new File(res.obtenerDir()+params[1]+"/temp"+params[2]+".zip")));

            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                if (isCancelled()) {
                    input.close();
                    output.close();
                    return null;
                }
                total += count;
                if (fileLength > 0) // only if total length is known
                    publishProgress((int) (total * 100 / fileLength));
                output.write(data, 0, count);
            }
        } catch (Exception e) {
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            	Log.e(TAG, "IOException");
    			ignored.printStackTrace();
    			if (bloqueado){
    				mWakeLock.release();
    				main.dismiss();
    				bloqueado=false;
    				spinner=false;
    			}
            }

            if (connection != null)
                connection.disconnect();
        }
		try {
			File zip=new File(res.obtenerDir()+params[1]+"/temp"+params[2]+".zip");
			File unzipped=new File(res.obtenerDir()+params[1]);
			Log.e(TAG,zip.toString());
			res.unzip(zip,unzipped);
			zip.delete();
		} catch (IOException e) {
			e.printStackTrace();
			if (bloqueado){
				mWakeLock.release();
				main.dismiss();
				bloqueado=false;
				spinner=false;
			}
			return null;
		}
		String[] args=new String[3];
		args[0]=params[1];
		args[1]=params[2];
		args[2]=params[3];
		return args;
	}

	@Override
	protected void onPostExecute(String[] args) {
		if (bloqueado){
			mWakeLock.release();
			main.dismiss();
			bloqueado=false;
		}
        if (!UDP.getInstance().checkConex()) {
			main.mensajeInternet();
			return;
        }
        if (args == null){
            Toast.makeText(context,context.getResources().getString(R.string.noConexion), Toast.LENGTH_LONG).show();
            return;
        }
        else
            Toast.makeText(context,context.getResources().getString(R.string.ok), Toast.LENGTH_SHORT).show();
		switch(args[1]){
		case "COM":{
			main.callBackTCPComun();
			break;
		}
		case "PRI":{
			main.callBackTCPPri();
			break;
		}
		case "INFO":{
			File archivo = new File(res.obtenerDir()
					+ "/Museum/"+ res.getNombre() + "/INFO"+ res.getIdioma());
			if ((archivo.exists())&&(archivo.list().length>5))
					res.settings.edit().putBoolean(res.getNombre()+"INFO"+ res.getIdioma(),true).apply();
			break;
		}
		default: break;
		}
		spinner=false;
	}
	@Override
    protected void onPreExecute() {
        super.onPreExecute();
        spinner=true;
        bloqueado=true;
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
             getClass().getName());
        mWakeLock.acquire();
        main.show();
        
    }
	@Override
	protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        // if we get here, length is known, now set indeterminate to false
        main.update(progress[0]);
    }
	
}