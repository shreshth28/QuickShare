package com.shreshth.quickshare;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageButton wifiImageToggleImageBtn;
    private Button discoverPeersBtn;
    private WifiManager wifiManager;

    private ListView peerDevicesListView;

    private ActivityResultLauncher<Intent> wifiStateResultLauncher;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifiP2pManagerChannel;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peerList=new ArrayList<WifiP2pDevice>();
    String [] deviceNameArray;
    WifiP2pDevice [] deviceArray;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeViews();
        initializeAppState();
        setViewStates();
        intializeListeners();

        wifiStateResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    setViewStates();
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                    }
                });
    }

    private void intializeListeners() {
        wifiImageToggleImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
                    wifiStateResultLauncher.launch(panelIntent);
                }
            }
    });
        discoverPeersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiP2pManager.discoverPeers(wifiP2pManagerChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Discovery Started", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(MainActivity.this, "Discovery Starting Failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    private void initializeAppState() {
        wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiP2pManager= (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        wifiP2pManagerChannel=wifiP2pManager.initialize(this,getMainLooper(),null);
        mReceiver=new WifiDirectBroadcastReceiver(wifiP2pManager, wifiP2pManagerChannel,this);
        mIntentFilter=new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    WifiP2pManager.PeerListListener peerListListener=new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            if(!peers.getDeviceList().equals(peerList)){
               peerList.clear();
               peerList.addAll(peers.getDeviceList());

               deviceNameArray=new String[peers.getDeviceList().size()];
               deviceArray=new WifiP2pDevice[peers.getDeviceList().size()];
               int index=0;

               for(WifiP2pDevice device: peers.getDeviceList()){
                   deviceNameArray[index]=device.deviceName;
                   deviceArray[index]=device;
                   index++;
               }
                ArrayAdapter<String> adapter=new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_expandable_list_item_1,deviceNameArray);
               peerDevicesListView.setAdapter(adapter);
            }
            if(peerList.size()==0){
                Toast.makeText(MainActivity.this, "No devices found", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void setViewStates(){
        if(wifiManager.isWifiEnabled()){
            wifiImageToggleImageBtn.setImageDrawable(getResources().getDrawable(R.drawable.wifi_on,this.getTheme()));
        }else{
            wifiImageToggleImageBtn.setImageDrawable(getResources().getDrawable(R.drawable.wifi_off,this.getTheme()));
        }
    }

    private void initializeViews() {
        wifiImageToggleImageBtn=findViewById(R.id.wifiToggleImageBtn);
        discoverPeersBtn=findViewById(R.id.discoverDevicesBtn);
        peerDevicesListView=findViewById(R.id.peerDevicesListView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver,mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }
}