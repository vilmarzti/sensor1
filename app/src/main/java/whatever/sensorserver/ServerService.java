package whatever.sensorserver;

import android.app.Service;
import android.content.Intent;
import android.graphics.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;


/**
 * Created by martin on 19.10.16.
 */

public class ServerService extends Service {
    private ServerSocket serverSocket;
    private Thread serverThread;
    private List<Sensor> sensorList;
    private SensorManager sensorManager;
    private boolean isRunning = true;
    private final LocalBinder lBinder = new LocalBinder();

    @Override
    public void onCreate() {
        try {
            serverSocket =  new ServerSocket(MainActivity.portNum);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
        serverThread = new Thread(new ServerThread());
        serverThread.start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return lBinder;
    }

    public void startServer(){
        isRunning = true;
        try {
            serverSocket = new ServerSocket(MainActivity.portNum);
        } catch (IOException e) {
            e.printStackTrace();
        }
        serverThread = new Thread(new ServerThread());
        serverThread.start();
    }

    public void stopServer(){
        isRunning = false;
        serverThread.interrupt();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ServerThread implements Runnable {

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept();

                    CommThread commt = new CommThread(socket);
                    Thread t = new Thread(commt);
                    t.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public class LocalBinder extends Binder{
       ServerService getService(){
           return ServerService.this;
       }
    }

    class CommThread implements Runnable{
        private Socket clientSocket;
        private BufferedReader input;
        private PrintStream output;
        private String htmlStart = "<html><head></head><body><ul>";
        private String htmlEnd = "</ul></body></html>";
        String route;

        public CommThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
                    String line = this.input.readLine();

                    while (line != null && !line.equals("")) {
                        if (line.startsWith("GET /")) {
                            int start = line.indexOf("/") + 1;
                            int end = line.indexOf(" ", start);
                            route = line.substring(start, end);
                            break;
                        }
                        line = this.input.readLine();
                    }

                    output = new PrintStream(this.clientSocket.getOutputStream());
                    if(route == null){
                            output.println("HTTP/1.0 500 ERROR");
                    }
                    else{
                        byte[] out = new byte[]{1};
                        try {
                            out = getOutputBytes(route);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        output.println("HTTP/1.0 200 OK");
                        output.println("Content-Type: text/html");
                        output.println("Content-Length: " + out.length);
                        output.println();
                        output.write(out);
                    }

                    output.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (output != null) {
                        output.close();
                    }
                    if (this.input != null) {
                        try {
                            this.input.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        private byte[] getOutputBytes(String route) throws InterruptedException {
            if (route.equals("")) {
                return getHTMLlist().getBytes();
            }
            else if(route.matches("\\d*\\.?\\d+") && Integer.parseInt(route) < sensorList.size()){
                return getSensorValue(Integer.parseInt(route)).getBytes();
            }
            else if(route.equals("vibrator")){
                vibrate();
               return "vibrating".getBytes();
            }
            else if(route.equals("flashlight")){
                take_photo();
                return "sound!".getBytes();
            }
            else{
                    return "Nothing to see here".getBytes();
            }

        }

        private void take_photo(){
            MediaPlayer mp = MainActivity.mediaPlayer;
            mp.setVolume(1.0f, 1.0f);
            mp.start();
        }
        private void vibrate(){
            Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vib.vibrate(5000);
        }

        private String getHTMLlist(){
            String html;
            Integer x = 0;
            html = htmlStart;

            for(Sensor sensor: sensorList){
                html += "<li><a  href=\"./" + x.toString() + "\">" + sensor.getName() + "</a></li>";
                x += 1;
            }
            html += "<p>";
            html += "<li><a href=\"./vibrator\"> vibrate </a></li>";
            html += "<li><a href=\"./flashlight\"> flashlight </a></li>";
            html += htmlEnd;
           return html;
        }

        private String getSensorValue(Integer position) throws InterruptedException {
            String html;
            Sensor sensor = sensorList.get(position);
            html = sensor.getName().toString();
            DataReader readData = new DataReader(sensor);
            (new Thread(readData)).start();
            synchronized (sensor){
                sensor.wait(500);
            }
            float[] values = readData.data;
            if(values == null){
                values =new float[]{0};
            }
            for(float val: values){
                html += "<li>" + val + "</li>";
            }
            html += "<a href=\"../\"> back </a>";
            return html;
        }

    }


    class DataReader implements Runnable, SensorEventListener{
        public float[] data;
        boolean waiting = true;
        Sensor mySensor;

        public DataReader(Sensor sensor){
            mySensor = sensor;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            data = event.values.clone();
            waiting = false;
            synchronized (mySensor){
                mySensor.notifyAll();
            }
            sensorManager.unregisterListener(this);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void run() {
            sensorManager.registerListener(this, mySensor, sensorManager.SENSOR_DELAY_NORMAL);
        }
    }
}
