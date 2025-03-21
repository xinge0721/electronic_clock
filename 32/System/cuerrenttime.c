#include "stm32f10x.h"
#include "cuerrenttime.h"

TimeStruct currentTime = {12, 0, 0, 0, 0};  // 默认时间12:00:00
float temperature = 0.0f;  // 温度值

// 更新时间显示
void UpdateTimeDisplay(void)
{
	// 显示时间，格式：HH:MM:SS
	OLED_ShowNum(1, 6, currentTime.hours, 2);
	OLED_ShowChar(1, 8, ':');
	OLED_ShowNum(1, 9, currentTime.minutes, 2);
	OLED_ShowChar(1, 11, ':');
	OLED_ShowNum(1, 12, currentTime.seconds, 2);
	
	// 如果在设置模式，显示光标
	if (currentTime.isSettingMode)
	{
		OLED_ShowString(3, 6, "SET   ");
		// 根据settingIndex显示当前正在设置的项
		switch(currentTime.settingIndex)
		{
			case 0: OLED_ShowString(3, 10, "HOUR"); break;
			case 1: OLED_ShowString(3, 10, "MIN "); break;
			case 2: OLED_ShowString(3, 10, "SEC "); break;
		}
	}
	else
	{
		OLED_ShowString(3, 6, "NORMAL");
	}
	
	// 显示温度
	if (temperature != 0)
	{
		// 显示温度，保留一位小数
		uint16_t temp_int = (uint16_t)(temperature * 10);
		OLED_ShowNum(2, 6, temp_int / 10, 2);
		OLED_ShowChar(2, 8, '.');
		OLED_ShowNum(2, 9, temp_int % 10, 1);
		OLED_ShowChar(2, 10, 'C');
	}
}

// 处理时间设置
void HandleTimeSetting(uint8_t increment)
{
	if (!currentTime.isSettingMode) return;
	
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

// 按键功能执行函数（非循环版）
void Key_Nums(void) 
{
	// 处理按键0（GPIOA.8）
	if (KeyCfg[0].KEY_Event == KEY_Event_SingleClick) 
	{
		// 进入/退出设置模式
		currentTime.isSettingMode = !currentTime.isSettingMode;
		currentTime.settingIndex = 0;
		KeyCfg[0].KEY_Event = KEY_Event_Null;
	} 
	else if (KeyCfg[0].KEY_Event == KEY_Event_DoubleClick) 
	{
		// 预留双击功能
		KeyCfg[0].KEY_Event = KEY_Event_Null;
	} 
	else if (KeyCfg[0].KEY_Event == KEY_Event_LongPress) 
	{
		// 预留长按功能
		KeyCfg[0].KEY_Event = KEY_Event_Null;
	}

	// 处理按键1（GPIOA.9）
	if (KeyCfg[1].KEY_Event == KEY_Event_SingleClick) 
	{
		// 在设置模式下增加当前值
		if (currentTime.isSettingMode)
		{
			HandleTimeSetting(1);
		}
		KeyCfg[1].KEY_Event = KEY_Event_Null;
	} 
	else if (KeyCfg[1].KEY_Event == KEY_Event_DoubleClick) 
	{
		// 在设置模式下切换设置项
		if (currentTime.isSettingMode)
		{
			currentTime.settingIndex = (currentTime.settingIndex + 1) % 3;
		}
		KeyCfg[1].KEY_Event = KEY_Event_Null;
	} 
	else if (KeyCfg[1].KEY_Event == KEY_Event_LongPress) 
	{
		// 在设置模式下减少当前值
		if (currentTime.isSettingMode)
		{
			HandleTimeSetting(0);
		}
		KeyCfg[1].KEY_Event = KEY_Event_Null;
	}

	// 处理按键2（GPIOA.10）
	if (KeyCfg[2].KEY_Event == KEY_Event_SingleClick) 
	{
		// 复位时间
		currentTime.hours = 12;
		currentTime.minutes = 0;
		currentTime.seconds = 0;
		currentTime.isSettingMode = 0;
		KeyCfg[2].KEY_Event = KEY_Event_Null;
	} 
	else if (KeyCfg[2].KEY_Event == KEY_Event_DoubleClick) 
	{
		// 预留双击功能
		KeyCfg[2].KEY_Event = KEY_Event_Null;
	} 
	else if (KeyCfg[2].KEY_Event == KEY_Event_LongPress) 
	{
		// 预留长按功能
		KeyCfg[2].KEY_Event = KEY_Event_Null;
	}

	// 处理按键3（GPIOA.11）
	if (KeyCfg[3].KEY_Event == KEY_Event_SingleClick) 
	{
		// 请求温度数据
//		Serial_SendPacket(0x02, 0x00, 0x00, 0x00);
		KeyCfg[3].KEY_Event = KEY_Event_Null;
	} 
	else if (KeyCfg[3].KEY_Event == KEY_Event_DoubleClick) 
	{
		// 预留双击功能
		KeyCfg[3].KEY_Event = KEY_Event_Null;
	} 
	else if (KeyCfg[3].KEY_Event == KEY_Event_LongPress) 
	{
		// 预留长按功能
		KeyCfg[3].KEY_Event = KEY_Event_Null;
	}
}

