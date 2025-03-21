package com.example.anzuo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
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
 * 蓝牙数据测试Activity
 * 提供六个按钮发送不同类型的数据进行测试
 */
public class BluetoothTestActivity extends AppCompatActivity {
    // 日志标签
    private static final String TAG = "user";
    
    // 权限请求码
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 100;
    
    // UI组件
    private Button connectButton;
    private Button quickConnectButton; // 快速连接按钮
    private TextView connectionStatusText;
    private TextView receivedDataText;
    
    // 正常数据测试按钮
    private Button normalTestButton1;
    private Button normalTestButton2;
    private Button normalTestButton3;
    
    // 极端数据测试按钮
    private Button extremeTestButton1;
    private Button extremeTestButton2;
    private Button extremeTestButton3;
    
    // 清空日志按钮
    private Button clearLogButton;
    
    // 蓝牙数据管理器
    private BluetoothDataManager bluetoothManager;
    
    // 选择的蓝牙设备
    private BluetoothDevice selectedDevice;
    
    // 处理UI更新的Handler
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    
    // 用于格式化时间
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());

    // SharedPreferences用于存储上次连接的设备地址
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "BluetoothPrefData";
    private static final String LAST_DEVICE_ADDRESS = "last_connected_device";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_test);
        
        // 初始化SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        
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
        
        // 添加快速连接按钮
        quickConnectButton = new Button(this);
        quickConnectButton.setText("快速连接上次设备");
        quickConnectButton.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"));
        quickConnectButton.setTextColor(android.graphics.Color.WHITE);
        quickConnectButton.setPadding(20, 10, 20, 10);
        
        // 将快速连接按钮添加到布局中（在连接按钮之后）
        android.view.ViewGroup layout = (android.view.ViewGroup) connectButton.getParent();
        layout.addView(quickConnectButton, layout.indexOfChild(connectButton) + 1);
        
        // 设置布局参数与连接按钮相同
        android.view.ViewGroup.LayoutParams params = quickConnectButton.getLayoutParams();
        params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
        
        // 设置边距
        if (params instanceof android.view.ViewGroup.MarginLayoutParams) {
            android.view.ViewGroup.MarginLayoutParams marginParams = (android.view.ViewGroup.MarginLayoutParams) params;
            marginParams.setMargins(0, 0, 0, 16); // 左、上、右、下边距，单位像素
        }
        
        quickConnectButton.setLayoutParams(params);
        
        connectionStatusText = findViewById(R.id.connectionStatusText);
        receivedDataText = findViewById(R.id.receivedDataText);
        
        // 获取正常数据测试按钮
        normalTestButton1 = findViewById(R.id.normalTestButton1);
        normalTestButton2 = findViewById(R.id.normalTestButton2);
        normalTestButton3 = findViewById(R.id.normalTestButton3);
        
        // 设置正常数据按钮文本更明确
        normalTestButton1.setText("发送测试数据1\n(0x10,0x20,0x30)");
        normalTestButton2.setText("发送测试数据2\n(0x80,0x40,0x20)");
        normalTestButton3.setText("发送测试数据3\n(0x12,0xAB,0x34)");
        
        // 获取极端数据测试按钮
        extremeTestButton1 = findViewById(R.id.extremeTestButton1);
        extremeTestButton2 = findViewById(R.id.extremeTestButton2);
        extremeTestButton3 = findViewById(R.id.extremeTestButton3);
        
        // 设置极端数据按钮文本更明确
        extremeTestButton1.setText("发送全FF数据\n(0xFF,0xFF,0xFF)");
        extremeTestButton2.setText("发送全00数据\n(0x00,0x00,0x00)");
        extremeTestButton3.setText("发送全55数据\n(0x55,0x55,0x55)");
        
        // 获取清空日志按钮
        clearLogButton = findViewById(R.id.clearLogButton);
        
        // 初始状态下禁用测试按钮
        setTestButtonsEnabled(false);
        
        // 添加测试说明到日志区
        addLogMessage("请先连接蓝牙设备，然后点击测试按钮发送数据\n每个按钮发送不同的测试数据:\n" +
                      "- 正常数据测试: 递增/递减/混合数据\n" +
                      "- 极端数据测试: 全FF/全00/全55(与数据头相同的值)");
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
                connectButton.setText("选择并连接蓝牙设备");
                connectionStatusText.setText("未连接蓝牙设备");
                setTestButtonsEnabled(false);
            } else {
                // 如果未连接，则检查权限并连接设备
                checkPermissionsAndConnectDevice();
            }
        });
        
        // 快速连接按钮
        quickConnectButton.setOnClickListener(v -> {
            // 从SharedPreferences获取上次连接的设备地址
            String lastDeviceAddress = sharedPreferences.getString(LAST_DEVICE_ADDRESS, "");
            
            if (lastDeviceAddress.isEmpty()) {
                Toast.makeText(this, "没有上次连接记录，请先选择设备连接", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 检查权限
            if (!checkBluetoothPermissions()) {
                return;
            }
            
            try {
                // 获取蓝牙适配器
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                
                if (bluetoothAdapter == null) {
                    Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 检查蓝牙是否启用
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH_PERMISSIONS);
                    return;
                }
                
                // 获取设备并连接
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(lastDeviceAddress);
                selectedDevice = device;
                
                Toast.makeText(this, "正在快速连接到上次设备...", Toast.LENGTH_SHORT).show();
                addLogMessage("正在快速连接到设备: " + (device.getName() != null ? device.getName() : lastDeviceAddress));
                
                // 连接设备
                if (bluetoothManager != null) {
                    bluetoothManager.connect(device);
                }
            } catch (Exception e) {
                Toast.makeText(this, "快速连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "快速连接失败: " + e.getMessage());
                addLogMessage("快速连接失败: " + e.getMessage());
            }
        });
        
        // 正常数据测试按钮1 - 递增数据
        normalTestButton1.setOnClickListener(v -> {
            byte command = 0x01;  // 状态命令
            byte[] data = {0x10, 0x20, 0x30};  // 三个递增数据
            sendTestData(command, data, "递增数据测试");
        });
        
        // 正常数据测试按钮2 - 递减数据
        normalTestButton2.setOnClickListener(v -> {
            byte command = 0x02;  // 配置命令
            byte[] data = {(byte)0x80, 0x40, 0x20};  // 三个递减数据
            sendTestData(command, data, "递减数据测试");
        });
        
        // 正常数据测试按钮3 - 混合数据
        normalTestButton3.setOnClickListener(v -> {
            byte command = 0x03;  // 传感器命令
            byte[] data = {0x12, (byte)0xAB, 0x34};  // 混合数据
            sendTestData(command, data, "混合数据测试");
        });
        
        // 极端数据测试按钮1 - 全FF
        extremeTestButton1.setOnClickListener(v -> {
            byte command = 0x04;  // 控制命令
            byte[] data = {(byte)0xFF, (byte)0xFF, (byte)0xFF};  // 全FF
            sendTestData(command, data, "极端数据测试 - 全FF");
        });
        
        // 极端数据测试按钮2 - 全00
        extremeTestButton2.setOnClickListener(v -> {
            byte command = 0x05;  // 诊断命令
            byte[] data = {0x00, 0x00, 0x00};  // 全00
            sendTestData(command, data, "极端数据测试 - 全00");
        });
        
        // 极端数据测试按钮3 - 全55 (与数据头相同)
        extremeTestButton3.setOnClickListener(v -> {
            byte command = 0x06;  // 特殊命令
            byte[] data = {0x55, 0x55, 0x55};  // 全55
            sendTestData(command, data, "极端数据测试 - 全55");
        });
        
        // 清空日志按钮
        clearLogButton.setOnClickListener(v -> {
            receivedDataText.setText("");
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
                receivedDataText.append(logMessage);
                
                // 滚动到底部
                scrollToBottom();
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
                            if (ContextCompat.checkSelfPermission(BluetoothTestActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) 
                                    != PackageManager.PERMISSION_GRANTED) {
                                connectionStatusText.setText("已连接设备，但缺少权限获取设备信息");
                                connectButton.setText("断开连接");
                                setTestButtonsEnabled(true);
                                addLogMessage("已连接到设备，但缺少权限获取设备信息");
                                return;
                            }
                        }
                        
                        // 有权限，可以正常获取设备名称
                        String deviceName = device.getName();
                        connectionStatusText.setText("已连接到: " + deviceName);
                        connectButton.setText("断开连接");
                        
                        // 确保测试按钮可见和可点击
                        setTestButtonsEnabled(true);
                        
                        // 强制刷新UI
                        normalTestButton1.invalidate();
                        normalTestButton2.invalidate();
                        normalTestButton3.invalidate();
                        extremeTestButton1.invalidate();
                        extremeTestButton2.invalidate();
                        extremeTestButton3.invalidate();
                        
                        addLogMessage("已连接到设备: " + deviceName);
                        
                        // 确保按钮点击监听器正确设置
                        ensureButtonListenersSetup();
                    } catch (SecurityException e) {
                        // 捕获可能的权限异常
                        Log.e(TAG, "获取设备名称权限被拒绝: " + e.getMessage());
                        connectionStatusText.setText("已连接设备，但无法获取设备信息");
                        connectButton.setText("断开连接");
                        setTestButtonsEnabled(true);
                        addLogMessage("已连接到设备，但无法获取设备信息");
                    }
                });
            }

            @Override
            public void onDisconnected() {
                uiHandler.post(() -> {
                    connectionStatusText.setText("未连接蓝牙设备");
                    connectButton.setText("连接设备");
                    setTestButtonsEnabled(false);
                    addLogMessage("蓝牙连接已断开");
                });
            }

            @Override
            public void onConnectionFailed(Exception e) {
                uiHandler.post(() -> {
                    connectionStatusText.setText("连接失败");
                    addLogMessage("连接失败: " + e.getMessage());
                    Toast.makeText(BluetoothTestActivity.this, 
                                  "连接失败: " + e.getMessage(), 
                                  Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * 检查蓝牙权限
     * @return 是否拥有所需的全部权限
     */
    private boolean checkBluetoothPermissions() {
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
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查权限并连接蓝牙设备
     */
    private void checkPermissionsAndConnectDevice() {
        // 检查权限
        if (!checkBluetoothPermissions()) {
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
                
                // 保存设备地址到SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(LAST_DEVICE_ADDRESS, selectedDevice.getAddress());
                editor.apply();
                
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
     * 发送测试数据
     * @param command 命令字节
     * @param data 数据字节数组
     * @param testName 测试名称
     */
    private void sendTestData(byte command, byte[] data, String testName) {
        if (bluetoothManager != null && bluetoothManager.isConnected()) {
            try {
                // 检查蓝牙连接权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) 
                            != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "缺少蓝牙连接权限，无法发送数据", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "发送数据时缺少权限");
                        return;
                    }
                }
                
                boolean success = bluetoothManager.sendData(command, data);
                
                if (success) {
                    // 格式化命令和数据为十六进制字符串
                    String cmdHex = String.format("%02X", command);
                    String dataHex = String.format("%02X %02X %02X", 
                                                 data[0] & 0xFF, 
                                                 data[1] & 0xFF, 
                                                 data[2] & 0xFF);
                    
                    // 添加发送日志
                    String timeStr = timeFormat.format(new Date());
                    String logMessage = timeStr + " [发送] " + testName + 
                                       ", 命令: 0x" + cmdHex + 
                                       ", 数据: " + dataHex + "\n";
                    
                    addLogMessage(logMessage);
                    
                    // 显示提示
                    Toast.makeText(this, "数据发送成功", Toast.LENGTH_SHORT).show();
                } else {
                    addLogMessage("数据发送失败");
                    Toast.makeText(this, "数据发送失败", Toast.LENGTH_SHORT).show();
                }
            } catch (SecurityException e) {
                Toast.makeText(this, "蓝牙权限被拒绝，无法发送数据", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "发送数据权限被拒绝: " + e.getMessage());
                addLogMessage("发送失败: 权限被拒绝");
            }
        } else {
            Toast.makeText(this, "请先连接蓝牙设备", Toast.LENGTH_SHORT).show();
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
        receivedDataText.append(message);
        
        // 滚动到底部
        scrollToBottom();
    }
    
    /**
     * 滚动到底部
     */
    private void scrollToBottom() {
        View scrollView = findViewById(R.id.receivedDataText);
        if (scrollView != null) {
            scrollView.post(() -> {
                scrollView.requestFocus();
                int scrollAmount = receivedDataText.getLayout().getLineTop(receivedDataText.getLineCount()) 
                                  - scrollView.getHeight();
                if (scrollAmount > 0) {
                    scrollView.scrollTo(0, scrollAmount);
                } else {
                    scrollView.scrollTo(0, 0);
                }
            });
        }
    }
    
    /**
     * 设置测试按钮是否启用
     * @param enabled 是否启用
     */
    private void setTestButtonsEnabled(boolean enabled) {
        // 设置正常数据测试按钮状态
        normalTestButton1.setEnabled(enabled);
        normalTestButton2.setEnabled(enabled);
        normalTestButton3.setEnabled(enabled);
        
        // 设置极端数据测试按钮状态
        extremeTestButton1.setEnabled(enabled);
        extremeTestButton2.setEnabled(enabled);
        extremeTestButton3.setEnabled(enabled);
        
        // 设置按钮的可点击性和视觉反馈
        normalTestButton1.setClickable(enabled);
        normalTestButton2.setClickable(enabled);
        normalTestButton3.setClickable(enabled);
        extremeTestButton1.setClickable(enabled);
        extremeTestButton2.setClickable(enabled);
        extremeTestButton3.setClickable(enabled);
        
        // 更改按钮的透明度和背景色，提供视觉反馈
        float alpha = enabled ? 1.0f : 0.5f;
        normalTestButton1.setAlpha(alpha);
        normalTestButton2.setAlpha(alpha);
        normalTestButton3.setAlpha(alpha);
        extremeTestButton1.setAlpha(alpha);
        extremeTestButton2.setAlpha(alpha);
        extremeTestButton3.setAlpha(alpha);
        
        // 更改按钮背景色
        int bgColor = enabled ? android.graphics.Color.parseColor("#4CAF50") : android.graphics.Color.parseColor("#CCCCCC");
        normalTestButton1.setBackgroundColor(bgColor);
        normalTestButton2.setBackgroundColor(bgColor);
        normalTestButton3.setBackgroundColor(bgColor);
        
        int extremeBgColor = enabled ? android.graphics.Color.parseColor("#FF5722") : android.graphics.Color.parseColor("#CCCCCC");
        extremeTestButton1.setBackgroundColor(extremeBgColor);
        extremeTestButton2.setBackgroundColor(extremeBgColor);
        extremeTestButton3.setBackgroundColor(extremeBgColor);
        
        // 记录按钮状态到日志
        if (enabled) {
            Log.d(TAG, "测试按钮已启用");
            Toast.makeText(this, "测试按钮已启用，可以开始测试", Toast.LENGTH_SHORT).show();
            addLogMessage("测试按钮已启用，可以开始测试发送数据");
        }
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

    /**
     * 确保按钮监听器被正确设置
     */
    private void ensureButtonListenersSetup() {
        // 正常数据测试按钮1 - 递增数据
        normalTestButton1.setOnClickListener(v -> {
            byte command = 0x01;  // 状态命令
            byte[] data = {0x10, 0x20, 0x30};  // 三个递增数据
            sendTestData(command, data, "递增数据测试");
        });
        
        // 正常数据测试按钮2 - 递减数据
        normalTestButton2.setOnClickListener(v -> {
            byte command = 0x02;  // 配置命令
            byte[] data = {(byte)0x80, 0x40, 0x20};  // 三个递减数据
            sendTestData(command, data, "递减数据测试");
        });
        
        // 正常数据测试按钮3 - 混合数据
        normalTestButton3.setOnClickListener(v -> {
            byte command = 0x03;  // 传感器命令
            byte[] data = {0x12, (byte)0xAB, 0x34};  // 混合数据
            sendTestData(command, data, "混合数据测试");
        });
        
        // 极端数据测试按钮1 - 全FF
        extremeTestButton1.setOnClickListener(v -> {
            byte command = 0x04;  // 控制命令
            byte[] data = {(byte)0xFF, (byte)0xFF, (byte)0xFF};  // 全FF
            sendTestData(command, data, "极端数据测试 - 全FF");
        });
        
        // 极端数据测试按钮2 - 全00
        extremeTestButton2.setOnClickListener(v -> {
            byte command = 0x05;  // 诊断命令
            byte[] data = {0x00, 0x00, 0x00};  // 全00
            sendTestData(command, data, "极端数据测试 - 全00");
        });
        
        // 极端数据测试按钮3 - 全55 (与数据头相同)
        extremeTestButton3.setOnClickListener(v -> {
            byte command = 0x06;  // 特殊命令
            byte[] data = {0x55, 0x55, 0x55};  // 全55
            sendTestData(command, data, "极端数据测试 - 全55");
        });
    }
} 