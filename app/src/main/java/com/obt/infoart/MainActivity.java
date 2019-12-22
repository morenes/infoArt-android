package com.obt.infoart;
import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import org.altbeacon.beacon.BeaconManager;
import android.os.Build;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
	protected static final String TAG = "MainActivity";
	static TelephonyManager tel;
	public static int seguirBuscando = 0;
	public static boolean resultadoCheck = false;
	public static long TIEMPO_CORTESIA = 3600 * 4 * 1000;
	ProgressDialog mProgressDialog;
	private boolean inicializado = false;
	private boolean entrando=false;
	private Res res;
    private UDP udp;
	public static MainActivity instance;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        udp=UDP.getInstance();
		instance=this;
		res=Res.getInstance();
		res.settings = getSharedPreferences("perfil", MODE_PRIVATE);

		inicializado = false;
		Res.setBluetooth(false);

		tel = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);

		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setMessage(getResources().getString(R.string.cargando));
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setIndeterminate(false);
		mProgressDialog.setMax(100);
		new File(res.obtenerDir(), "Museum").mkdirs();
		establecerIdiomaDefecto();
		pedirComun();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		res.settings = getSharedPreferences("perfil", MODE_PRIVATE);
		String nombre=res.settings.getString("ultimo", null);

		if (nombre != null) {
			res.setNombre(nombre);
			cambiarBotones();
			visibilizar(R.id.Button1Main, false);
			visibilizar(R.id.button2Main, false);
			if (res.getIdioma() == -1) {
				Intent myIntent = new Intent(this, ListViewActivity.class);
				myIntent.putExtra("nada", 0);
				this.startActivity(myIntent);
			}
		}
		//CAMBIAR BANDERITA
		if (res.settings.getBoolean("COMUN", false)) {
			pedirPrioritarios();
			findViewById(R.id.buttonLanMain).setBackground(Drawable.createFromPath(res
					.obtenerDir()
					+ "/Museum/IDIOMA"
					+ res.getIdioma()
					+ Res.EX_IMAGEN));
		}
		//res.context = this;
	}

	void inicializar() {

		String nombre=res.settings.getString("ultimo", null);
		if (nombre != null) {
			res.setNombre(nombre);
			cambiarBotones();
			visibilizar(R.id.Button1Main, false);
		} else {
			Toast.makeText(getApplicationContext(),
				getResources().getString(R.string.seleccionaMuseo),
				Toast.LENGTH_SHORT).show();
		}
		visibilizar(R.id.button2Main, false);
		inicializado = true;
	}

	public void irMuseo() {
		String nombre=res.getNombre();
		if (!entrando) {
			entrando = true;
			//PEDIR CODIGO
			comprobarMuseo(nombre);
			if (!resultadoCheck) {
				DialogoNombre();
			}
			if (resultadoCheck) {
				if (res.settings.getBoolean(nombre + "INFO" + res.getIdioma(), false)) {
					Intent myIntent = null;
					myIntent = new Intent(this, MonitoringActivity.class);
					entrando=false;
					this.startActivity(myIntent);
				} else pedirInfoUrl();

			}
			entrando = false;
		}
	}

	///////////////////////////////////////////
	public void pedirComun() {
		System.out.println("pedir comun");
		if (!res.settings.getBoolean("COMUN", false))
			new Thread(new Runnable() {
				@Override
				public void run() {
				int i = 0;
				String resp = "";
				while (resp.equals("") && i < 5) {
					resp = udp.sendUrl(0);
					if (!resp.equals("")) {
						System.out.println("URL: "+resp);
						Res.URL_COM = resp;
						TIEMPO_CORTESIA = (long) (1000 * 3600); //1 hora
						//res.estado = 4;
						//res.settings.edit().putInt("ESTADO", res.estado).apply();
						Res.setBluetooth(false);
						new HttpGetTask().execute(Res.URL_COM, "/Museum", "COM", null);
						break;
					}
					i++;
				}
				if ((i == 5) && (!udp.checkConex())) mensajeInternet();
				}
			}).start();
		else {
			callBackTCPComun();
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				App.sendToken();
			}
		}).start();
	}

	public void callBackTCPComun() {
		System.out.println("callback tcp");
		res.settings.edit().putBoolean("COMUN", true).apply();
		String[] datos = res.obtenerText("/Museum/MAP").split(">");
		res.mapaMusIdiomas = new HashMap<>();
		res.mapaMusBeacons = new HashMap<>();
		res.mapaMusNumbers = new HashMap<>();
		String[] sub;
		int cont=1;
		for (String line : datos) {
			sub = line.split(",");
			//if (Res.estado == 4)
			res.mapaMusBeacons.put(sub[0], sub[4] + "," + sub[5]);
			//else res.mapaMusBeacons.put(sub[0], sub[3] + "," + sub[4] + "," + sub[5]);
			res.mapaMusIdiomas.put(sub[0], sub[6]);
			res.mapaMusNumbers.put(sub[0],cont++);
		}

		String fichero = res.obtenerText("/Museum/IDIOMAS");
		res.idiomas = fichero.split(">");
		if (!inicializado) inicializar();
	}

	public void pedirPrioritarios() {
		//System.out.println("Pedir prioritarios");
		final String nombre=res.getNombre();
        if (nombre == null) return;
        //TODO cambio true
		if (!res.settings.getBoolean(nombre + "PRI", false)) {
			System.out.println("No estan los prioritarios");
			Res.setBluetooth(false);
			//PEDIMOS LA URL
			new Thread(new Runnable() {
				@Override
				public void run() {
					int i = 0;
					String resp = "";
					while (resp.equals("") && i < UDP.MAX_INTENTOS) {
						resp = udp.sendUrl(res.getPlace());
						if (!resp.equals("")) {
							Res.URL_PRI = resp;
							if (!HttpGetTask.spinner)
								new HttpGetTask().execute(Res.URL_PRI, "/Museum/" + nombre, "PRI", null);
							break;
						}
						i++;
					}
					if ((i == UDP.MAX_INTENTOS) && (!udp.checkConex())) mensajeInternet();
				}
			}).start();
		} else {//SI YA ESTAN
			res.obtenerListado();
			pedirInfoUrl();
		}
	}

	public void callBackTCPPri() {
		String nombre=res.getNombre();
		res.settings.edit().putBoolean(nombre + "PRI", true).apply();
		res.obtenerListado();
		pedirInfoUrl();
	}

	public void pedirInfoUrl() {
		//System.out.println("Pedir info url");
		String nombre=res.getNombre();
		int idioma=res.getIdioma();
		boolean code = res.settings.getString(nombre, null) != null;
		//boolean serverError = resources.settings.getString("paypal" + resources.nombre, null) != null;
		if ((nombre != null) && (code)) {
			if (!HttpGetTask.spinner) {
				res.obtenerListado();
				Res.URL_INFO = res.obtenerText("/Museum/" + nombre + "/INFO" + idioma);
				System.out.println(Res.URL_INFO);
				if (Res.URL_INFO.equals("")) return;
				if (!res.settings.getBoolean(nombre + "INFO" + idioma, false)) {
					Res.setBluetooth(false);
					new HttpGetTask().execute(Res.URL_INFO, "/Museum/" + nombre + "/INFO" + idioma, "INFO", null);
				}
			}
		}
	}

	public void establecerIdiomaDefecto() {
		long imei = res.settings.getLong("IMEI", 0);
		if (imei == 0) {
			imei = System.currentTimeMillis() %10;//% 10000000;
			imei+=100;
			res.settings.edit().putLong("IMEI", imei).apply();
		}
		res.setId(imei);
		/////////FAKE
		res.settings.edit().putLong("IMEI", ++imei).apply();
		res.setId(imei);
		///////
		//CARGAR EL IDIOMA PREESTABLECIDO O DETERMINAR NUEVO IDIOMA
		res.setIdioma(res.settings.getInt("IDIOMA", -1));
		if (res.getIdioma() == -1) {
			Locale current = getResources().getConfiguration().locale;
			String country = "";
			if (current != null) {
				country = current.getLanguage();
			}
			if (country.equals("en")) {
				res.setIdioma(1);
			} else if (country.equals("es")) {
				res.setIdioma(0);
			}
			res.settings.edit().putInt("IDIOMA", res.getIdioma()).apply();
		}
	}

	public void cambiarBotones() {
		Button boton;
		boton = (Button) findViewById(R.id.Button1Main);
		boton.setText("  " + getResources().getString(R.string.comenzar) + " " + res.getNombre() + " »  ");
		boton = (Button) findViewById(R.id.button2Main);
		boton.setText("  " + getResources().getString(R.string.seleccionaMuseo) + " »  ");
	}

	public void dismiss() {
		runOnUiThread(new Runnable() {
			public void run() {
				mProgressDialog.dismiss();
			}
		});
	}

	public void show() {
		runOnUiThread(new Runnable() {
			public void run() {
				mProgressDialog.show();
			}
		});
	}

	public void update(final int up) {
		mProgressDialog.setProgress(up);
	}

	//PULSAR BOTONES
	public void onRangingClickedMain(View view) {

		Intent myIntent = null;
		switch (view.getId()) {
			case R.id.Button1Main:
				pedirPrioritarios();
				irMuseo();
				seguirBuscando = 0;
				break;

			case R.id.button2Main:
				seguirBuscando = 0;
				myIntent = new Intent(this, ListViewActivity.class);
				if (res.mapaMusIdiomas == null)
					break;
				String[] nombresMuseos = new String[res.mapaMusIdiomas.size()];
				int i = 0;
				for (String s : res.mapaMusIdiomas.keySet()) {
					nombresMuseos[i] = s;
					i++;
				}

				myIntent.putExtra("museos", nombresMuseos);
				this.startActivity(myIntent);
				break;

			case R.id.buttonLanMain:
				myIntent = new Intent(this, ListViewActivity.class);
				myIntent.putExtra("nada", 0);
				this.startActivity(myIntent);
				break;
		}
	}

	public void mensajeInternet() {
		runOnUiThread(new Runnable() {
			@SuppressWarnings("deprecation")
			public void run() {
				showDialog(1);
				String mensaje = getResources().getString(R.string.noConexion);
				Toast.makeText(App.context, mensaje,
						Toast.LENGTH_SHORT).show();
			}
		});
	}

	//HACER VISIBLES LOS BOTONES
	public void visibilizar(final int id, final boolean unlocated) {
		final String nombre=res.getNombre();
		runOnUiThread(new Runnable() {
			public void run() {
				if (unlocated)
					switch (id) {
						case R.id.Button1Main:
							Toast.makeText(
									getApplicationContext(), nombre
											+ " "
											+ getResources().getString(R.string.localizado),
									Toast.LENGTH_SHORT).show();
							break;
						case R.id.button2Main:
							Toast.makeText(
									getApplicationContext(),
									getResources().getString(R.string.noLocalizado),
									Toast.LENGTH_SHORT).show();
							break;
					}
				if (id == R.id.Button1Main) {
					if (nombre != null) {
						Button b = (Button) findViewById(id);
						b.setText("  " + getResources().getString(R.string.comenzar) + " " + nombre + " »  ");
					}
				}
				findViewById(id).setVisibility(View.VISIBLE);
			}
		});

	}

	/*
	@SuppressLint("NewApi")
	public static String obtenerId() {

		if (Resources.imei == null) {
			String imei = tel.getDeviceId();
			String country = tel.getSimCountryIso();
			String number = "null";
			if (Build.VERSION.SDK_INT >= 18)
				number = tel.getGroupIdLevel1();
			if (imei != null)
				return imei + ">" + country + ">" + number;
			return "null>null>null";
		}
		return Resources.imei + ">null>null";

	}
*/
	public void comprobarCodigo(final String code) {
		resultadoCheck = false;
		if (!udp.checkConex()) {
			mensajeInternet();
			return;
		}
		udp.setCallback(new IUDPCallback() {
			@Override
			public void received(RxMensaje rx) {
                if (rx!=null) {
                    long recibo = rx.getId();
                    if (recibo == 0 || recibo == res.getId()) {
                        resultadoCheck = true;
                        pedirPrioritarios();
                    }
                    Looper.prepare();
                    if (resultadoCheck) {
                        grabarCodigo(code);
                        Toast.makeText(getApplicationContext(),
                                "OK", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(
                                getApplicationContext(),
                                getResources().getString(R.string.codigoErroneo),
                                Toast.LENGTH_SHORT).show();

                    }
                }
			}
		});
		udp.sendCode(Long.parseLong(code));
	}

	public void comprobarMuseo(final String museo) {
		resultadoCheck = false;
		SharedPreferences settings = getSharedPreferences("perfil", MODE_PRIVATE);
		//boolean serverError = (!settings.getString("paypal" + museo, "").equals(""));

		if (!settings.getString(museo, "").equals("")) {
			resultadoCheck = true;
			pedirPrioritarios();
		} else if (res.isDemo()) {
            System.out.println("Es DEMO!!!!!!!!!!!!!!!!!!!!!!");
			resultadoCheck = true;
			settings.edit().putString(museo, "demo").apply();
			pedirPrioritarios();
		}
	}

	private void grabarCodigo(String code) {
		String nombre=res.getNombre();
		File folder = new File(res.obtenerDir() + "/Museum/" + nombre);
		folder.mkdir();
		res.settings.edit().putString(nombre, code).apply();
	}


	//ALERT DIALOG DE INTRODUCIR CODIGO DE LICENCIA
	@SuppressLint("InflateParams")
	private void DialogoNombre() {
		// Rescatamos el layout creado para el prompt

		LayoutInflater li = LayoutInflater.from(this);
		View prompt = li.inflate(R.layout.prom, null);
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setView(prompt);

		final EditText numeroCuadro = (EditText) prompt
				.findViewById(R.id.numeroCuadro);
		String code = res.settings.getString(res.getNombre(), "");
		if (!code.equals(""))
			numeroCuadro.setText(code);
		TextView titulo = (TextView) prompt.findViewById(R.id.titulo);
		titulo.setText(getResources().getString(R.string.insertaCodigo));
		// Mostramos el mensaje del cuadro de dialogo
		alertDialogBuilder
				.setCancelable(true)
				.setPositiveButton(
						getResources().getString(R.string.comprobar),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// Rescatamos el nombre del EditText y lo
								// mostramos por pantalla
								String numero = numeroCuadro.getText()
										.toString();
								comprobarCodigo(numero);
							}
						});
		/*PAYPAL
						.setNegativeButton(getResources().getString(R.string.obtener),
								new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								File archivo = new File(Resources.obtenerDir()+"/Museum/"+Resources.nombre+"/PORTADA"+AppData.EX_IMAGEN);
								if (Resources.existeFichero(archivo)){
									Resources.setBluetooth(false);
									PayPalPayment payment = new PayPalPayment(new BigDecimal("2.0"), "EUR", "infoArt",
											PayPalPayment.PAYMENT_INTENT_SALE);
									intentActivity.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
									intentActivity.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);
									startActivityForResult( intentActivity, 0);
								}
							}
						});
						*/
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	//ALERT DIALOG DEL NO HAY INTERNET
	protected Dialog onCreateDialog(int id) {
		String nohayconexion = getResources().getString(R.string.noConexion);
		String noconexionmsg = getResources().getString(R.string.msgNoConexion);
		String botonsalir = getResources().getString(R.string.salir);
		switch (id) {
			case 1: //NO CONEXION
				return new AlertDialog.Builder(App.context)
						.setTitle(nohayconexion)
						.setMessage(noconexionmsg)
						.setPositiveButton(botonsalir,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
														int whichButton) {
										finish();
//										startActivity(new Intent(
//												Settings.ACTION_WIRELESS_SETTINGS));
									}
								})
						.setOnCancelListener(
								new DialogInterface.OnCancelListener() {
									public void onCancel(DialogInterface dialog) {
										finish();
									}
								}).create();
		}
		return null;
	}

	//ALERT DIALOG DE NO HAY BLUETOOTH
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	private void verifyBluetooh() {
		String salir = getResources().getString(R.string.ok);
		String sin = getResources().getString(R.string.noBluetooth);
		String alerta = getResources().getString(R.string.alerta);
		String mensaje = getResources().getString(R.string.msgNoBluetooth);
		try {
			if (!BeaconManager.getInstanceForApplication(this)
					.checkAvailability()) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(
						this);
				builder.setTitle(alerta)
						.setMessage(mensaje)
						.setCancelable(false)
						.setPositiveButton(salir,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
														int id) {
										System.exit(0);
										finish();
									}
								})
						.setNegativeButton(sin,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
														int id) {
										if (res.getNombre() == null) {
											Toast.makeText(
													getApplicationContext(),
													getResources().getString(R.string.noLocalizado),
													Toast.LENGTH_SHORT)
													.show();
											findViewById(R.id.button2Main)
													.setVisibility(
															View.VISIBLE);
										}
									}
								});
				builder.show();
			}
		} catch (RuntimeException e) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getResources().getString(R.string.noBluetooth));
			builder.setMessage(getResources().getString(R.string.msgNoBluetooth));
			builder.setPositiveButton(android.R.string.ok, null);
			builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					if (res.getNombre() == null) {
						Toast.makeText(
								getApplicationContext(),
								getResources().getString(R.string.noLocalizado),
								Toast.LENGTH_SHORT).show();
						findViewById(R.id.button2Main).setVisibility(
								View.VISIBLE);
					}
				}
			});
			builder.show();
		}
	}

	//////////PAYPAL METHODS///////////////////////////////////
	/*
		@Override
		protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		    if (resultCode == Activity.RESULT_OK) {
		        final PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
		        if (confirm != null) {
		            try {
		                Log.e("paymentExample", confirm.toJSONObject().toString(4));

		            } catch (JSONException e) {
		                Log.e("paymentExample", "an extremely unlikely failure occurred: ", e);
		            }
		            final String id=confirm.getProofOfPayment().getPaymentId();
		            new Thread(new Runnable() {
		    			public void run() {
		    				enviarInfoPaypal(id);
		    				pedirPrioritarios();
		    			}
		    			}).start();

		        }
		        else if (resultCode == Activity.RESULT_CANCELED) {
		        for (int i=0;i<20;i++) Log.e(TAG,"paypal_cancel");
		        Log.e("paymentExample", "The user canceled.");
		        }
		        else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
		        for (int i=0;i<20;i++) Log.e(TAG,"paypal_invalid");
		        Log.e("paymentExample", "An invalid Payment or PayPalConfiguration was submitted. Please see the docs.");
		        }
		    }
		}
		boolean enviarInfoPaypal(String id){
			String res=null;
			String request=id+"<"+obtenerInfo();
			res=Resources.ponerTextoWeb(request,UDPClient.PAYPAL);
			Log.e(TAG,res);
			int contador=0;
			while((res==null)||(res.equals(""))||(!res.split("<")[1].equals(id))){
				res=Resources.ponerTextoWeb(request,UDPClient.PAYPAL);
				contador++;
				if (contador==5) break;
				Log.e(TAG,String.valueOf(contador));
			}
			if (contador==5){
				SharedPreferences.Editor sp = Resources.settings.edit();
		    	sp.putString("paypal"+Resources.nombre,id);
		    	sp.commit();
		    	return false;
			} else{
				Resources.settings.edit().putString(Resources.nombre,res.split("<")[0]).commit();
			return true;
			}

		}
		void comprobarPaypal(){
			final String id=Resources.settings.getString("paypal"+Resources.nombre,"");
			if (!id.equals("")){
				new Thread(new Runnable() {
	    			public void run() {
	    				boolean enviado=enviarInfoPaypal(id);
	    				if (enviado){
	    					SharedPreferences.Editor sp = Resources.settings.edit();
	    					sp.remove("paypal"+Resources.nombre);
	    					sp.commit();
	    				}
	    			}
	    			}).start();  
			}
		}
		*/
}
