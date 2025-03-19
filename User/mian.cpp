#include "stm32f10x.h"                  // Device header
#include "pivot.h"
//≥ı ºªØ
OLED oled(GPIOB, GPIO_Pin_8, GPIOB, GPIO_Pin_9);
Key one(GPIOB, GPIO_Pin_8);
Key tow(GPIOB, GPIO_Pin_9);

int main()
{
	Hardware_Init();


	while(1)
	{


	}
}


void TIM2_IRQHandler(void)
{
	if (TIM_GetITStatus(TIM2, TIM_IT_Update) == SET)
	{
		
		TIM_ClearITPendingBit(TIM2, TIM_IT_Update);
	}
}

