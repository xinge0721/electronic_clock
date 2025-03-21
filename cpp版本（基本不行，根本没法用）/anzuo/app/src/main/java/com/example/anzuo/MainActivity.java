package com.example.anzuo;

// 导入必要的Android和Java类
import android.Manifest;  // 用于处理权限相关
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;  // 蓝牙适配器，用于管理蓝牙功能
import android.bluetooth.BluetoothDevice;   // 表示蓝牙设备
import android.bluetooth.BluetoothSocket;   // 用于蓝牙通信的socket
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;   // 用于检查权限
import android.os.Bundle;                   // Activity生命周期相关
import android.os.Handler;                  // 用于在子线程和主线程之间传递消息
import android.os.Looper;
import android.os.Message;                  // Handler消息对象
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;               // 按钮控件
import android.widget.EditText;             // 数据输入框
import android.widget.ListView;
import android.widget.TextView;             // 文本显示控件
import android.widget.Toast;                // 消息提示控件

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;  // Android基础Activity类
import androidx.core.app.ActivityCompat;         // 用于处理运行时权限
import androidx.core.content.ContextCompat;      // 用于检查权限状态

import java.io.IOException;    // 输入输出异常处理
import java.io.InputStream;    // 数据输入流
import java.io.OutputStream;   // 数据输出流
import java.util.Calendar;
import java.util.Set;         // 集合类
import java.util.UUID;        // 通用唯一识别码
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.lang.reflect.InvocationTargetException;

// MainActivity类，继承自AppCompatActivity
public class MainActivity extends AppCompatActivity {
    // 蓝牙通信的UUID，这是一个标准的串口通信UUID
    private static final UUID HC05_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // 请求启用蓝牙的请求码
    private static final int REQUEST_ENABLE_BT = 1;
    // 请求权限的请求码
    private static final int REQUEST_PERMISSION = 2;
    // 添加一个常量来标识蓝牙设备选择请求
    private static final int REQUEST_SELECT_DEVICE = 3;

    // 蓝牙相关对象声明
    private BluetoothAdapter bluetoothAdapter;    // 蓝牙适配器
    private BluetoothSocket bluetoothSocket;      // 蓝牙通信Socket
    private OutputStream outputStream;            // 数据输出流
    private InputStream inputStream;              // 数据输入流
    private boolean isConnected = false;          // 蓝牙连接状态标志
    private ExecutorService executorService;

    // UI控件声明
    private TextView tvStatus;      // 显示蓝牙状态的文本框
    private Button btnSearch;        // 搜索按钮
    private Button btnConnect;        // 连接/断开按钮
    private Button btnSend;         // 发送数据按钮
    private EditText editData1;    // 数据位一输入框
    private EditText editData2;    // 数据位二输入框
    private EditText editData3;    // 数据位三输入框
    private ListView lvDevices;      // 设备列表
    private ArrayAdapter<String> deviceListAdapter;

    private String targetDeviceAddress = null;

