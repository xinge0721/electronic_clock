#ifndef __control_H
#define __control_H

#include "sys.h"
#include "IO_Core.h" 

class wheel
{
private:
		GPIO IN1;
		GPIO IN2;
public:

	wheel(
			GPIO_TypeDef* _IN1_GPIOx, uint16_t _IN1Pin, 
			GPIO_TypeDef* _IN2_GPIOx, uint16_t _IN2Pin);
	void Go(void);
	void Break(void);
};

class control
{

public:
		control(
						GPIO_TypeDef* left_IN1_GPIOx, uint16_t left_IN1Pin, 
						GPIO_TypeDef* left_IN2_GPIOx, uint16_t left_IN2Pin, 
						GPIO_TypeDef* right_IN1_GPIOx, uint16_t right_IN1Pin,
						GPIO_TypeDef* right_IN2_GPIOx, uint16_t right_IN2Pin,

						GPIO_TypeDef* Oc1_GPIOx, uint16_t Oc1_IN2Pin,
						GPIO_TypeDef* Oc4_GPIOx, uint16_t Oc4_IN2Pin,
						uint16_t arr,uint16_t psc);

			wheel left;
			wheel right;
			PWM pwm;
private:
};

#endif
