package com.obt.infoart;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.LinkedList;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.Toast;

public class UDP {

    private final int TIMEOUT = 3000;
    private final int PORT = 8015;
    final static int MAX_INTENTOS = 5;
    //private String DIR_IP = "192.168.0.134";
    private final String DIR_IP = "www.obtcontrol.com";

    private IUDPCallback callback;
    private ILocationListener locationListener;
    private Thread t;
    private boolean alive;
    private LinkedList<TxMensaje> cola;
    private static UDP instance = null;
    private Res res;

    private UDP(){
        cola = new LinkedList<>();
        res = Res.getInstance();
    }

    public static UDP getInstance() {
        if (instance == null) instance = new UDP();
        return instance;
    }

    private TxMensaje initTx(int tipo) {
        return initTx(tipo, res.getPlace());
    }

    private TxMensaje initTx(int tipo, int place) {
        TxMensaje tx = new TxMensaje();
        tx.inicia();
        tx.setTipo(tipo);
        tx.addLong(res.getId());
        tx.addInt(place);
        tx.addTimeStamp();
        return tx;
    }

    private void sendFrame(SparseIntArray array, byte idEsc, byte duracion, byte tipoAc) {
        TxMensaje tx = initTx(RxMensaje.FRAME);
        byte num = (byte) array.size();
        tx.addByte(num);
        for (int i = 0; i < num; i++) {
            tx.addByte((byte) array.keyAt(i));
            tx.addByte((byte) array.valueAt(i));
        }
        tx.addByte(idEsc);
        tx.addByte(duracion);
        tx.addByte(tipoAc);
        cola.addLast(tx);
    }

    void sendLike(byte idBea, byte value) {
        TxMensaje tx = initTx(RxMensaje.LIKE);
        tx.addByte(idBea);
        tx.addByte(value);
        send(tx);
    }

    void sendHigh() {
        TxMensaje tx = initTx(RxMensaje.HIGH);
        send(tx);
    }

    String sendUrl(int place) {
        TxMensaje tx = initTx(RxMensaje.URL, place);
        RxMensaje rx = sendSync(tx);
        if (rx != null) return rx.getUrl();
        else return "";
    }

    long sendBuzon(String msg) {
        TxMensaje tx = initTx(RxMensaje.BUZON);
        tx.addStr(msg);
        RxMensaje rx = sendSync(tx);
        if (rx != null) return rx.getId();
        return -1;
    }

    void sendCode(long code) {
        TxMensaje tx = initTx(RxMensaje.CODECHECK);
        tx.addLong(code);
        send(tx);
    }

    private void send(final TxMensaje tx) {
        Thread hilo = new Thread(new Runnable() {
            public void run() {
                RxMensaje rx = sendSync(tx);
                if (callback != null) callback.received(rx);
            }
        });
        hilo.start();
    }

    private RxMensaje sendSync(final TxMensaje tx) {
        System.out.println("El tipo es: "+tx.getTipo());
        System.out.println("El time es: "+tx.getTime());
        DatagramSocket clientSocket=null;
        RxMensaje rx = null;
        try {
            clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(TIMEOUT);
            InetAddress IPAddress = InetAddress.getByName(DIR_IP);
            byte[] receiveData = new byte[1024];
            tx.finMensaje();
            DatagramPacket sendPacket = new DatagramPacket(tx.getMensaje(), tx.longitudMensaje(), IPAddress, PORT);
            clientSocket.send(sendPacket);
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            rx = new RxMensaje();
            rx.procesaMensaje(receivePacket.getData());
            return rx;
        } catch (IOException e) {
            Log.e("TIMEOUT", "En el mensaje con tx: " + tx.getTime());
        } finally {
            if (clientSocket!=null) clientSocket.close();
        }
        return null;
    }

    boolean checkConex() {
        ConnectivityManager connec = (ConnectivityManager) App.context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean hayConexion = false;
        NetworkInfo redmovil = connec
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo redwifi = connec
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        // if (redmovil.isConnected())
        // connec.setNetworkPreference(ConnectivityManager.TYPE_MOBILE);

        // Si hay alguna red conectada, entonces hay internet
        if (redwifi.isConnected() || redmovil.isConnected()) {
            hayConexion = true;
        }

        return hayConexion;
    }

    void setCallback(IUDPCallback call) {
        callback = call;
    }
    void setLocationListener(ILocationListener listener){ locationListener=listener;}
    void stopLoop(){
        alive=false;
    }
    void startLoop() {
        System.out.println("Start loop");
        MainActivity.instance.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.instance,"ID: "+res.getId(),Toast.LENGTH_SHORT).show();
            }
        });
        if (t == null || !t.isAlive() || !alive) {
            alive=true;
            cola.clear();
            t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    Frame f = Frame.getInstance();
                    TxMensaje tx;
                    RxMensaje rx;
                    int cont = 0;
                    while (alive) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }
                        cont++;
                        if (cont > 2) { //MAX_INTENTOS
                            byte idEsc = f.getIdEsc();
                            byte duracion;
                            if (idEsc == 0) duracion = 0;
                            else duracion = f.getDuracion();
                            sendFrame(f.getMap(), idEsc, duracion, f.getTipo());
                            f.clearList();
                            System.out.println("Envia frame");
                            cont = 0;
                        }
                        if (!cola.isEmpty()){
                            tx = cola.getFirst();
                            rx = sendSync(tx);
                            if (rx != null) {
                                int diff = (int) (rx.getId() - tx.getTime());
                                System.out.println("X: "+rx.getX()+" ,Y:"+rx.getY());
                                if(locationListener!=null) locationListener.update(rx.getX(),rx.getY());
                                if (diff==0) {
                                    System.out.println("El id es: " + rx.getId());
                                    cola.removeFirst();
                                } else{
                                    System.out.println("El id no coincide, es: " + rx.getId() + " y " + tx.getTime());
                                    Toast.makeText(MainActivity.instance,"El id2 no coincide, es: " + rx.getId() + " y " + tx.getTime(),Toast.LENGTH_SHORT).show();

                                }
                            } else{
                                System.out.println("TIMEOUT");
                                cont=3;
                                MainActivity.instance.runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast.makeText(MainActivity.instance,"TIMEOUT",Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                        }
                    }
                }
            });
            t.start();
        }
    }
}
