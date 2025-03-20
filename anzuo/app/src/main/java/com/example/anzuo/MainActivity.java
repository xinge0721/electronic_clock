package com.example.anzuo;

// 导入必要的Android和Java类
import android.Manifest;  // 用于处理权限相关
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;  // 蓝牙适配器，用于管理蓝牙功能
import android.bluetooth.BluetoothDevice;   // 表示蓝牙设备
import android.bluetooth.BluetoothSocket;   // 用于蓝牙通信的socket
import android.content.Intent;
import android.content.pm.PackageManager;   // 用于检查权限
import android.os.Bundle;                   // Activity生命周期相关
import android.os.Handler;                  // 用于在子线程和主线程之间传递消息
import android.os.Looper;
import android.os.Message;                  // Handler消息对象
import android.text.TextUtils;
import android.widget.Button;               // 按钮控件
import android.widget.EditText;             // 数据输入框
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

// MainActivity类，继承自AppCompatActivity
public class MainActivity extends AppCompatActivity {
    // 蓝牙通信的UUID，这是一个标准的串口通信UUID
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // 请求启用蓝牙的请求码
    private static final int REQUEST_ENABLE_BT = 1;
    // 请求权限的请求码
    private static final int PERMISSION_REQUEST_CODE = 2;

    // 蓝牙相关对象声明
    private BluetoothAdapter bluetoothAdapter;    // 蓝牙适配器
    private BluetoothSocket bluetoothSocket;      // 蓝牙通信Socket
    private OutputStream outputStream;            // 数据输出流
    private InputStream inputStream;              // 数据输入流
    private boolean isConnected = false;          // 蓝牙连接状态标志
    private ExecutorService executorService;

    // UI控件声明
    private TextView statusText;      // 显示蓝牙状态的文本框
    private TextView receivedDataText; // 显示接收数据的文本框
    private Button btnConnect;        // 连接/断开按钮
    private Button btnSend1;         // 发送数据按钮1
    private Button btnSend2;         // 发送数据按钮2
    private Button btnSend3;         // 发送数据按钮3
    private Button btnSendCustom;    // 发送自定义数据按钮
    private Button btnSendTime;      // 发送当前时间按钮
    private Button btnSendFF;         // 发送FF数据按钮
    private EditText editData1;    // 数据位一输入框
    private EditText editData2;    // 数据位二输入框
    private EditText editData3;    // 数据位三输入框

