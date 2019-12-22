package com.obt.infoart;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.altbeacon.beacon.Beacon;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.util.Log;

public class Res {
	private int place;
	private long id;
	private String nombre;
	public static final String EX_AUDIO=".mp3";
	public static final String EX_TEXTO=".txt";
	public static final String EX_IMAGEN=".jpg";
	public static final int VERSION = 17;

	public static String URL_COM=null;
	public static String URL_PRI=null;
	public static String URL_INFO=null;

	public int idioma = -1;
	public String[] idiomas;
	public HashMap<String, String> mapaMusIdiomas;
	public HashMap<String, String> mapaMusBeacons;
	public HashMap<String,Integer> mapaMusNumbers;
	public HashMap<String, String> mapaCuadros;
	public HashMap<Byte, String>[] mapaNumeros;
	public HashMap<String, Byte>[] mapaNumerosInv;

	private String ultimoMuseoListado;
	private LinkedList<Byte> favoritos;
	public SharedPreferences settings;

	private static Res instance=new Res();

	private Res(){
		ultimoMuseoListado="";
	}
	public static Res getInstance(){
		return instance;
	}

	public String getNombre(){
		return nombre;
	}

	public void setNombre(String name){
		nombre=name;
		place=mapaMusNumbers.get(nombre);
	}

	public int getPlace(){
		return place;
	}

	public long getId(){
		return id;
	}

	public void setId(long aux){
		id=aux;
	}

	public long getTime(){
        //System.out.println("Gap: "+gap+" nuevo time: "+System.currentTimeMillis()+gap);
        return System.currentTimeMillis();//+gap;
	}
	public int getIdioma() {
		return idioma;
	}

	public void setIdioma(int idioma) {
		this.idioma = idioma;
	}

    public List<Byte> getFavoritos(){
        if(favoritos==null){
            favoritos=new LinkedList<>();
            String favString=settings.getString("favoritos","");
            String [] cadenas=favString.split(",");

			for (String cadena : cadenas)
				if (!cadena.equals(""))
					favoritos.add(Byte.parseByte(cadena));
        }
        return favoritos;
    }
    public void addFavorito(Byte key){
        favoritos.add(key);
        updateFavorito();
    }
    public void removeFavorito(Byte key){
        favoritos.remove(key);
        updateFavorito();
    }
    private void updateFavorito(){
        String array="";
        for (Byte b: favoritos)
            array+=b+",";
        settings.edit().putString("favoritos",array).apply();
    }
	public File obtenerDir() {
		//return App.context.getFilesDir();
		return Environment.getExternalStorageDirectory();
	}

	public  Bitmap obtenerImageWeb(String id) {
		File imagesFolder = new File(obtenerDir() + "/Museum/",
				nombre);
		File archivo = new File(imagesFolder, id + EX_IMAGEN);
		if (!existeFichero(archivo))
			return BitmapFactory.decodeFile(new File(imagesFolder,id+"-1"+EX_IMAGEN).getAbsolutePath());
		return BitmapFactory.decodeFile(archivo.getAbsolutePath());
	}

	public void save(File nombre, byte[] data) {
		try {
			BufferedOutputStream os = new BufferedOutputStream(
					new FileOutputStream(nombre));
			BufferedInputStream is = new BufferedInputStream(
					new ByteArrayInputStream(data));
			copy(is, os);
		} catch (IOException e) {
			Log.e("Error", "FileNot! FoundException");
			Log.e("Error", nombre.getAbsolutePath());
		}
	}

