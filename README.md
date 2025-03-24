# 电子时钟蓝牙通信系统

## 项目概述
本项目包含Android端蓝牙通信模块和STM32端电子时钟硬件系统，通过蓝牙SPP协议实现时间与温度的远程控制。

---

## Android端蓝牙通信模块

### 项目概述
基于Android的蓝牙通信模块，通过蓝牙连接STM32电子时钟设备，实现时间/温度控制、数据日志记录等功能。

### 功能特点
- **设备管理**：快速连接/配对设备，支持上次连接记忆
- **时间控制**：发送系统时间/自定义时间，支持时间复位
- **温度控制**：发送温度值（含符号），支持温度复位
- **数据监控**：实时显示发送/接收的8字节数据包

### 数据通信协议
```text
55 | 01 | XX | YY YY YY | CC | AA
数据包结构说明：
- 55：固定帧头
- 01：协议版本
- XX：主指令（0x01时间/0x02温度）
- YY YY YY：3字节数据
- CC：校验和（(XX+YY[0]+YY[1]+YY[2])%256）
- AA：固定帧尾
主要指令
指令码	功能	数据格式示例
0x01	时间设置	[时, 分, 秒]<br>例：12:34:56 → 0x0C 0x22 0x38
0x02	温度设置	[整数, 小数(×100), 符号]<br>例：25.75℃ → 0x19 0x4B 0xFF
0x00	复位	[0x00, 0x00, 0x00]（时间/温度通用）
关键类说明
Java
深色版本
// 蓝牙数据管理类
public class BluetoothDataManager {
    public void connect(BluetoothDevice device); // 连接设备
    public void disconnect(); // 断开连接
    public boolean isConnected(); // 连接状态检测
    public void sendData(byte command, byte[] data); // 发送数据包
}

// 主界面类
public class BluetoothTestActivity {
    public void sendCurrentTime(); // 发送当前系统时间
    public void sendTemperature(); // 发送温度数据
    public void resetTime(); // 时间复位
    // ...其他控制方法
}
界面设计
按钮功能	颜色代码	功能说明
获取当前时间	#4CAF50	发送系统时间
设置自定义时间	#9C27B0	用户输入时间
发送温度	#FF5722	发送当前温度
复位时间	#03A9F4	重置为00:00:00
复位温度	#FF9800	重置为0℃
使用说明
打开应用 → 点击"选择设备"按钮
从配对列表选择STM32设备
连接成功后启用控制按钮
点击对应功能按钮发送指令
实时日志窗口显示通信数据
权限要求
Xml
深色版本
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android:name="android.permission.BLUETOOTH_SCAN" />
<!-- 低版本需添加 -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
STM32端电子时钟系统
硬件组成
组件	型号/规格
主控芯片	STM32F10x系列
显示屏	OLED模块（I2C接口）
按键	4个GPIO按键（PA.8-11）
通信接口	USART2（115200波特率）
核心功能
时间显示：24小时制，精确到秒
温度显示：支持正/负温度，小数精度
按键控制：时间设置/增减/模式切换
串口通信：接收Android指令并更新状态
按键操作逻辑
按键编号	操作模式	功能说明
按键0	-	预留功能
按键1	单击/长按/双击	时间项递增/持续递增/预留功能
按键2	单击/长按/双击	时间项递减/持续递减/预留功能
按键3	单击/长按/双击	时间项切换/预留功能
通信协议
Text
深色版本
55 | 01 | XX | YY YY YY | CC | 55
STM32数据包说明：
- 帧头/帧尾：0x55（与Android端不同，需注意！）
- 命令码：
  - 0x01：时间校准（时/分/秒）
  - 0x02：温度校准（整数/小数/符号）
- 示例数据包：
  设置时间12:30:45 → `0x55 0x01 0x01 0x12 0x30 0x45 0x88 0x55`
  设置温度-10.8℃ → `0x55 0x01 0x02 0x10 0x80 0x00 0x93 0x55`
时间更新机制
自动计时：通过TIM3定时器每秒更新
按键调整：支持手动增减时间项
串口同步：接收Android指令立即更新
扩展接口
闹钟功能预留
日期显示扩展
传感器数据接口
新指令支持扩展（如湿度/气压）
开发注意事项
蓝牙配对：首次使用需手动配对STM32设备
权限问题：部分安卓设备需开启位置权限
通信兼容性：STM32使用0x55作为帧头/尾，与Android端AA不同（需注意协议差异）
数据校验：确保校验和计算正确（(指令+数据)%256）
深色版本

### 说明：
1. 修正了STM32数据包中的帧尾问题（原文档矛盾处）
2. 使用代码块展示关键数据结构和代码片段
3. 添加了注意事项中的协议差异说明
4. 采用分章节结构，便于移动端和硬件端的对比阅读
5. 使用表格增强信息对比效果
6. 保留了原始功能描述的核心内容，进行了结构化整理

建议将此内容分为两个独立文件：
- `android.md`：保存Android端文档
- `stm32.md`：保存STM32端文档
- 在根目录添加`README.md`进行项目总览说明

需要进一步调整请随时告知！
