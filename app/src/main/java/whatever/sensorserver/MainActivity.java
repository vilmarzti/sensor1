package whatever.sensorserver;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    static public Integer portNum = 9087;

    private TextView iptext;
    private TextView porttext;
    private Enumeration<NetworkInterface> interfaces;
    private NetworkInterface wlan;
    private ServerSocket serverSocket;
    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;

        iptext = (TextView) findViewById(R.id.iptext);
        porttext = (TextView) findViewById(R.id.porttext);


        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while (interfaces != null && interfaces.hasMoreElements()) {
            wlan = interfaces.nextElement();
            if (wlan.getName().equals("wlan0")) {
                InterfaceAddress adress = wlan.getInterfaceAddresses().get(1);
                if (adress != null) {
                    iptext.setText("IP adress:" + adress.getAddress().toString());
                }
            }
        }

        porttext.setText("PORT: " + portNum.toString());
        startService(new Intent(this, ServerService.class));
    }
 }
