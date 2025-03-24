#include "stm32f10x.h"                  // Device header
#include "Delay.h"
#include "OLED.h"
#include "Serial.h"
#include "Key.h"
#include "Timer.h"
#include "cuerrenttime.h"


void Hardware_Init(void)
{
	OLED_Init();		//OLED初始化
	KEY_Init();
	TIM2_Int_Init(200, 7200);  // 72MHz / 7200 = 10kHz, 200次计数 = 20ms中断
	TIM3_Int_Init(2000, 36000);  // 72MHz / 36000 = 2kHz, 2000次计数 = 1s中断，用于时钟计时
	Serial_Init();  
	cuerrenttime_Init();
	OLED_ShowString(1, 1, "Time:");
	OLED_ShowString(2, 1, "Temp:");
	OLED_ShowString(3, 1, "Mode:");
}

/**
 * @brief 在OLED上直接显示rxBuffer的8个字节（十六进制）
 */
void OLED_ShowRxBuffer(void)
{
    // 第一行显示前4个字节
    OLED_ShowHexNum(4, 1, rxBuffer[0], 2);
    OLED_ShowHexNum(4, 4, rxBuffer[1], 2);
    OLED_ShowHexNum(4, 7, rxBuffer[2], 2);
    OLED_ShowHexNum(4, 10, rxBuffer[3], 2);
    
    // 第二行显示后4个字节
    OLED_ShowHexNum(5, 1, rxBuffer[4], 2);
    OLED_ShowHexNum(5, 4, rxBuffer[5], 2);
    OLED_ShowHexNum(5, 7, rxBuffer[6], 2);
    OLED_ShowHexNum(5, 10, rxBuffer[7], 2);
}
int main(void)
{
	/*模块初始化*/
	Hardware_Init();
	currentTime.settingIndex = 2;
	while (1)
	{
		UpdateTimeDisplay();
//		OLED_ShowRxBuffer();
		USART_data();  // 处理串口数据
		Key_Nums();
	}
}

void TIM2_IRQHandler(void)
{
	if (TIM_GetITStatus(TIM2, TIM_IT_Update) == SET)
	{
		// 处理按键
		for (u8 i = 0; i < 4; i++) {
			KEY_ReadStateMachine(i);
		}
		
		TIM_ClearITPendingBit(TIM2, TIM_IT_Update);
	}
}

void TIM3_IRQHandler(void)
{
	if (TIM_GetITStatus(TIM3, TIM_IT_Update) == SET)
	{
		// 更新时间计数 - 不需要检查设置模式标志
		// 更新时间
		currentTime.seconds++;
		if (currentTime.seconds >= 60)
		{
			currentTime.seconds = 0;
			currentTime.minutes++;
			if (currentTime.minutes >= 60)
			{
				currentTime.minutes = 0;
				currentTime.hours = (currentTime.hours + 1) % 24;
			}
		}
		
//		UpdateTimeDisplay(); //更新时间
		TIM_ClearITPendingBit(TIM3, TIM_IT_Update);
	}
}