    // Handler用于在子线程中更新UI
    private final Handler handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 1) {  // 消息类型为1时处理
                byte[] readBuff = (byte[]) msg.obj;  // 获取接收到的字节数组
                String receivedData = bytesToHex(readBuff);  // 转换为16进制字符串
                tvStatus.setText("收到数据: " + receivedData);  // 更新UI显示
            }
            return true;
        }
    });

    // Activity创建时调用的方法
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // 设置界面布局

        executorService = Executors.newSingleThreadExecutor();
        initViews();     // 初始化UI控件
        initBluetooth();  // 初始化蓝牙
    }

    // 初始化所有UI控件
    private void initViews() {
        // 通过ID查找并绑定所有控件
        tvStatus = findViewById(R.id.tvStatus);
        btnSearch = findViewById(R.id.btnSearch);
        btnConnect = findViewById(R.id.btnConnect);
        btnSend = findViewById(R.id.btnSend);
        editData1 = findViewById(R.id.editData1);   // 初始化数据位一输入框
        editData2 = findViewById(R.id.editData2);   // 初始化数据位二输入框
        editData3 = findViewById(R.id.editData3);   // 初始化数据位三输入框
        lvDevices = findViewById(R.id.lvDevices);

        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        lvDevices.setAdapter(deviceListAdapter);

        btnSearch.setOnClickListener(v -> startSearch());
        btnConnect.setOnClickListener(v -> connectToDevice());
        btnSend.setOnClickListener(v -> sendTestData());

        lvDevices.setOnItemClickListener((parent, view, position, id) -> {
            String info = deviceListAdapter.getItem(position);
            if (info != null && info.contains("\n")) {
                targetDeviceAddress = info.split("\n")[1];
                tvStatus.setText("已选择设备: " + info.split("\n")[0]);
            }
        });
    }

    // 初始化蓝牙功能
    private void initBluetooth() {
        // 获取蓝牙适配器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // 如果设备不支持蓝牙，显示提示并关闭应用
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 检查蓝牙是否开启
        if (!bluetoothAdapter.isEnabled()) {
            // 请求用户开启蓝牙
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // 注册蓝牙搜索广播接收器
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    // 开始搜索蓝牙设备
    private void startSearch() {
        if (checkPermissions()) {
            deviceListAdapter.clear();
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
            tvStatus.setText("正在搜索设备...");
        }
    }

    // 检查并请求必要的蓝牙权限
    private boolean checkPermissions() {
        // 需要请求的权限数组
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
        };

        // 检查每个权限，如果有未授权的权限就请求
        boolean needRequestPermission = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                needRequestPermission = true;
                break;
            }
        }

        if (needRequestPermission) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION);
        }
        return !needRequestPermission;
    }

    // 连接或断开蓝牙设备
    private void connectToDevice() {
        if (targetDeviceAddress == null) {
            Toast.makeText(this, "请先选择要连接的设备", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isConnected) {
            disconnectDevice();
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(targetDeviceAddress);
        new Thread(() -> {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(HC05_UUID);
                bluetoothSocket.connect();
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                isConnected = true;

                runOnUiThread(() -> {
                    btnConnect.setText("断开连接");
                    tvStatus.setText("已连接到: " + device.getName());
                    startReadData();
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    tvStatus.setText("连接失败");
                });
                disconnectDevice();
            }
        }).start();
    }

    // 断开蓝牙连接
    private void disconnectDevice() {
        executorService.execute(() -> {
            isConnected = false;  // 更新连接状态
            try {
                // 关闭所有连接和流
                if (bluetoothSocket != null) {
                    bluetoothSocket.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                runOnUiThread(() -> {
                    tvStatus.setText("已断开连接");
                    btnConnect.setText("连接");
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> 
                    Toast.makeText(MainActivity.this,
                        "断开连接时出错: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

    // 将字节数组转换为16进制字符串
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));  // 每个字节转换为两位16进制
        }
        return sb.toString().trim();  // 返回转换后的字符串，去除首尾空格
    }

    // 开始读取数据的线程
    private void startReadData() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (isConnected) {
                try {
                    if (inputStream.available() > 0) {
                        int bytes = inputStream.read(buffer);
                        String data = bytesToHex(buffer);
                        runOnUiThread(() -> tvStatus.setText("收到数据: " + data));
                    }
                    Thread.sleep(100);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }).start();
    }

    // 发送测试数据
    private void sendTestData() {
        if (!isConnected || outputStream == null) {
            Toast.makeText(this, "请先连接设备", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                // 发送测试数据
                outputStream.write(new byte[]{0x01, 0x02, 0x03});
                outputStream.flush();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "发送成功", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "发送失败", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Activity销毁时调用
    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectDevice();  // 确保断开蓝牙连接
        executorService.shutdown();
        unregisterReceiver(receiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                // 权限已授予，重新尝试连接
                startSearch();
            } else {
                Toast.makeText(this, "需要位置权限才能搜索蓝牙设备", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "需要开启蓝牙才能使用此功能", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getName() != null) {
                    String deviceInfo = device.getName() + "\n" + device.getAddress();
                    if (!deviceListAdapter.getPosition(deviceInfo) >= 0) {
                        deviceListAdapter.add(deviceInfo);
                    }
                }
            }
        }
    };
}