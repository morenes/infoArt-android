package com.obt.infoart;
import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class UtilActivity extends Activity {
	private static final int MSG_LENGTH=255;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		Bundle extras = getIntent().getExtras();
		boolean infoArt = false;
		boolean info = false;
		boolean sugerencias=false;
		String nombre=null;
		if (extras != null) {
			sugerencias=extras.getBoolean("sugerencias");
			infoArt = extras.getBoolean("iguides");
			info = extras.getBoolean("info");
			nombre=extras.getString("nombre");
		}
		EditText text=null;
		setContentView(R.layout.activity_util);
		text = (EditText) findViewById(R.id.text5);
		text.setVisibility(View.INVISIBLE);
		String peticion = null;
		if (infoArt) {
			peticion = "INFOART";
		} else if (info) {
			peticion = nombre + "-INFOMUSEO";
		}

		if (infoArt || info) {
			String cadenas = Res.getInstance().obtenerText("/Museum/" + peticion + Res.getInstance().getIdioma());
			
			String texto = cadenas.replace('>', '\n');
			text.setText(texto);
			text.setKeyListener(null);
			text.setVisibility(View.VISIBLE);
		}
		else if (sugerencias){
			text.setVisibility(View.VISIBLE);
			text.setText("");
			Button boton=(Button)findViewById(R.id.ButtonSend);
			boton.setText("  "+getResources().getString(R.string.enviar)+" »  ");
			boton.setVisibility(View.VISIBLE);
		}
	}   

	@Override
	protected void onResume() {
		super.onResume();
	} 
	 
	public void onRangingClickedUtil(View view) {
		EditText edit=(EditText) findViewById(R.id.text5);
		String aux=edit.getText().toString();
		if (aux.length()>MSG_LENGTH) aux=aux.substring(0,MSG_LENGTH);
		final String texto=aux;
		if (!texto.equals(""))
			new Thread(new Runnable() {
				public void run() {
					long res;
					res=UDP.getInstance().sendBuzon(texto);
					pushToast(getResources().getString(R.string.enviando));
					int i=0;
					while(i<UDP.MAX_INTENTOS&&(res==-1)){
						res=UDP.getInstance().sendBuzon(texto);
						i++;
					}
					if (res==-1){
						Res.getInstance().settings.edit().putString("BUZON",texto).apply();
						pushToast(getResources().getString(R.string.noConexion));
					}
					else pushToast(getResources().getString(R.string.enviado));
				}
				}).start();
		
	}
	public void pushToast(final String mensaje){
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(),
						mensaje,
						Toast.LENGTH_SHORT).show();
				if (mensaje.equals(getResources().getString(R.string.enviado))) finish();
			}
		});
	}
}