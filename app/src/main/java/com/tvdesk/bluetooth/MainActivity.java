package com.tvdesk.bluetooth;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.tvdesk.bluetooth.databinding.ActivityMainBinding;

@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity {
    private final String TAG=MainActivity.class.getSimpleName();

    private ActivityResultLauncher<Intent> enableBluetooth;//打开蓝牙意图
    private ActivityResultLauncher<String> requestBluetoothConnect;
    private ActivityMainBinding binding;

    //获取系统蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter;
    //扫描者
    private BluetoothLeScanner scanner;
    //是否正在扫描
    boolean isScanning = false;

    private ActivityResultLauncher<String> requestBluetoothScan;

    private void registerIntent(){
        //打开蓝牙意图
        enableBluetooth=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode()== Activity.RESULT_OK){
                BluetoothManager manager= (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
                mBluetoothAdapter = manager.getAdapter();
                scanner = mBluetoothAdapter.getBluetoothLeScanner();
                showMsg("蓝牙已打开");
            }else {
                showMsg("蓝牙未打开");
            }
        });

        //请求打开系统蓝牙
        requestBluetoothConnect=registerForActivityResult(new ActivityResultContracts.RequestPermission(),result -> {
           if (result){
               enableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
           }else {
               showMsg("Android12中未获取此权限，无法打开蓝牙。");
           }
        });

        requestBluetoothScan=registerForActivityResult(new ActivityResultContracts.RequestPermission(),result -> {
            if (result){
                startScan();
            }else {
                showMsg("Android12中未获取此权限，则无法扫描蓝牙。");
            }
        });
    }

    //判断系统蓝牙是否开启
    private boolean isOpenBluetooth(){
        BluetoothManager bluetoothManager= (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        if (adapter==null){
            return false;
        }
        return adapter.isEnabled();
    }

    @SuppressLint("ObsoleteSdkInt")
    private boolean isAndroid12(){
        return Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    /*
    检查此权限是否授予的方法和一个显示Toast的方法
     */

    private boolean hasPermission(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
    private void showMsg(CharSequence msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerIntent();
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();
    }

    /*
    首先判断蓝牙是否已经打开了，打开了就不往下执行，
    没打开，再判断当前是否为Android12，不是就直接打开系统蓝牙，
    是Android12，再去检查是否授予BLUETOOTH_CONNECT权限，授予了就打开系统蓝牙，没有授予就去请求此权限
     */
    @SuppressLint("InlinedApi")
    private void initView(){
        if (isOpenBluetooth()){
            BluetoothManager manager= (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            mBluetoothAdapter=manager.getAdapter();
            scanner=mBluetoothAdapter.getBluetoothLeScanner();
        }

        binding.btnOpenBluetooth.setOnClickListener(v->{
            if (isOpenBluetooth()){
                showMsg("蓝牙已打开");
                return;
            }
            if (isAndroid12()){
                if (hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)){
                    //打开蓝牙
                    enableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                }else {
                    //请求权限
                    requestBluetoothConnect.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
                }
                return;
            }
            enableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        });

        //扫描蓝牙按钮点击事件
        binding.btnScanBluetooth.setOnClickListener(v -> {
            if (isAndroid12()) {
                if (!hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                    requestBluetoothConnect.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
                    return;
                }
                if (hasPermission(android.Manifest.permission.BLUETOOTH_SCAN)) {
                    //扫描或者停止扫描
                    if (isScanning) stopScan();
                    else startScan();
                } else {
                    //请求权限
                    requestBluetoothScan.launch(android.Manifest.permission.BLUETOOTH_SCAN);
                }
            }
        });
    }


    //扫描结果回调
    private final ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            Log.d(TAG, "name: " + device.getName()+ ", address: " + device.getAddress());
        }
    };

    @SuppressLint("MissingPermission")
    private void startScan() {
        if (!isScanning) {
            scanner.startScan(scanCallback);
            isScanning = true;
            binding.btnScanBluetooth.setText("停止扫描");
        }
    }

    @SuppressLint("MissingPermission")
    private void stopScan() {
        if (isScanning) {
            scanner.stopScan(scanCallback);
            isScanning = false;
            binding.btnScanBluetooth.setText("扫描蓝牙");
        }
    }




}