#include "pivot.h"

void Hardware_Init()
{
	LED_Init();
	NVIC_Config();
	KEY_Init();
	TIM2_Int_Init(20, 7200);
}