	private void copy(InputStream is, OutputStream os) throws IOException {
		final byte[] buf = new byte[1024];
		int numBytes;
		try {
			while (-1 != (numBytes = is.read(buf))) {
				os.write(buf, 0, numBytes);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		} finally {
			try {
				is.close();
				os.close();
			} catch (IOException e) {
				Log.e("error", "IOException");
			}
		}
	}

	public String obtenerText(String id) {
		File archivo = new File(obtenerDir() + id + EX_TEXTO);
		if (!existeFichero(archivo)) return "";
		FileReader fr = null;
		BufferedReader br = null;
		String fichero = "";
		String linea;
		try {
			fr = new FileReader(archivo);
			br = new BufferedReader(fr);
			while ((linea = br.readLine()) != null)
				fichero += linea;
			return fichero;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != fr)
				try {
					fr.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return fichero;
	}

	public void obtenerListado() {
		System.out.println("Obtener listado");
		File archivo=new File(obtenerDir()+"/Museum/"+nombre+"/LISTADO"+EX_TEXTO);
		if ((nombre!=null)&&existeFichero(archivo)){
			if ((mapaNumeros==null)||(!ultimoMuseoListado.equals(nombre))){
				ultimoMuseoListado=nombre;
				String[] numeros = null;
				numeros = obtenerText("/Museum/" + nombre + "/LISTADO").split(">");
				String subcadenas[];
				mapaNumeros = new HashMap[idiomas.length];
				mapaNumerosInv = new HashMap[idiomas.length];
				mapaCuadros = new HashMap<String, String>();
				for(int j=0;j<(idiomas.length);j++){
					mapaNumeros[j]=new HashMap<Byte,String>();
					mapaNumerosInv[j]=new HashMap<String,Byte>();
				}
				for (String numero : numeros) {
					subcadenas = numero.split("<");
					//if (estado==4)
					mapaCuadros.put(subcadenas[1].split(",")[1] + "," + subcadenas[1].split(",")[2], subcadenas[2]);
					//else mapaCuadros.put(subcadenas[1],subcadenas[2]);
					for (int j = 0; j < (subcadenas.length - 2); j++) {
						mapaNumeros[j].put(Byte.parseByte(subcadenas[0]), subcadenas[2 + j]);
						mapaNumerosInv[j].put(subcadenas[2 + j], Byte.parseByte(subcadenas[0]));
					}
				}
			}
		}else System.out.println("HAY PROBLEM CON LISTADO");
	}

	public boolean existeFichero(File archivo) {
		// if (archivo.exists()
		// && (System.currentTimeMillis() - archivo.lastModified() <
		// MainActivity.TIEMPO_CORTESIA))
		return archivo.exists();
	}

	public Bitmap getRoundedCornerBitmap(Bitmap bitmap, boolean square,
			int cant) {
		int width = 0;
		int height = 0;

		if (square) {
			if (bitmap.getWidth() < bitmap.getHeight()) {
				width = bitmap.getWidth();
				height = bitmap.getWidth();
			} else {
				width = bitmap.getHeight();
				height = bitmap.getHeight();
			}
		} else {
			height = bitmap.getHeight();
			width = bitmap.getWidth();
		}

		Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, width, height);
		final RectF rectF = new RectF(rect);
		final float roundPx = cant;

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		return output;
	}

	public static boolean setBluetooth(boolean enable) {
	    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    boolean isEnabled = bluetoothAdapter.isEnabled();
	    if (enable && !isEnabled) {
	        return bluetoothAdapter.enable();
	    }
	    else if(!enable && isEnabled) {
	        return bluetoothAdapter.disable();
	    }
	    return true;
	}

	public void unzip(File zipFile, File targetDirectory) throws IOException {
	    ZipInputStream zis = new ZipInputStream(
	            new BufferedInputStream(new FileInputStream(zipFile)));
	    try {
	        ZipEntry ze;
	        int count;
	        byte[] buffer = new byte[8192];
	        while ((ze = zis.getNextEntry()) != null) {
	            File file = new File(targetDirectory, ze.getName());
	            File dir = ze.isDirectory() ? file : file.getParentFile();
	            if (!dir.isDirectory() && !dir.mkdirs())
	                throw new FileNotFoundException("Failed to ensure directory: " +
	                        dir.getAbsolutePath());
	            if (ze.isDirectory())
	                continue;
	            FileOutputStream fout = new FileOutputStream(file);
	            try {
	                while ((count = zis.read(buffer)) != -1)
	                    fout.write(buffer, 0, count);
	            } finally {
	                fout.close();
	            }
	        }
	    } finally {
	        zis.close();
	    }
	}
	boolean isDemo(){
		return getPlace()==1;
	}

	static int isNumeric(String str)
	{
		int d;
		try
		{
			d = Integer.parseInt(str);
		}
		catch(NumberFormatException nfe)
		{
			return -1;
		}
		return d;
	}
	static byte getDistance(Beacon beacon){
		int dis=Math.round((int)(beacon.getDistance()*10));
		if (dis>255) return (byte)255;
		else return (byte)dis;
	}
	static String getInfo(Beacon beacon){return beacon.getId3()+","+beacon.getId2();}
}