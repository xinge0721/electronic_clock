#include "pivot.h"

void Hardware_Init()
{
	LED_Init();
	KEY_Init();
	Serial_Init();
	TIM2_Int_Init(72, 1000);

}

