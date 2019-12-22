package com.obt.infoart;
import android.util.SparseIntArray;

/**
 * Created by Administrador on 27/02/2017.
 */

public class Frame {
    public static final byte LIKE=1;
    public static final byte HIGH=2;
    public static final byte BEACON=3;
    public static final byte SEARCH=4;
    private byte tipo;
    private long inicio;
    private byte idEsc;
    private SparseIntArray old;
    private SparseIntArray map;
    private static Frame instance;

    private Frame(){
        map=new SparseIntArray();
        old=new SparseIntArray();
    }

    public static Frame getInstance(){
        if (instance==null) instance=new Frame();
        return instance;
    }

    public void setTipo(byte tipo){
        this.tipo=tipo;
    }
    public void setInicio(){
        this.inicio=System.currentTimeMillis();
    }
    public void setIdEsc(byte idEsc){
        this.idEsc=idEsc;
    }

    public void addBeacon(int id,int distance){
        int aux=map.get(id);
        int value;
        if (aux!=0) value=(distance+aux)/2;
        else value=distance;
        map.put(id,value);
    }
    public void clearList(){
        old=map;
        map=new SparseIntArray();
    }

    public byte getTipo() {
        return tipo;
    }

    public byte getDuracion() {
        return (byte)((System.currentTimeMillis()-inicio)/1000);
    }

    public byte getIdEsc() {
        return idEsc;
    }

    public SparseIntArray getMap() {
        SparseIntArray aux=map.clone();
        int key,value,value2;
        for(int i=0;i<old.size();i++){
            key=old.keyAt(i);
            value=old.valueAt(i);
            value2=aux.get(key);
            if (value2!=0) aux.put(key,(value2+value)/2);
            else aux.put(key,value);
        }
        return aux;
    }

}