    // Handler用于在子线程中更新UI
    private final Handler handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 1) {  // 消息类型为1时处理
                byte[] readBuff = (byte[]) msg.obj;  // 获取接收到的字节数组
                String receivedData = bytesToHex(readBuff);  // 转换为16进制字符串
                receivedDataText.setText("接收到的数据: " + receivedData);  // 更新UI显示
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
        initializeViews();     // 初始化UI控件
        setupBluetooth();      // 设置蓝牙
        setupClickListeners(); // 设置按钮点击监听器
    }

    // 初始化所有UI控件
    private void initializeViews() {
        // 通过ID查找并绑定所有控件
        statusText = findViewById(R.id.statusText);
        receivedDataText = findViewById(R.id.receivedDataText);
        btnConnect = findViewById(R.id.btnConnect);
        btnSend1 = findViewById(R.id.btnSend1);
        btnSend2 = findViewById(R.id.btnSend2);
        btnSend3 = findViewById(R.id.btnSend3);
        btnSendCustom = findViewById(R.id.btnSendCustom);
        btnSendTime = findViewById(R.id.btnSendTime);
        btnSendFF = findViewById(R.id.btnSendFF);
        editData1 = findViewById(R.id.editData1);   // 初始化数据位一输入框
        editData2 = findViewById(R.id.editData2);   // 初始化数据位二输入框
        editData3 = findViewById(R.id.editData3);   // 初始化数据位三输入框
    }

    // 初始化蓝牙功能
    private void setupBluetooth() {
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                Toast.makeText(this, "请手动开启蓝牙", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        checkAndRequestPermissions();  // 检查并请求必要的权限
    }

    // 设置按钮点击事件
    private void setupClickListeners() {
        // 连接/断开按钮点击事件
        btnConnect.setOnClickListener(v -> connectToBluetooth());

        // 特殊功能1按钮 - 数据位全为0x00
        btnSend1.setOnClickListener(v -> {
            byte[] data = BluetoothProtocol.generateCommand(
                (byte) 0x01,  // 主指令1
                (byte) 0x00,  // 数据位全为0x00
                (byte) 0x00,
                (byte) 0x00
            );
            sendData(data);
        });

        // 特殊功能2按钮 - 数据位全为0x00
        btnSend2.setOnClickListener(v -> {
            byte[] data = BluetoothProtocol.generateCommand(
                (byte) 0x02,  // 主指令2
                (byte) 0x00,  // 数据位全为0x00
                (byte) 0x00,
                (byte) 0x00
            );
            sendData(data);
        });

        // 特殊功能3按钮 - 数据位全为0x00
        btnSend3.setOnClickListener(v -> {
            byte[] data = BluetoothProtocol.generateCommand(
                (byte) 0x03,  // 主指令3
                (byte) 0x00,  // 数据位全为0x00
                (byte) 0x00,
                (byte) 0x00
            );
            sendData(data);
        });

        // 发送自定义数据按钮
        btnSendCustom.setOnClickListener(v -> {
            if (validateInputs()) {
                byte[] data = BluetoothProtocol.generateCommand(
                    (byte) 0x04,  // 主指令4
                    getDataByte1(),
                    getDataByte2(),
                    getDataByte3()
                );
                sendData(data);
            }
        });

        // 发送当前时间按钮
        btnSendTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            byte[] data = BluetoothProtocol.generateCommand(
                (byte) 0x05,  // 主指令5
                (byte) calendar.get(Calendar.HOUR_OF_DAY),  // 时
                (byte) calendar.get(Calendar.MINUTE),       // 分
                (byte) calendar.get(Calendar.SECOND)        // 秒
            );
            sendData(data);
        });

        // 发送FF数据按钮
        btnSendFF.setOnClickListener(v -> {
            byte[] data = BluetoothProtocol.generateCommand(
                (byte) 0x06,  // 主指令6
                (byte) 0xFF,  // 数据位全为0xFF
                (byte) 0xFF,
                (byte) 0xFF
            );
            sendData(data);
        });
    }

    // 验证输入数据是否有效
    private boolean validateInputs() {
        String data1Str = editData1.getText().toString();
        String data2Str = editData2.getText().toString();
        String data3Str = editData3.getText().toString();

        if (TextUtils.isEmpty(data1Str) || TextUtils.isEmpty(data2Str) || TextUtils.isEmpty(data3Str)) {
            Toast.makeText(this, "请输入所有数据", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            int data1 = Integer.parseInt(data1Str);
            int data2 = Integer.parseInt(data2Str);
            int data3 = Integer.parseInt(data3Str);

            if (data1 < 0 || data1 > 255 || data2 < 0 || data2 > 255 || data3 < 0 || data3 > 255) {
                Toast.makeText(this, "数据必须在0-255之间", Toast.LENGTH_SHORT).show();
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // 获取数据位一的值
    private byte getDataByte1() {
        return (byte) Integer.parseInt(editData1.getText().toString());
    }

    // 获取数据位二的值
    private byte getDataByte2() {
        return (byte) Integer.parseInt(editData2.getText().toString());
    }

    // 获取数据位三的值
    private byte getDataByte3() {
        return (byte) Integer.parseInt(editData3.getText().toString());
    }

    // 检查并请求必要的蓝牙权限
    private void checkAndRequestPermissions() {
        // 需要请求的权限数组
        String[] permissions = {
                Manifest.permission.BLUETOOTH,           // 基本蓝牙功能
                Manifest.permission.BLUETOOTH_ADMIN,     // 蓝牙管理功能
                Manifest.permission.BLUETOOTH_CONNECT,   // 蓝牙连接权限（Android 12及以上需要）
                Manifest.permission.BLUETOOTH_SCAN,      // 蓝牙扫描权限（Android 12及以上需要）
                Manifest.permission.ACCESS_FINE_LOCATION,    // 精确位置权限（蓝牙扫描需要）
                Manifest.permission.ACCESS_COARSE_LOCATION   // 粗略位置权限
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
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    // 连接或断开蓝牙设备
    private void connectToBluetooth() {
        if (!isConnected) {  // 如果当前未连接
            // 检查蓝牙连接权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                // 请求蓝牙连接权限
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    PERMISSION_REQUEST_CODE);
                return;
            }

            // 在主线程中显示进度对话框
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在连接蓝牙设备...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // 在后台线程中执行蓝牙连接
            new Thread(() -> {
                try {
                    // 获取已配对的设备列表
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    if (pairedDevices.isEmpty()) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "没有已配对的蓝牙设备", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    // 获取第一个配对的设备
                    BluetoothDevice device = pairedDevices.iterator().next();
                    
                    // 创建蓝牙Socket
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    
                    // 确保蓝牙发现过程不会干扰连接
                    if (bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.cancelDiscovery();
                    }

                    // 尝试连接
                    bluetoothSocket.connect();

                    // 获取输入输出流
                    outputStream = bluetoothSocket.getOutputStream();
                    inputStream = bluetoothSocket.getInputStream();
                    
                    // 更新连接状态
                    isConnected = true;
                    
                    // 在主线程更新UI
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        statusText.setText("蓝牙状态: 已连接到 " + device.getName());
                        btnConnect.setText("断开连接");
                        Toast.makeText(this, "已连接到设备: " + device.getName(), Toast.LENGTH_SHORT).show();
                    });

                    // 开始监听数据
                    startListening();

                } catch (IOException e) {
                    e.printStackTrace();
                    // 连接失败，清理资源
                    try {
                        if (bluetoothSocket != null) {
                            bluetoothSocket.close();
                        }
                    } catch (IOException closeException) {
                        closeException.printStackTrace();
                    }
                    
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "连接失败，请重试", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();

        } else {  // 如果当前已连接，则断开连接
            disconnectBluetooth();
        }
    }

    // 开始监听接收数据的线程
    private void startListening() {
        Thread thread = new Thread(() -> {
            byte[] buffer = new byte[8];  // 8字节的接收缓冲区
            while (isConnected) {  // 当蓝牙保持连接时循环
                try {
                    if (inputStream.available() > 0) {  // 如果有可用数据
                        inputStream.read(buffer);  // 读取数据
                        if (BluetoothProtocol.validateReceivedData(buffer)) {
                            // 发送消息到Handler更新UI
                            Message msg = handler.obtainMessage(1, buffer.clone());
                            handler.sendMessage(msg);
                        }
                    }
                    Thread.sleep(100); // 添加短暂延时，避免过度占用CPU
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    break;  // 发生异常时退出循环
                }
            }
        });
        thread.start();  // 启动接收线程
    }

    // 发送数据的方法
    private void sendData(byte[] data) {
        if (!isConnected) {
            Toast.makeText(this, "请先连接蓝牙设备", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            try {
                outputStream.write(data);     // 写入数据
                outputStream.flush();         // 刷新输出流
                runOnUiThread(() -> 
                    Toast.makeText(MainActivity.this,
                        "数据发送成功",
                        Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                        "发送数据失败: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                    // 发送失败可能是因为连接断开，重置连接状态
                    isConnected = false;
                    statusText.setText("蓝牙状态: 未连接");
                });
            }
        });
    }

    // 断开蓝牙连接
    private void disconnectBluetooth() {
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
                    statusText.setText("蓝牙状态: 未连接");
                    Toast.makeText(MainActivity.this,
                        "蓝牙已断开连接",
                        Toast.LENGTH_SHORT).show();
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

    // Activity销毁时调用
    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectBluetooth();  // 确保断开蓝牙连接
        executorService.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                // 权限已授予，重新尝试连接
                connectToBluetooth();
            } else {
                Toast.makeText(this, "需要蓝牙权限以连接设备", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // 蓝牙已开启，继续检查权限
                checkAndRequestPermissions();
            } else {
                Toast.makeText(this, "需要开启蓝牙才能使用此功能", Toast.LENGTH_SHORT).show();
            }
        }
    }
}