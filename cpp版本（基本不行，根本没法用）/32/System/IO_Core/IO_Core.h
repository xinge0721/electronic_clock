#ifndef IO_CORE_H
#define IO_CORE_H

#include "stm32f10x.h"
#include "stddef.h"

#ifdef __cplusplus
extern "C" {
#endif

// GPIO操作函数
void GPIO_Init_Custom(GPIO_TypeDef* GPIOx, uint16_t Pin, GPIOMode_TypeDef mode);
void GPIO_Write(GPIO_TypeDef* GPIOx, uint16_t Pin, uint8_t state);
uint8_t GPIO_Read(GPIO_TypeDef* GPIOx, uint16_t Pin);

// PWM相关函数
void PWM_Init_Custom(GPIO_TypeDef* Oc1_GPIO, uint16_t Oc1_Pin,
                    GPIO_TypeDef* Oc4_GPIO, uint16_t Oc4_Pin,
                    uint16_t arr, uint16_t psc);
void PWM_SetOc1(uint16_t value);
void PWM_SetOc4(uint16_t value);

// 编码器相关函数
void Encoder_Init(GPIO_TypeDef* EN1_GPIO, uint16_t EN1_Pin,
                 GPIO_TypeDef* EN2_GPIO, uint16_t EN2_Pin,
                 uint16_t arr, uint16_t psc);
int16_t Encoder_GetRight(void);
void Encoder_SetRight(int16_t* Temp);

#ifdef __cplusplus
}
#endif

#endif











