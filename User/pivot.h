#ifndef __pivot_H
#define __pivot_H
#include "sys.h"
#include "Delay.h"
#include "LED.h"
#include "PWM.h"
#include "OLED.h"
#include "control.h"
#include "Key.h"
#include "Timer.h"

void Hardware_Init();
uint32_t GetGpioClock(GPIO_TypeDef* GPIOx);



#endif
