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
	
	OLED_ShowString(1, 1, "Time:");
	OLED_ShowString(2, 1, "Temp:");
	OLED_ShowString(3, 1, "Mode:");
}


int main(void)
{
	/*模块初始化*/
	Hardware_Init();
	
	while (1)
	{
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
		
		// 更新时间计数
		if (!currentTime.isSettingMode)  // 只在非设置模式下更新时间
		{
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
		}
		UpdateTimeDisplay(); //更新时间
		TIM_ClearITPendingBit(TIM3, TIM_IT_Update);
	}
}
