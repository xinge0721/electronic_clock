#ifndef __PWM_H
#define __PWM_H

#include "stm32f10x.h"

#ifdef __cplusplus
extern "C" {
#endif

// PWM相关函数声明
void PWM_Init(void);                    // PWM初始化
void PWM_SetCompare1(uint16_t Compare); // 设置比较值1
void PWM_SetCompare2(uint16_t Compare); // 设置比较值2
void PWM_SetCompare3(uint16_t Compare); // 设置比较值3
void PWM_SetCompare4(uint16_t Compare); // 设置比较值4
void PWM_SetPrescaler(uint16_t Prescaler); // 设置预分频值
void PWM_SetAutoreload(uint16_t Autoreload); // 设置自动重装值

#ifdef __cplusplus
}
#endif

#endif
