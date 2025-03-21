package com.example.anzuo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 蓝牙连接Activity
 * 专门负责蓝牙设备的连接，连接成功后可手动跳转到测试界面
 */
public class BluetoothConnectActivity extends AppCompatActivity {
    // 日志标签
    private static final String TAG = "user";
    
    // 权限请求码
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 100;
    
    // UI组件
    private Button connectButton;
    private Button goToTestButton;
    private TextView connectionStatusText;
    private TextView logText;
    private Button clearLogButton;
    
    // 蓝牙数据管理器
    private BluetoothDataManager bluetoothManager;
    
    // 选择的蓝牙设备
    private BluetoothDevice selectedDevice;
    
    // 处理UI更新的Handler
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    
    // 用于格式化时间
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    
    // 是否已连接
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);
        
        // 初始化UI组件
        initViews();
        
        // 设置按钮点击监听器
        setupButtonListeners();
        
        // 初始化蓝牙数据管理器
        initBluetoothManager();
    }
    
    /**
     * 初始化UI组件
     */
    private void initViews() {
        // 获取连接相关组件
        connectButton = findViewById(R.id.connectButton);
        goToTestButton = findViewById(R.id.goToTestButton);
        connectionStatusText = findViewById(R.id.connectionStatusText);
        logText = findViewById(R.id.logText);
        clearLogButton = findViewById(R.id.clearLogButton);
        
        // 初始状态下禁用测试按钮
        goToTestButton.setEnabled(false);
    }
    
    /**
     * 设置按钮点击监听器
     */
    private void setupButtonListeners() {
        // 连接按钮
        connectButton.setOnClickListener(v -> {
            if (bluetoothManager != null && bluetoothManager.isConnected()) {
                // 如果已连接，则断开连接
                bluetoothManager.disconnect();
                connectButton.setText("连接设备");
                connectionStatusText.setText("未连接蓝牙设备");
                goToTestButton.setEnabled(false);
                isConnected = false;
            } else {
                // 如果未连接，则检查权限并连接设备
                checkPermissionsAndConnectDevice();
            }
        });
        
        // 跳转到测试界面按钮
        goToTestButton.setOnClickListener(v -> {
            if (isConnected) {
                // 创建Intent跳转到测试界面
                Intent intent = new Intent(BluetoothConnectActivity.this, BluetoothTestActivity.class);
                
                // 将蓝牙设备地址传递给测试界面
                if (selectedDevice != null) {
                    intent.putExtra("DEVICE_ADDRESS", selectedDevice.getAddress());
                }
                
                // 添加日志
                addLogMessage("正在跳转到测试界面...");
                
                // 启动测试界面
                startActivity(intent);
            } else {
                Toast.makeText(this, "请先连接蓝牙设备", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 清空日志按钮
        clearLogButton.setOnClickListener(v -> {
            logText.setText("");
        });
    }
    
    /**
     * 初始化蓝牙数据管理器
     */
    private void initBluetoothManager() {
        // 获取蓝牙适配器
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 创建蓝牙数据管理器，传入上下文以便权限检查
        bluetoothManager = new BluetoothDataManager(bluetoothAdapter, this);
        
        // 设置数据接收监听器
        bluetoothManager.setOnDataReceivedListener((command, data) -> {
            // 在UI线程处理接收到的数据
            uiHandler.post(() -> {
                // 格式化时间
                String timeStr = timeFormat.format(new Date());
                
                // 格式化命令和数据
                String cmdHex = String.format("%02X", command);
                String dataHex = String.format("%02X %02X %02X", 
                                             data[0] & 0xFF, 
                                             data[1] & 0xFF, 
                                             data[2] & 0xFF);
                
                // 构建日志消息
                String logMessage = timeStr + " [接收] 命令: 0x" + cmdHex + 
                                   ", 数据: " + dataHex + "\n";
                
                // 添加到日志显示区域
                addLogMessage(logMessage);
            });
        });
        
        // 设置连接状态变化监听器
        bluetoothManager.setOnConnectionStateChangeListener(new BluetoothDataManager.OnConnectionStateChangeListener() {
            @Override
            public void onConnected(BluetoothDevice device) {
                uiHandler.post(() -> {
                    try {
                        // 检查蓝牙连接权限
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ContextCompat.checkSelfPermission(BluetoothConnectActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) 
                                    != PackageManager.PERMISSION_GRANTED) {
                                connectionStatusText.setText("已连接设备，但缺少权限获取设备信息");
                                connectButton.setText("断开连接");
                                
                                // 启用跳转按钮
                                goToTestButton.setEnabled(true);
                                isConnected = true;
                                
                                addLogMessage("已连接到设备，但缺少权限获取设备信息");
                                return;
                            }
                        }
                        
                        // 有权限，可以正常获取设备名称
                        String deviceName = device.getName();
                        connectionStatusText.setText("已连接到: " + deviceName);
                        connectButton.setText("断开连接");
                        
                        // 启用跳转按钮
                        goToTestButton.setEnabled(true);
                        isConnected = true;
                        
                        addLogMessage("已连接到设备: " + deviceName);
                        
                        // 显示提示消息
                        Toast.makeText(BluetoothConnectActivity.this,
                                "已连接到设备，点击\"进入测试界面\"按钮开始测试",
                                Toast.LENGTH_LONG).show();
                    } catch (SecurityException e) {
                        // 捕获可能的权限异常
                        Log.e(TAG, "获取设备名称权限被拒绝: " + e.getMessage());
                        connectionStatusText.setText("已连接设备，但无法获取设备信息");
                        connectButton.setText("断开连接");
                        
                        // 启用跳转按钮
                        goToTestButton.setEnabled(true);
                        isConnected = true;
                        
                        addLogMessage("已连接到设备，但无法获取设备信息");
                    }
                });
            }

            @Override
            public void onDisconnected() {
                uiHandler.post(() -> {
                    connectionStatusText.setText("未连接蓝牙设备");
                    connectButton.setText("连接设备");
                    
                    // 禁用跳转按钮
                    goToTestButton.setEnabled(false);
                    isConnected = false;
                    
                    addLogMessage("蓝牙连接已断开");
                });
            }

            @Override
            public void onConnectionFailed(Exception e) {
                uiHandler.post(() -> {
                    connectionStatusText.setText("连接失败");
                    
                    // 禁用跳转按钮
                    goToTestButton.setEnabled(false);
                    isConnected = false;
                    
                    addLogMessage("连接失败: " + e.getMessage());
                    Toast.makeText(BluetoothConnectActivity.this, 
                                  "连接失败: " + e.getMessage(), 
                                  Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * 检查权限并连接蓝牙设备
     */
    private void checkPermissionsAndConnectDevice() {
        // 需要检查的权限列表
        List<String> permissionsNeeded = new ArrayList<>();
        
        // 对于Android 12 (API 31)及以上版本需要的权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.BLUETOOTH_CONNECT);
            }
            
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.BLUETOOTH_SCAN);
            }
        }
        
        // 位置权限（低版本Android需要）
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        
        // 如果需要权限，则请求
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                REQUEST_BLUETOOTH_PERMISSIONS);
            return;
        }
        
        // 已有权限，显示设备选择对话框
        showDeviceSelectionDialog();
    }
    
    /**
     * 显示设备选择对话框
     */
    private void showDeviceSelectionDialog() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 检查蓝牙是否启用
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) 
                        != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "缺少蓝牙连接权限", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH_PERMISSIONS);
                return;
            }
            
            // 获取已配对设备前检查权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) 
                        != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "缺少蓝牙连接权限，无法获取已配对设备", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            // 获取已配对设备
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            
            if (pairedDevices.size() == 0) {
                Toast.makeText(this, "没有已配对的蓝牙设备", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 构建设备列表
            final BluetoothDevice[] devices = pairedDevices.toArray(new BluetoothDevice[0]);
            String[] deviceNames = new String[devices.length];
            
            for (int i = 0; i < devices.length; i++) {
                deviceNames[i] = devices[i].getName() + " - " + devices[i].getAddress();
            }
            
            // 创建并显示设备选择对话框
            new AlertDialog.Builder(this)
                .setTitle("选择设备")
                .setItems(deviceNames, (dialog, which) -> {
                    // 保存选择的设备
                    selectedDevice = devices[which];
                    
                    // 连接到选择的设备
                    connectToSelectedDevice();
                })
                .setNegativeButton("取消", null)
                .show();
                
        } catch (SecurityException e) {
            Toast.makeText(this, "获取蓝牙设备需要权限", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "获取蓝牙设备列表时权限被拒绝: " + e.getMessage());
        }
    }
    
    /**
     * 连接到选择的设备
     */
    private void connectToSelectedDevice() {
        if (selectedDevice != null && bluetoothManager != null) {
            try {
                // 检查蓝牙连接权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) 
                            != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "缺少蓝牙连接权限", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "连接设备时缺少权限");
                        return;
                    }
                }
                
                Toast.makeText(this, "正在连接到 " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
                addLogMessage("正在连接到设备: " + selectedDevice.getName());
                bluetoothManager.connect(selectedDevice);
            } catch (SecurityException e) {
                Toast.makeText(this, "蓝牙连接权限被拒绝", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "蓝牙连接权限被拒绝: " + e.getMessage());
                addLogMessage("连接失败: 权限被拒绝");
            }
        }
    }
    
    /**
     * 添加日志消息
     * @param message 要添加的消息
     */
    private void addLogMessage(String message) {
        // 如果不是以换行结尾，则添加换行符
        if (!message.endsWith("\n")) {
            message += "\n";
        }
        
        // 添加消息到日志区域
        logText.append(message);
    }
    
    /**
     * 权限请求结果回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                // 权限已授予，显示设备选择对话框
                showDeviceSelectionDialog();
            } else {
                Toast.makeText(this, "未授予必要权限，无法使用蓝牙功能", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Activity结果回调
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            try {
                // 检查蓝牙权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) 
                            != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "缺少蓝牙连接权限", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                
                if (resultCode == RESULT_OK) {
                    // 蓝牙已启用，显示设备选择对话框
                    showDeviceSelectionDialog();
                } else {
                    Toast.makeText(this, "蓝牙未启用，无法使用蓝牙功能", Toast.LENGTH_SHORT).show();
                }
            } catch (SecurityException e) {
                Toast.makeText(this, "蓝牙权限被拒绝", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "处理蓝牙启用结果时权限被拒绝: " + e.getMessage());
            }
        }
    }
    
    /**
     * Activity销毁回调
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        try {
            // 断开蓝牙连接并释放资源
            if (bluetoothManager != null) {
                bluetoothManager.disconnect();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "断开蓝牙连接时权限被拒绝: " + e.getMessage());
        }
    }
} 