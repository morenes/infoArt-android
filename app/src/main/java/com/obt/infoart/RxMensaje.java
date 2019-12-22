package com.obt.infoart;

import android.util.SparseIntArray;

/**
 * Created by Administrador on 23/02/2017.
 */

public class RxMensaje {
    static final int FRAME=1;
    static final int LIKE=2;
    static final int HIGH=3;
    static final int URL=4;
    static final int BUZON=5;
    static final int CODECHECK=6;

    private static final int MSG_MAXLEN=1000;
    private SparseIntArray map;
    private int tipo;
    private String url;
    private long id;
    private int x,y;
    public RxMensaje(){
        map=new SparseIntArray();
    }

    long procesaMensaje(byte[] buffer) {
        int i,j,lngMsg,posicion;
        int lngMensajeOk;
        i = buscaInicioMensaje(buffer); if (i < 0) return 0;
        j = buscaFinMensaje(buffer);    if (j < 0) return 0;

        lngMensajeOk = j-i;

        if (lngMensajeOk > MSG_MAXLEN) return 0;

        posicion = i + 6;
        lngMsg = (buffer[posicion++]& 0xFF) + ((buffer[posicion++]& 0xFF) * 256);

        if ( lngMensajeOk < (lngMsg)) {
            return 0;
        }

        tipo = (buffer[posicion++] & 0xFF);
        id = (buffer[posicion++] & 0xFF) + ((buffer[posicion++] & 0xFF) * 256) + ((buffer[posicion++] & 0xFF) * 65536) + ((buffer[posicion++] & 0xFF) * 16777216);
        if (id==-1) return id;

        switch(tipo){
            case HIGH: {
                if(id==0) map.put(1,0);
                for (int k = 0; k < id; k++)
                    map.put((buffer[posicion++] & 0xFF), (buffer[posicion++] & 0xFF));
                break;
            }
            case URL:
                lngMsg = (buffer[posicion++] & 0xFF);
                byte[] mensaje = new byte[lngMsg];
                for (int m=0; m < lngMsg; m++){
                    mensaje[m] = buffer[posicion++];
                }
                url=new String(mensaje);
                break;
            case FRAME:
                x=(buffer[posicion++]& 0xFF) + ((buffer[posicion++]& 0xFF) * 256);
                y=(buffer[posicion++]& 0xFF) + ((buffer[posicion++]& 0xFF) * 256);
                break;
            //case CODECHECK:
                //long newTime=(buffer[posicion++] & 0xFF) + ((buffer[posicion++] & 0xFF) * 256) + ((buffer[posicion++] & 0xFF) * 65536) + ((buffer[posicion++] & 0xFF) * 16777216);
                //Res.getInstance().setGap((int)(System.currentTimeMillis()-newTime));
        }

        return id;
    }

    public String getUrl(){
        return url;
    }

    public SparseIntArray getMap(){
        return map;
    }

    public int getTipo(){
        return tipo;
    }
    public long getId(){return id;}

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    private int buscaInicioMensaje(byte[] buffer)
    {
        String str = new String(buffer);
        return str.indexOf("OBIAND");
    }

    private int buscaFinMensaje(byte[] buffer)
    {
        String str = new String(buffer);
        return str.indexOf("OBFAND");
    }
}