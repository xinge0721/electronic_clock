#include "stm32f10x.h"
#include "cuerrenttime.h"
#include "Serial.h"  // 串口相关函数头文件

/**
 * @brief 时间结构体和温度变量的初始化
 * currentTime - 默认时间12:00:00，且默认从秒开始修改(settingIndex=2)
 * temperature - 温度值，初始为0
 */
TimeStruct currentTime = {12, 0, 0, 0, 2};  // 默认时间12:00:00，默认从秒开始修改
float temperature = 0.0f;  // 温度值

// 修改初始化函数，移除重复代码，并添加负号位置
void cuerrenttime_Init(void)
{
    // 时间显示固定字符
    OLED_ShowChar(1, 8, ':');
    OLED_ShowChar(1, 11, ':');
    
    // 编辑模式显示
    OLED_ShowString(3, 6, "EDIT:  ");
    
    // 温度显示固定字符
    OLED_ShowChar(2, 8, '.');  // 小数点
    OLED_ShowChar(2, 10, 'C'); // 温度单位
    
    // 初始化时默认显示的编辑项
    switch(currentTime.settingIndex)
    {
        case 0: OLED_ShowString(3, 12, "HOUR"); break;
        case 1: OLED_ShowString(3, 12, "MIN "); break;
        case 2: OLED_ShowString(3, 12, "SEC "); break;
    }
}

// 修改温度显示部分，处理负数情况
void UpdateTimeDisplay(void)
{
    // 显示时间数字部分（冒号已在初始化时显示）
    OLED_ShowNum(1, 6, currentTime.hours, 2);
    OLED_ShowNum(1, 9, currentTime.minutes, 2);
    OLED_ShowNum(1, 12, currentTime.seconds, 2);
    
    // 显示温度（小数点和单位已在初始化时显示）
    if (temperature != 0)
    {
        uint16_t temp_abs = (uint16_t)(temperature < 0 ? -temperature * 10 : temperature * 10);
        
        // 处理负数符号
        if (temperature < 0)
            OLED_ShowChar(2, 5, '-');
        else
            OLED_ShowChar(2, 5, ' '); // 清除可能存在的负号
        
        // 显示温度整数部分和小数部分
        OLED_ShowNum(2, 6, temp_abs / 10, 2);
        OLED_ShowNum(2, 9, temp_abs % 10, 1);
    }
    else 
    {
        OLED_ShowChar(2, 5, ' '); // 确保无负号
        OLED_ShowNum(2, 6, 0, 2);
        OLED_ShowNum(2, 9, 0, 1);
    }
}

/**
 * @brief 处理时间设置函数
 * 功能：根据当前选择的时间项(settingIndex)和递增/递减标志，修改对应的时间值
 * 参数：
 *   increment: 1表示增加时间，0表示减少时间
 * 注意：
 *   - 时(hours)范围: 0-23
 *   - 分/秒(minutes/seconds)范围: 0-59
 */
void HandleTimeSetting(uint8_t increment)
{
	switch(currentTime.settingIndex)
	{
		case 0: // 设置小时
			if (increment)
			{
				currentTime.hours = (currentTime.hours + 1) % 24;
			}
			else
			{
				currentTime.hours = (currentTime.hours + 23) % 24;
			}
			break;
			
		case 1: // 设置分钟
			if (increment)
			{
				currentTime.minutes = (currentTime.minutes + 1) % 60;
			}
			else
			{
				currentTime.minutes = (currentTime.minutes + 59) % 60;
			}
			break;
			
		case 2: // 设置秒
			if (increment)
			{
				currentTime.seconds = (currentTime.seconds + 1) % 60;
			}
			else
			{
				currentTime.seconds = (currentTime.seconds + 59) % 60;
			}
			break;
	}
}

/**
 * @brief 按键功能执行函数（非循环版）
 * 说明：
 * - 这个函数需要在main循环中反复调用
 * - KeyCfg[0-3]对应四个按键，对应GPIOA的8-11引脚
 * - 每个按键都有三种事件：单击(SingleClick)，双击(DoubleClick)，长按(LongPress)
 * - 处理完事件后，必须将事件置为KEY_Event_Null(除非是需要持续触发的长按)
 * 
 * 用法举例:
 * if (KeyCfg[0].KEY_Event == KEY_Event_SingleClick) {
 *     // 处理按键0的单击事件
 *     KeyCfg[0].KEY_Event = KEY_Event_Null; // 处理完后清除事件标志
 * }
 */
void Key_Nums(void) 
{
	static uint8_t cont = 2;
	// 按键0 (GPIOA.8)
	if (KeyCfg[0].KEY_Event == KEY_Event_SingleClick) //单击
	{
		
		KeyCfg[0].KEY_Event = KEY_Event_Null;
	} 
	else if (KeyCfg[0].KEY_Event == KEY_Event_DoubleClick) //双击
	{
		KeyCfg[0].KEY_Event = KEY_Event_Null;
	} 
	else if (KeyCfg[0].KEY_Event == KEY_Event_LongPress)  //长按
	{
		KeyCfg[0].KEY_Event = KEY_Event_Null;
	}
	
	// 按键1 (GPIOA.9)
	if (KeyCfg[1].KEY_Event == KEY_Event_SingleClick) //单击
	{
		HandleTimeSetting(1);
		KeyCfg[1].KEY_Event = KEY_Event_Null;
	} 
	else if (KeyCfg[1].KEY_Event == KEY_Event_DoubleClick) //双击
	{
		KeyCfg[1].KEY_Event = KEY_Event_Null;
	} 
	else if (KeyCfg[1].KEY_Event == KEY_Event_LongPress)  //长按
	{
		Delay_ms(50);
		HandleTimeSetting(1);
	}
	
	// 按键2 (GPIOA.10)
	if (KeyCfg[2].KEY_Event == KEY_Event_SingleClick) //单击
	{
		HandleTimeSetting(0);
		KeyCfg[2].KEY_Event = KEY_Event_Null;
	} 
	else if (KeyCfg[2].KEY_Event == KEY_Event_DoubleClick) //双击
	{
		KeyCfg[2].KEY_Event = KEY_Event_Null;
	} 
	else if (KeyCfg[2].KEY_Event == KEY_Event_LongPress)  //长按
	{
				Delay_ms(50);

		HandleTimeSetting(0);
	}
	
	// 按键3 (GPIOA.11)
	if (KeyCfg[3].KEY_Event == KEY_Event_SingleClick) //单击
	{
		// 切换修改项：秒->分->时->秒
		cont = cont == 0 ? 2 : cont - 1;
    currentTime.settingIndex = cont;
    
    // 只更新编辑项显示，其他固定显示在初始化时已设置
    switch(currentTime.settingIndex)
    {
        case 0: OLED_ShowString(3, 12, "HOUR"); break;
        case 1: OLED_ShowString(3, 12, "MIN "); break;
        case 2: OLED_ShowString(3, 12, "SEC "); break;
    }
    
		
		KeyCfg[3].KEY_Event = KEY_Event_Null;
	} 
	else if (KeyCfg[3].KEY_Event == KEY_Event_DoubleClick) //双击
	{
		KeyCfg[3].KEY_Event = KEY_Event_Null;
	} 
	else if (KeyCfg[3].KEY_Event == KEY_Event_LongPress)  //长按
	{
		KeyCfg[3].KEY_Event = KEY_Event_Null;
	}
}

