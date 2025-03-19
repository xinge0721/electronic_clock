#ifndef IO_CORE_H  // 推荐使用普通名称
#define IO_CORE_H

#include "stm32f10x.h"                  // Device header
#include "stddef.h"

class GPIO {

public:
    GPIO_TypeDef* GPIOx;
    uint16_t Pin;
		GPIO(GPIO_TypeDef* _GPIOx, uint16_t _Pin,GPIOMode_TypeDef mode);

    // 通用IO操作
    void Write(bool state);

    bool Read(void);
};

typedef struct {
    GPIO_TypeDef* GPIOx;
    uint16_t Pin;
    TIM_TypeDef* TIMx;
} TIM_GPIO_Mapping;

extern const TIM_GPIO_Mapping timMap[];  


class PWM
{
private:
	GPIO Oc1;
	GPIO Oc4;
	TIM_TypeDef* pwm_tim;


public:
	PWM(GPIO_TypeDef* _Oc1, u16 _Oc1Pin,
			GPIO_TypeDef* _Oc4, u16 _Oc4Pin,
			u16 arr,u16 psc);
	void setOc1(u16 value = 0);
	void setOc4(u16 value = 0);

};


class encoder
{
	private:
	GPIO EN1;
	GPIO EN2;
	TIM_TypeDef* timx;
	public:
	encoder(GPIO_TypeDef* _EN1, u16 _EN1Pin,
			GPIO_TypeDef* _EN2, u16 _EN2Pin,
			u16 arr,u16 psc);
	int16_t Right(void);
	void Right(int16_t &Temp) ;


};

#endif











