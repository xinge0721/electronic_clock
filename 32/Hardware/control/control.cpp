#include "control.h"
wheel::wheel(
			GPIO_TypeDef* _IN1_GPIOx, uint16_t _IN1Pin, 
			GPIO_TypeDef* _IN2_GPIOx, uint16_t _IN2Pin)
			:IN1(_IN1_GPIOx, _IN1Pin,GPIO_Mode_Out_PP),
			IN2(_IN2_GPIOx, _IN2Pin,GPIO_Mode_Out_PP)
			
		{
			//初始化俩个都是低电平
			IN1.Write(0);
			IN2.Write(0);
		}
void wheel::Go(void)
{
			IN1.Write(0);
			IN2.Write(1);
}		
void wheel::Break(void)
{
			IN1.Write(1);
			IN2.Write(0);
}					
control::control(
						GPIO_TypeDef* left_IN1_GPIOx, uint16_t left_IN1Pin, 
						GPIO_TypeDef* left_IN2_GPIOx, uint16_t left_IN2Pin, 
						GPIO_TypeDef* right_IN1_GPIOx, uint16_t right_IN1Pin,
						GPIO_TypeDef* right_IN2_GPIOx, uint16_t right_IN2Pin,

						GPIO_TypeDef* Oc1_GPIOx, uint16_t Oc1_IN2Pin,
						GPIO_TypeDef* Oc4_GPIOx, uint16_t Oc4_IN2Pin,
						uint16_t arr,uint16_t psc)
					:left(left_IN1_GPIOx,left_IN1Pin,left_IN2_GPIOx,left_IN2Pin),
					right(right_IN1_GPIOx,right_IN1Pin,right_IN2_GPIOx,right_IN2Pin),
					pwm(Oc1_GPIOx,Oc1_IN2Pin,Oc4_GPIOx,Oc4_IN2Pin,arr,psc)
{
		
}

