package com.example.anzuo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import android.Manifest;

/**
 * 蓝牙数据管理示例类
 * 展示如何使用BluetoothDataManager类进行蓝牙数据通信
 */
public class BluetoothDataExample {
    // 日志标签
    private static final String TAG = "user";
    
    // 上下文引用
    private Context context;
    
    // 蓝牙数据管理器
    private BluetoothDataManager bluetoothManager;
    
    /**
     * 构造函数
     * @param context 应用上下文
     */
    public BluetoothDataExample(Context context) {
        this.context = context;
        
        // 获取蓝牙适配器
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        if (bluetoothAdapter == null) {
            Log.e(TAG, "设备不支持蓝牙");
            return;
        }
        
        // 初始化蓝牙数据管理器，传入上下文以便进行权限检查
        bluetoothManager = new BluetoothDataManager(bluetoothAdapter, context);
        
        // 设置数据接收监听器
        bluetoothManager.setOnDataReceivedListener(new BluetoothDataManager.OnDataReceivedListener() {
            @Override
            public void onDataReceived(byte command, byte[] data) {
                // 处理接收到的数据
                handleReceivedData(command, data);
            }
        });
        
        // 设置连接状态变化监听器
        bluetoothManager.setOnConnectionStateChangeListener(new BluetoothDataManager.OnConnectionStateChangeListener() {
            @Override
            public void onConnected(BluetoothDevice device) {
                try {
                    // 连接成功
                    Log.d(TAG, "已连接到设备: " + device.getName());
                    Toast.makeText(context, "已连接到设备: " + device.getName(), Toast.LENGTH_SHORT).show();
                    
                    // 连接成功后发送测试数据
                    sendTestData();
                } catch (SecurityException e) {
                    Log.e(TAG, "获取设备名称权限被拒绝: " + e.getMessage());
                    Toast.makeText(context, "已连接到设备，但无法获取设备名称", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onDisconnected() {
                // 连接断开
                Log.d(TAG, "蓝牙连接已断开");
                Toast.makeText(context, "蓝牙连接已断开", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onConnectionFailed(Exception e) {
                // 连接失败
                Log.e(TAG, "蓝牙连接失败: " + e.getMessage());
                Toast.makeText(context, "蓝牙连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 检查是否有蓝牙连接权限
     * @return 是否有权限
     */
    private boolean hasBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
                   == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) 
                   == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    /**
     * 连接到指定的蓝牙设备
     * @param device 要连接的蓝牙设备
     */
    public void connectToDevice(BluetoothDevice device) {
        if (bluetoothManager != null) {
            try {
                // 检查权限
                if (!hasBluetoothConnectPermission()) {
                    Toast.makeText(context, "缺少蓝牙连接权限", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "连接设备时缺少蓝牙权限");
                    return;
                }
                
                Log.d(TAG, "正在连接到设备: " + device.getName());
                bluetoothManager.connect(device);
            } catch (SecurityException e) {
                Log.e(TAG, "连接设备权限被拒绝: " + e.getMessage());
                Toast.makeText(context, "缺少蓝牙连接权限", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * 断开蓝牙连接
     */
    public void disconnect() {
        if (bluetoothManager != null) {
            bluetoothManager.disconnect();
        }
    }
    
    /**
     * 发送测试数据
     */
    private void sendTestData() {
        if (bluetoothManager != null && bluetoothManager.isConnected()) {
            try {
                // 检查权限
                if (!hasBluetoothConnectPermission()) {
                    Log.e(TAG, "发送数据缺少蓝牙权限");
                    return;
                }
                
                // 主指令
                byte command = 0x02;
                
                // 3个数据位
                byte[] data = new byte[3];
                data[0] = 0x55;  // 注意：数据位可以包含与数据头相同的值
                data[1] = 0x55;
                data[2] = (byte)0xFF;
                
                // 发送数据
                boolean success = bluetoothManager.sendData(command, data);
                
                if (success) {
                    Log.d(TAG, "测试数据发送成功");
                } else {
                    Log.e(TAG, "测试数据发送失败");
                }
            } catch (SecurityException e) {
                Log.e(TAG, "发送数据权限被拒绝: " + e.getMessage());
            }
        }
    }
    
    /**
     * 手动发送数据
     * @param command 主指令
     * @param data1 数据位1
     * @param data2 数据位2
     * @param data3 数据位3
     * @return 是否发送成功
     */
    public boolean sendCustomData(byte command, byte data1, byte data2, byte data3) {
        if (bluetoothManager != null && bluetoothManager.isConnected()) {
            try {
                // 检查权限
                if (!hasBluetoothConnectPermission()) {
                    Log.e(TAG, "发送自定义数据缺少蓝牙权限");
                    return false;
                }
                
                byte[] data = new byte[3];
                data[0] = data1;
                data[1] = data2;
                data[2] = data3;
                
                return bluetoothManager.sendData(command, data);
            } catch (SecurityException e) {
                Log.e(TAG, "发送自定义数据权限被拒绝: " + e.getMessage());
                return false;
            }
        }
        return false;
    }
    
    /**
     * 处理接收到的数据
     * @param command 主指令
     * @param data 数据位
     */
    private void handleReceivedData(byte command, byte[] data) {
        // 示例：根据主指令处理不同类型的数据
        switch (command) {
            case 0x01:
                // 处理状态数据
                processStatusData(data);
                break;
                
            case 0x02:
                // 处理配置数据
                processConfigData(data);
                break;
                
            case 0x03:
                // 处理传感器数据
                processSensorData(data);
                break;
                
            default:
                // 未知指令
                Log.w(TAG, "收到未知指令: " + String.format("%02X", command));
                break;
        }
    }
    
    /**
     * 处理状态数据
     */
    private void processStatusData(byte[] data) {
        // 示例：解析状态数据
        int status = data[0] & 0xFF;
        int battery = data[1] & 0xFF;
        int signal = data[2] & 0xFF;
        
        Log.d(TAG, "状态数据 - 状态: " + status + 
              ", 电池电量: " + battery + "%, 信号强度: " + signal);
        
        // 可以根据状态做相应处理
        if (battery < 20) {
            Log.w(TAG, "设备电量低");
        }
    }
    
    /**
     * 处理配置数据
     */
    private void processConfigData(byte[] data) {
        // 示例：解析配置数据
        int mode = data[0] & 0xFF;
        int interval = data[1] & 0xFF;
        int option = data[2] & 0xFF;
        
        Log.d(TAG, "配置数据 - 模式: " + mode + 
              ", 间隔: " + interval + ", 选项: " + option);
    }
    
    /**
     * 处理传感器数据
     */
    private void processSensorData(byte[] data) {
        // 示例：解析传感器数据
        int sensor1 = data[0] & 0xFF;
        int sensor2 = data[1] & 0xFF;
        int sensor3 = data[2] & 0xFF;
        
        Log.d(TAG, "传感器数据 - 传感器1: " + sensor1 + 
              ", 传感器2: " + sensor2 + ", 传感器3: " + sensor3);
    }
    
    /**
     * 安全地检查是否已连接
     * @return 连接状态
     */
    public boolean isConnected() {
        try {
            return bluetoothManager != null && bluetoothManager.isConnected();
        } catch (SecurityException e) {
            Log.e(TAG, "检查蓝牙连接状态权限被拒绝: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 安全地获取连接的设备
     * @return 蓝牙设备，如果未连接则返回null
     */
    public BluetoothDevice getConnectedDevice() {
        try {
            // 检查权限
            if (!hasBluetoothConnectPermission()) {
                Log.e(TAG, "获取连接设备需要权限");
                return null;
            }
            
            if (bluetoothManager != null) {
                return bluetoothManager.getConnectedDevice();
            }
            return null;
        } catch (SecurityException e) {
            Log.e(TAG, "获取连接设备权限被拒绝: " + e.getMessage());
            return null;
        }
    }
} 