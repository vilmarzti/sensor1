package whatever.sensorserver;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    static public Integer portNum = 9087;

    private TextView iptext;
    private TextView porttext;
    private ServerService serverService;
    private boolean serviceIsRunning = true;
    private Button button;
    private Enumeration<NetworkInterface> interfaces;
    private NetworkInterface wlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iptext = (TextView) findViewById(R.id.iptext);

        porttext = (TextView) findViewById(R.id.porttext);
        porttext.setText("PORT: " + portNum.toString());

        button = (Button) findViewById(R.id.service_button);
        button.setText(R.string.turn_off);
        button.setOnClickListener(this);


        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while (interfaces != null && interfaces.hasMoreElements()) {
            wlan = interfaces.nextElement();
            if (wlan.getName().equals("wlan0")) {
                List<InterfaceAddress> adresses = wlan.getInterfaceAddresses();
                if(adresses.size() !=0){
                    for(InterfaceAddress adress: adresses){
                        if(adress != null &&
                                !adress.getAddress().isLoopbackAddress() &&
                                adress.getAddress().getClass() == Inet4Address.class){
                            iptext.setText("IP adress:" + adress.getAddress().toString());
                        }
                        int x = 1;
                    }
                }
            }
        }


        Intent intent = new Intent(this, ServerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onClick(View v) {
        if(serviceIsRunning){
            button.setText(R.string.turn_on);
            serviceIsRunning = false;
            serverService.stopServer();
        }
        else{
            button.setText(R.string.turn_off);
            serviceIsRunning = true;
            serverService.startServer();
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ServerService.LocalBinder binder = (ServerService.LocalBinder) service;
            serverService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

    };
}
