package whatever.sensorserver;

import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.telephony.PhoneNumberUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.regex.Pattern;

import static whatever.sensorserver.R.id.porttext;

/**
 * Created by martin on 19.10.16.
 */

public class ServerService extends Service {
    private ServerSocket serverSocket;
    private List<Sensor> sensorList;
    private SensorManager sensorManager;
    private float[] eventvalues;
    Activity mainActivity;

    @Override
    public void onCreate() {
        try {
            serverSocket =  new ServerSocket(MainActivity.portNum);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
        new Thread(new ServerThread()).start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class ServerThread implements Runnable {
        public void run() {

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept();

                    CommThread commt = new CommThread(socket);
                    new Thread(commt).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
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

                    byte[] out = new byte[0];
                    try {
                        out = getOutputBytes(route);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    output = new PrintStream(this.clientSocket.getOutputStream());
                    output.println("HTTP/1.0 200 OK");
                    output.println("Content-Type: text/html");
                    output.println("Content-Length: " + out.length);
                    output.println();
                    output.write(out);
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
            else if(route.matches("\\d*\\.?\\d+") && Integer.parseInt(route) <= sensorList.size()){
                return getSensorValue(Integer.parseInt(route)).getBytes();
            }
            else{
                return "Fuck".getBytes();
            }
        }

        private String getHTMLlist(){
            String html;
            Integer x = 0;
            html = htmlStart;

            for(Sensor sensor: sensorList){
                html += "<li><a  href=\"./" + x.toString() + "\">" + sensor.getName() + "</a></li>";
                x += 1;
            }
            html += htmlEnd;
           return html;
        }

        private String getSensorValue(Integer position) throws InterruptedException {
            String html;
            Sensor sensor = sensorList.get(position);
            DataReader test = new DataReader(sensor);
            Thread t = new Thread(test);
            t.start();
            html = sensor.getName().toString();
            synchronized (sensor){
                sensor.wait();
            }
            float[] values = test.data;
            for(float val: values){
                html += "<li>" + val + "</li>";
            }
            return html;
        }

    }

    class DataReader implements Runnable, SensorEventListener{
        public float[] data;
        boolean waiting = true;
        Sensor mySensor;

        public float[] returnData(){
            return data;
        }

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
            sensorManager.registerListener(this, mySensor, sensorManager.SENSOR_DELAY_FASTEST);
        }
    }

}
