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
import android.view.ViewParent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;

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
    
    // 功能测试按钮
    private Button timeButton;      // 获取时间按钮
    private Button temperatureButton; // 获取温度按钮
    private Button customTimeButton;  // 自定义时间按钮
    private Button resetTimeButton;   // 时间复位按钮
    private Button resetTempButton;   // 温度复位按钮
    private Button autoTimeButton;    // 自动发送时间按钮
    
    // 时间输入框
    private TextView timeInputLabel;
    private android.widget.EditText hourInput;
    private android.widget.EditText minuteInput;
    private android.widget.EditText secondInput;
    
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
    
    // 自动发送时间相关
    private boolean isAutoSendingTime = false;
    private java.util.Timer autoSendTimer;
    private java.util.TimerTask autoSendTask;

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
        try {
            // 获取连接相关组件
            connectButton = findViewById(R.id.connectButton);
            if (connectButton == null) {
                Log.e(TAG, "找不到连接按钮，布局可能有问题");
                return;
            }
            
            connectionStatusText = findViewById(R.id.connectionStatusText);
            receivedDataText = findViewById(R.id.receivedDataText);
            if (connectionStatusText == null || receivedDataText == null) {
                Log.e(TAG, "找不到状态文本或数据文本视图，布局可能有问题");
                return;
            }
            
            // 添加快速连接按钮
            quickConnectButton = new Button(this);
            quickConnectButton.setText("快速连接上次设备");
            quickConnectButton.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"));
            quickConnectButton.setTextColor(android.graphics.Color.WHITE);
            quickConnectButton.setPadding(20, 10, 20, 10);
            
            // 将快速连接按钮添加到布局中（在连接按钮之后）
            android.view.ViewGroup layout = (android.view.ViewGroup) connectButton.getParent();
            if (layout != null) {
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
            } else {
                Log.e(TAG, "无法获取connectButton的父布局");
                // 继续执行，虽然快速连接按钮无法添加
            }
            
            // 查找原有按钮的父布局，以便替换
            android.view.ViewGroup buttonsLayout = null;
            boolean buttonLayoutCreated = false;
            
            try {
                // 尝试查找按钮布局区域
                Button existingButton = findViewById(R.id.normalTestButton1);
                if (existingButton != null) {
                    // 获取按钮的父布局
                    buttonsLayout = (android.view.ViewGroup) existingButton.getParent();
                    
                    // 清除原有的所有测试按钮
                    if (buttonsLayout != null) {
                        buttonsLayout.removeAllViews();
                    } else {
                        Log.e(TAG, "找到按钮但无法获取其父布局");
                    }
                } else {
                    Log.e(TAG, "找不到normalTestButton1按钮");
                }
            } catch (Exception e) {
                Log.e(TAG, "查找原有按钮失败: " + e.getMessage());
            }
            
            // 如果仍然没有找到布局，尝试创建一个新的
            if (buttonsLayout == null) {
                Log.w(TAG, "创建新的按钮布局");
                buttonLayoutCreated = true;
                buttonsLayout = new android.widget.LinearLayout(this);
                ((android.widget.LinearLayout) buttonsLayout).setOrientation(android.widget.LinearLayout.VERTICAL);
                
                // 找到接收数据的文本框，将新布局添加到其前面
                android.view.ViewGroup mainLayout = null;
                if (receivedDataText != null) {
                    ViewParent parentObj = receivedDataText.getParent();
                    ScrollView scrollView = null;
                    
                    // 向上查找ScrollView
                    while (parentObj != null) {
                        if (parentObj instanceof ScrollView) {
                            scrollView = (ScrollView) parentObj;
                            break;
                        }
                        parentObj = parentObj.getParent();
                    }
                    
                    // 如果找到了ScrollView，获取它的父View
                    if (scrollView != null) {
                        ViewParent grandParentObj = scrollView.getParent();
                        if (grandParentObj instanceof android.view.ViewGroup) {
                            mainLayout = (android.view.ViewGroup) grandParentObj;
                        }
                    }
                }
                
                if (mainLayout != null) {
                    int index = -1;
                    for (int i = 0; i < mainLayout.getChildCount(); i++) {
                        if (mainLayout.getChildAt(i) instanceof android.widget.ScrollView) {
                            index = i;
                            break;
                        }
                    }
                    
                    if (index != -1) {
                        mainLayout.addView(buttonsLayout, index);
                    } else {
                        // 如果找不到ScrollView，添加到顶部
                        mainLayout.addView(buttonsLayout, 0);
                    }
                } else {
                    Log.e(TAG, "无法找到主布局，无法添加新的按钮布局");
                    // 如果找不到适合的位置，可能需要更复杂的布局处理
                    // 这里简化处理，直接返回，避免后续操作导致崩溃
                    return;
                }
            }
            
            if (buttonsLayout == null) {
                Log.e(TAG, "无法创建或找到按钮布局，无法继续初始化UI");
                return;
            }
            
            // 创建获取时间按钮
            timeButton = new Button(this);
            timeButton.setText("获取当前时间并发送");
            timeButton.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"));
            timeButton.setTextColor(android.graphics.Color.WHITE);
            
            // 创建获取温度按钮
            temperatureButton = new Button(this);
            temperatureButton.setText("获取当前温度并发送");
            temperatureButton.setBackgroundColor(android.graphics.Color.parseColor("#FF5722"));
            temperatureButton.setTextColor(android.graphics.Color.WHITE);
            
            // 添加时间输入标签
            timeInputLabel = new TextView(this);
            timeInputLabel.setText("自定义时间(时:分:秒):");
            timeInputLabel.setTextSize(16);
            timeInputLabel.setPadding(0, 16, 0, 8);
            
            // 创建水平布局来放置三个输入框
            android.widget.LinearLayout timeInputLayout = new android.widget.LinearLayout(this);
            timeInputLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            
            // 创建时分秒输入框
            hourInput = new android.widget.EditText(this);
            hourInput.setHint("时(0-23)");
            hourInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            hourInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2)});
            hourInput.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            
            minuteInput = new android.widget.EditText(this);
            minuteInput.setHint("分(0-59)");
            minuteInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            minuteInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2)});
            minuteInput.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            
            secondInput = new android.widget.EditText(this);
            secondInput.setHint("秒(0-59)");
            secondInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            secondInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2)});
            secondInput.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            
            // 添加到水平布局
            timeInputLayout.addView(hourInput);
            timeInputLayout.addView(minuteInput);
            timeInputLayout.addView(secondInput);
            
            // 创建自定义时间发送按钮
            customTimeButton = new Button(this);
            customTimeButton.setText("发送自定义时间");
            customTimeButton.setBackgroundColor(android.graphics.Color.parseColor("#9C27B0"));
            customTimeButton.setTextColor(android.graphics.Color.WHITE);
            
            // 创建时间复位按钮
            resetTimeButton = new Button(this);
            resetTimeButton.setText("复位时间(00:00:00)");
            resetTimeButton.setBackgroundColor(android.graphics.Color.parseColor("#03A9F4"));
            resetTimeButton.setTextColor(android.graphics.Color.WHITE);
            
            // 创建温度复位按钮
            resetTempButton = new Button(this);
            resetTempButton.setText("复位温度(0℃)");
            resetTempButton.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"));
            resetTempButton.setTextColor(android.graphics.Color.WHITE);
            
            // 创建自动发送时间按钮
            autoTimeButton = new Button(this);
            autoTimeButton.setText("开始自动发送时间");
            autoTimeButton.setBackgroundColor(android.graphics.Color.parseColor("#E91E63"));
            autoTimeButton.setTextColor(android.graphics.Color.WHITE);
            
            // 将所有新控件添加到布局中
            buttonsLayout.addView(timeButton);
            buttonsLayout.addView(resetTimeButton);
            buttonsLayout.addView(autoTimeButton);
            buttonsLayout.addView(temperatureButton);
            buttonsLayout.addView(resetTempButton);
            buttonsLayout.addView(timeInputLabel);
            buttonsLayout.addView(timeInputLayout);
            buttonsLayout.addView(customTimeButton);
            
            // 为每个按钮单独创建LinearLayout.LayoutParams
            android.widget.LinearLayout.LayoutParams timeParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            timeParams.setMargins(0, 8, 0, 8);
            timeButton.setLayoutParams(timeParams);
            
            android.widget.LinearLayout.LayoutParams tempParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            tempParams.setMargins(0, 8, 0, 8);
            temperatureButton.setLayoutParams(tempParams);
            
            android.widget.LinearLayout.LayoutParams resetTimeParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            resetTimeParams.setMargins(0, 8, 0, 8);
            resetTimeButton.setLayoutParams(resetTimeParams);
            
            android.widget.LinearLayout.LayoutParams resetTempParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            resetTempParams.setMargins(0, 8, 0, 8);
            resetTempButton.setLayoutParams(resetTempParams);
            
            android.widget.LinearLayout.LayoutParams customParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            customParams.setMargins(0, 8, 0, 8);
            customTimeButton.setLayoutParams(customParams);
            
            android.widget.LinearLayout.LayoutParams autoParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            autoParams.setMargins(0, 8, 0, 8);
            autoTimeButton.setLayoutParams(autoParams);
            
            // 获取清空日志按钮
            clearLogButton = findViewById(R.id.clearLogButton);
            
            // 初始状态下禁用测试按钮
            setTestButtonsEnabled(false);
            
            // 添加测试说明到日志区
            addLogMessage("请先连接蓝牙设备，然后点击功能按钮:\n" +
                         "- 获取当前时间: 发送时分秒 (主指令 0x01)\n" +
                         "- 复位时间: 发送00:00:00 (主指令 0x03)\n" +
                         "- 自动发送时间: 每秒发送当前时间 (主指令 0x01)\n" +
                         "- 获取当前温度: 发送温度数据 (主指令 0x02)\n" +
                         "- 复位温度: 发送0℃ (主指令 0x02)\n" +
                         "- 自定义时间: 手动输入时分秒后发送 (主指令 0x03)");
        } catch (Exception e) {
            // 捕获所有可能的异常，防止UI初始化崩溃
            Log.e(TAG, "UI初始化失败: " + e.getMessage());
            e.printStackTrace();
            // 显示错误提示
            Toast.makeText(this, "UI初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 设置按钮点击监听器
     */
    private void setupButtonListeners() {
        try {
            // 检查按钮是否已初始化
            if (connectButton == null || quickConnectButton == null || 
                timeButton == null || temperatureButton == null || 
                customTimeButton == null || resetTimeButton == null || 
                resetTempButton == null || autoTimeButton == null || 
                clearLogButton == null) {
                Log.e(TAG, "部分按钮未初始化，无法设置监听器");
                return;
            }
            
            // 连接按钮
            connectButton.setOnClickListener(v -> {
                try {
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
                } catch (Exception e) {
                    Log.e(TAG, "连接按钮点击处理异常: " + e.getMessage());
                    Toast.makeText(this, "连接操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
            // 快速连接按钮
            quickConnectButton.setOnClickListener(v -> {
                try {
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
                        
                        // 检查蓝牙连接权限
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) 
                                    != PackageManager.PERMISSION_GRANTED) {
                                Toast.makeText(this, "缺少蓝牙连接权限，无法快速连接", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "快速连接时缺少权限");
                                return;
                            }
                        }
                        
                        // 检查蓝牙是否启用
                        if (!bluetoothAdapter.isEnabled()) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH_PERMISSIONS);
                            return;
                        }
                        
                        // 获取设备并连接
                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(lastDeviceAddress);
                        if (device == null) {
                            Toast.makeText(this, "无法找到上次连接的设备", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        selectedDevice = device;
                        
                        Toast.makeText(this, "正在快速连接到上次设备...", Toast.LENGTH_SHORT).show();
                        
                        // 检查获取设备名称权限
                        String deviceName = lastDeviceAddress;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) 
                                    == PackageManager.PERMISSION_GRANTED) {
                                deviceName = device.getName() != null ? device.getName() : lastDeviceAddress;
                            }
                        } else {
                            deviceName = device.getName() != null ? device.getName() : lastDeviceAddress;
                        }
                        addLogMessage("正在快速连接到设备: " + deviceName);
                        
                        // 连接设备
                        if (bluetoothManager != null) {
                            bluetoothManager.connect(device);
                        } else {
                            Log.e(TAG, "蓝牙管理器未初始化");
                            Toast.makeText(this, "蓝牙管理器未初始化，无法连接", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "快速连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "快速连接失败: " + e.getMessage());
                        addLogMessage("快速连接失败: " + e.getMessage());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "快速连接按钮点击处理异常: " + e.getMessage());
                    Toast.makeText(this, "快速连接操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
            // 获取当前时间按钮
            timeButton.setOnClickListener(v -> {
                try {
                    sendCurrentTime();
                } catch (Exception e) {
                    Log.e(TAG, "发送时间异常: " + e.getMessage());
                    Toast.makeText(this, "发送时间失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
            // 获取温度按钮
            temperatureButton.setOnClickListener(v -> {
                try {
                    sendTemperature();
                } catch (Exception e) {
                    Log.e(TAG, "发送温度异常: " + e.getMessage());
                    Toast.makeText(this, "发送温度失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
            // 自定义时间按钮
            customTimeButton.setOnClickListener(v -> {
                try {
                    sendCustomTime();
                } catch (Exception e) {
                    Log.e(TAG, "发送自定义时间异常: " + e.getMessage());
                    Toast.makeText(this, "发送自定义时间失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
            // 时间复位按钮
            resetTimeButton.setOnClickListener(v -> {
                try {
                    resetTime();
                } catch (Exception e) {
                    Log.e(TAG, "时间复位异常: " + e.getMessage());
                    Toast.makeText(this, "时间复位失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
            // 温度复位按钮
            resetTempButton.setOnClickListener(v -> {
                try {
                    resetTemperature();
                } catch (Exception e) {
                    Log.e(TAG, "温度复位异常: " + e.getMessage());
                    Toast.makeText(this, "温度复位失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
            // 自动发送时间按钮
            autoTimeButton.setOnClickListener(v -> {
                try {
                    toggleAutoSendTime();
                } catch (Exception e) {
                    Log.e(TAG, "自动发送时间切换异常: " + e.getMessage());
                    Toast.makeText(this, "自动发送时间设置失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
            // 清空日志按钮
            if (clearLogButton != null) {
                clearLogButton.setOnClickListener(v -> {
                    try {
                        if (receivedDataText != null) {
                            receivedDataText.setText("");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "清空日志异常: " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "设置按钮监听器失败: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "界面初始化失败，部分功能可能无法使用", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 初始化蓝牙数据管理器
     */
    private void initBluetoothManager() {
        try {
            // 获取蓝牙适配器
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 检查并记录蓝牙权限状态
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                boolean hasPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) 
                        == PackageManager.PERMISSION_GRANTED;
                Log.d(TAG, "蓝牙连接权限状态: " + (hasPermission ? "已授权" : "未授权"));
                
                if (!hasPermission) {
                    // 这里我们只记录但不阻止，因为实际连接时会再次检查权限
                    Log.w(TAG, "注意：蓝牙功能可能受限，因为没有BLUETOOTH_CONNECT权限");
                }
            }
            
            // 创建蓝牙数据管理器，传入上下文以便权限检查
            bluetoothManager = new BluetoothDataManager(bluetoothAdapter, this);
            
            // 设置数据接收监听器
            bluetoothManager.setOnDataReceivedListener((command, data) -> {
                try {
                    // 在UI线程处理接收到的数据
                    uiHandler.post(() -> {
                        try {
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
                            
                            if (receivedDataText != null) {
                                // 添加到日志显示区域
                                receivedDataText.append(logMessage);
                            
                                // 滚动到底部
                                scrollToBottom();
                            } else {
                                Log.e(TAG, "receivedDataText为空，无法显示接收数据");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "处理接收数据异常: " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "数据接收监听器异常: " + e.getMessage());
                }
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
                                    if (connectionStatusText != null) {
                                        connectionStatusText.setText("已连接设备，但缺少权限获取设备信息");
                                    }
                                    if (connectButton != null) {
                                        connectButton.setText("断开连接");
                                    }
                                    setTestButtonsEnabled(true);
                                    addLogMessage("已连接到设备，但缺少权限获取设备信息");
                                    return;
                                }
                            }
                            
                            // 有权限，可以正常获取设备名称
                            String deviceName = device.getName();
                            if (connectionStatusText != null) {
                                connectionStatusText.setText("已连接到: " + deviceName);
                            }
                            if (connectButton != null) {
                                connectButton.setText("断开连接");
                            }
                            
                            // 确保测试按钮可见和可点击
                            setTestButtonsEnabled(true);
                            
                            // 强制刷新UI
                            if (timeButton != null) timeButton.invalidate();
                            if (temperatureButton != null) temperatureButton.invalidate();
                            if (customTimeButton != null) customTimeButton.invalidate();
                            if (autoTimeButton != null) autoTimeButton.invalidate();
                            
                            addLogMessage("已连接到设备: " + deviceName);
                            
                            // 确保按钮点击监听器正确设置
                            ensureButtonListenersSetup();
                        } catch (SecurityException e) {
                            // 捕获可能的权限异常
                            Log.e(TAG, "获取设备名称权限被拒绝: " + e.getMessage());
                            if (connectionStatusText != null) {
                                connectionStatusText.setText("已连接设备，但无法获取设备信息");
                            }
                            if (connectButton != null) {
                                connectButton.setText("断开连接");
                            }
                            setTestButtonsEnabled(true);
                            addLogMessage("已连接到设备，但无法获取设备信息");
                        } catch (Exception e) {
                            Log.e(TAG, "处理连接成功回调异常: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onDisconnected() {
                    uiHandler.post(() -> {
                        try {
                            if (connectionStatusText != null) {
                                connectionStatusText.setText("未连接蓝牙设备");
                            }
                            if (connectButton != null) {
                                connectButton.setText("选择并连接蓝牙设备");
                            }
                            setTestButtonsEnabled(false);
                            addLogMessage("蓝牙连接已断开");
                            
                            // 如果正在自动发送时间，停止它
                            if (isAutoSendingTime) {
                                stopAutoSendTime();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "处理断开连接回调异常: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onConnectionFailed(Exception e) {
                    uiHandler.post(() -> {
                        try {
                            if (connectionStatusText != null) {
                                connectionStatusText.setText("连接失败");
                            }
                            addLogMessage("连接失败: " + e.getMessage());
                            Toast.makeText(BluetoothTestActivity.this, 
                                          "连接失败: " + e.getMessage(), 
                                          Toast.LENGTH_SHORT).show();
                        } catch (Exception ex) {
                            Log.e(TAG, "处理连接失败回调异常: " + ex.getMessage());
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "初始化蓝牙管理器异常: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "初始化蓝牙管理器失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
            
            // 检查蓝牙是否启用及权限
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
                
                // 获取设备名称，确保已有权限
                String deviceName;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // 前面已经检查过权限，这里再次确认
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) 
                            == PackageManager.PERMISSION_GRANTED) {
                        deviceName = selectedDevice.getName() != null ? selectedDevice.getName() : selectedDevice.getAddress();
                    } else {
                        deviceName = selectedDevice.getAddress(); // 使用地址作为备选
                    }
                } else {
                    deviceName = selectedDevice.getName() != null ? selectedDevice.getName() : selectedDevice.getAddress();
                }
                
                Toast.makeText(this, "正在连接到 " + deviceName, Toast.LENGTH_SHORT).show();
                addLogMessage("正在连接到设备: " + deviceName);
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
        try {
            if (receivedDataText == null) {
                Log.e(TAG, "receivedDataText为空，无法添加日志: " + message);
                return;
            }
            
            // 如果不是以换行结尾，则添加换行符
            if (!message.endsWith("\n")) {
                message += "\n";
            }
            
            // 添加消息到日志区域
            receivedDataText.append(message);
            
            // 自动滚动到底部
            scrollToBottom();
        } catch (Exception e) {
            Log.e(TAG, "添加日志消息异常: " + e.getMessage());
        }
    }
    
    /**
     * 将日志滚动到底部
     */
    private void scrollToBottom() {
        try {
            if (receivedDataText == null) {
                return;
            }
            
            // 获取父视图（不进行强制类型转换）
            ViewParent parent = receivedDataText.getParent();
            
            // 循环向上查找ScrollView
            while (parent != null && !(parent instanceof ScrollView)) {
                parent = parent.getParent();
            }
            
            // 如果找到了ScrollView，就滚动到底部
            if (parent instanceof ScrollView) {
                final ScrollView scrollView = (ScrollView) parent;
                scrollView.post(() -> {
                    try {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    } catch (Exception e) {
                        Log.e(TAG, "滚动到底部异常: " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "滚动到底部方法异常: " + e.getMessage());
        }
    }
    
    /**
     * 设置测试按钮的启用状态
     * @param enabled 是否启用
     */
    private void setTestButtonsEnabled(boolean enabled) {
        try {
            // 检查按钮是否已初始化
            if (timeButton != null) timeButton.setEnabled(enabled);
            if (temperatureButton != null) temperatureButton.setEnabled(enabled);
            if (customTimeButton != null) customTimeButton.setEnabled(enabled);
            if (resetTimeButton != null) resetTimeButton.setEnabled(enabled);
            if (resetTempButton != null) resetTempButton.setEnabled(enabled);
            if (autoTimeButton != null) autoTimeButton.setEnabled(enabled);
            
            // 如果按钮被禁用，同时停止自动发送时间（如果正在进行）
            if (!enabled && isAutoSendingTime) {
                stopAutoSendTime();
            }
            
            // 更新按钮文本颜色
            int textColor = enabled ? android.graphics.Color.WHITE : android.graphics.Color.LTGRAY;
            
            if (timeButton != null) timeButton.setTextColor(textColor);
            if (temperatureButton != null) temperatureButton.setTextColor(textColor);
            if (customTimeButton != null) customTimeButton.setTextColor(textColor);
            if (resetTimeButton != null) resetTimeButton.setTextColor(textColor);
            if (resetTempButton != null) resetTempButton.setTextColor(textColor);
            if (autoTimeButton != null) autoTimeButton.setTextColor(textColor);
            
            // 如果正在进行自动发送时间，更新按钮文本
            if (autoTimeButton != null) {
                if (isAutoSendingTime) {
                    autoTimeButton.setText("停止自动发送时间");
                } else {
                    autoTimeButton.setText("开始自动发送时间");
                }
            }
            
            // 更新输入框状态
            boolean inputEnabled = enabled && !isAutoSendingTime;
            if (hourInput != null) hourInput.setEnabled(inputEnabled);
            if (minuteInput != null) minuteInput.setEnabled(inputEnabled);
            if (secondInput != null) secondInput.setEnabled(inputEnabled);
        } catch (Exception e) {
            Log.e(TAG, "设置测试按钮状态异常: " + e.getMessage());
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
        // 确保停止自动发送
        if (isAutoSendingTime) {
            stopAutoSendTime();
        }
        
        try {
            // 断开蓝牙连接并释放资源
            if (bluetoothManager != null) {
                bluetoothManager.disconnect();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "断开蓝牙连接时权限被拒绝: " + e.getMessage());
        }
        
        super.onDestroy();
    }

    /**
     * 确保按钮监听器被正确设置
     */
    private void ensureButtonListenersSetup() {
        // 获取当前时间按钮
        timeButton.setOnClickListener(v -> {
            sendCurrentTime();
        });
        
        // 获取温度按钮
        temperatureButton.setOnClickListener(v -> {
            sendTemperature();
        });
        
        // 自定义时间按钮
        customTimeButton.setOnClickListener(v -> {
            sendCustomTime();
        });
        
        // 时间复位按钮
        resetTimeButton.setOnClickListener(v -> {
            resetTime();
        });
        
        // 温度复位按钮
        resetTempButton.setOnClickListener(v -> {
            resetTemperature();
        });
        
        // 自动发送时间按钮
        autoTimeButton.setOnClickListener(v -> {
            toggleAutoSendTime();
        });
    }
    
    /**
     * 发送当前时间
     * 主指令: 0x01
     * 数据位: 时, 分, 秒
     */
    private void sendCurrentTime() {
        // 获取当前时间
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = calendar.get(java.util.Calendar.MINUTE);
        int second = calendar.get(java.util.Calendar.SECOND);
        
        // 构建数据
        byte command = 0x01;  // 时间命令
        byte[] data = {(byte)hour, (byte)minute, (byte)second};
        
        // 发送数据
        sendTestData(command, data, "发送当前时间");
        
        // 显示发送的时间
        addLogMessage(String.format("当前时间: %02d:%02d:%02d", hour, minute, second));
    }
    
    /**
     * 发送温度数据
     * 主指令: 0x02
     * 数据位1: 小数点前两位
     * 数据位2: 小数点后两位
     * 数据位3: 温度正(0xFF)负或零(0x00)
     */
    private void sendTemperature() {
        // 模拟获取温度(实际项目中应从传感器获取)
        java.util.Random random = new java.util.Random();
        double temp = 15 + random.nextDouble() * 25; // 模拟15-40度之间的温度
        
        // 随机生成负温度(20%概率)
        if (random.nextInt(10) < 2) {
            temp = -temp;
        }
        
        // 解析温度
        int tempInt = (int)Math.abs(temp);  // 整数部分
        int tempDec = (int)((Math.abs(temp) - tempInt) * 100);  // 小数部分(两位)
        byte tempSign = (temp > 0) ? (byte)0xFF : (byte)0x00;  // 符号(正:FF, 负或零:00)
        
        // 构建数据
        byte command = 0x02;  // 温度命令
        byte[] data = {(byte)tempInt, (byte)tempDec, tempSign};
        
        // 发送数据
        sendTestData(command, data, "发送温度数据");
        
        // 显示发送的温度
        addLogMessage(String.format("当前温度: %.2f°C (%s)", 
                                   temp, 
                                   (temp > 0) ? "正" : "负或零"));
    }
    
    /**
     * 发送自定义时间
     * 主指令: 0x03
     * 数据位: 时, 分, 秒
     */
    private void sendCustomTime() {
        // 获取输入框的值
        String hourStr = hourInput.getText().toString().trim();
        String minuteStr = minuteInput.getText().toString().trim();
        String secondStr = secondInput.getText().toString().trim();
        
        // 验证输入
        if (hourStr.isEmpty() || minuteStr.isEmpty() || secondStr.isEmpty()) {
            Toast.makeText(this, "请输入完整的时分秒", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int hour = Integer.parseInt(hourStr);
            int minute = Integer.parseInt(minuteStr);
            int second = Integer.parseInt(secondStr);
            
            // 验证范围
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59) {
                Toast.makeText(this, "时间输入超出范围(时:0-23,分:0-59,秒:0-59)", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 构建数据
            byte command = 0x03;  // 自定义时间命令
            byte[] data = {(byte)hour, (byte)minute, (byte)second};
            
            // 发送数据
            sendTestData(command, data, "发送自定义时间");
            
            // 显示发送的时间
            addLogMessage(String.format("自定义时间: %02d:%02d:%02d", hour, minute, second));
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 复位时间为00:00:00
     * 主指令: 0x03
     * 数据位: 全部为0
     */
    private void resetTime() {
        // 构建数据
        byte command = 0x03;  // 自定义时间命令
        byte[] data = {0x00, 0x00, 0x00};  // 时分秒全为0
        
        // 发送数据
        sendTestData(command, data, "复位时间");
        
        // 显示发送的时间
        addLogMessage("复位时间: 00:00:00");
    }
    
    /**
     * 复位温度为0℃
     * 主指令: 0x02
     * 数据位: 全部为0
     */
    private void resetTemperature() {
        // 构建数据
        byte command = 0x02;  // 温度命令
        byte[] data = {0x00, 0x00, 0x00};  // 温度为0℃
        
        // 发送数据
        sendTestData(command, data, "复位温度");
        
        // 显示发送的温度
        addLogMessage("复位温度: 0.00℃ (零或负)");
    }
    
    /**
     * 切换自动发送时间状态
     */
    private void toggleAutoSendTime() {
        if (isAutoSendingTime) {
            // 如果正在自动发送，则停止
            stopAutoSendTime();
        } else {
            // 如果未自动发送，则开始
            startAutoSendTime();
        }
    }
    
    /**
     * 开始自动发送时间
     */
    private void startAutoSendTime() {
        // 如果未连接蓝牙，提示并返回
        if (bluetoothManager == null || !bluetoothManager.isConnected()) {
            Toast.makeText(this, "请先连接蓝牙设备", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 更新按钮文字和状态
        autoTimeButton.setText("停止自动发送时间");
        autoTimeButton.setBackgroundColor(android.graphics.Color.parseColor("#F44336"));
        isAutoSendingTime = true;
        
        // 添加日志
        addLogMessage("开始自动发送时间（每秒一次）");
        
        // 创建定时任务
        autoSendTimer = new java.util.Timer();
        autoSendTask = new java.util.TimerTask() {
            @Override
            public void run() {
                // 在UI线程上执行发送时间操作
                uiHandler.post(() -> {
                    // 只有在连接状态下才发送
                    if (bluetoothManager != null && bluetoothManager.isConnected()) {
                        sendCurrentTime();
                    } else {
                        // 如果连接断开，停止自动发送
                        stopAutoSendTime();
                        uiHandler.post(() -> {
                            Toast.makeText(BluetoothTestActivity.this, 
                                          "蓝牙连接已断开，停止自动发送时间", 
                                          Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        };
        
        // 立即执行一次，然后每秒执行一次
        autoSendTimer.schedule(autoSendTask, 0, 1000);
    }
    
    /**
     * 停止自动发送时间
     */
    private void stopAutoSendTime() {
        // 如果定时器和任务存在，取消它们
        if (autoSendTimer != null) {
            autoSendTimer.cancel();
            autoSendTimer = null;
        }
        
        if (autoSendTask != null) {
            autoSendTask.cancel();
            autoSendTask = null;
        }
        
        // 更新按钮文字和状态
        autoTimeButton.setText("开始自动发送时间");
        autoTimeButton.setBackgroundColor(android.graphics.Color.parseColor("#E91E63"));
        isAutoSendingTime = false;
        
        // 添加日志
        addLogMessage("停止自动发送时间");
    }
    
    /**
     * 处理蓝牙断开连接的情况
     */
    @Override
    protected void onPause() {
        super.onPause();
        
        // 如果活动暂停且正在自动发送，则停止自动发送
        if (isAutoSendingTime) {
            stopAutoSendTime();
        }
    }
} 