#include "pwm.h"

/**
  * 函    数：PWM初始化
  * 参    数：无
  * 返 回 值：无
  * 注意事项：默认使用TIM2的四个通道
  *           CH1 - PA0
  *           CH2 - PA1
  *           CH3 - PA2
  *           CH4 - PA3
  */
void PWM_Init(void)
{
    // 开启时钟
    RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOA, ENABLE);
    RCC_APB1PeriphClockCmd(RCC_APB1Periph_TIM2, ENABLE);
    
    // 配置GPIO
    GPIO_InitTypeDef GPIO_InitStructure;
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AF_PP;
    GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_0 | GPIO_Pin_1 | GPIO_Pin_2 | GPIO_Pin_3;
    GPIO_Init(GPIOA, &GPIO_InitStructure);
    
    // 配置时基单元
    TIM_TimeBaseInitTypeDef TIM_TimeBaseInitStructure;
    TIM_TimeBaseInitStructure.TIM_ClockDivision = TIM_CKD_DIV1;
    TIM_TimeBaseInitStructure.TIM_CounterMode = TIM_CounterMode_Up;
    TIM_TimeBaseInitStructure.TIM_Period = 100 - 1;     // ARR，设置PWM频率为72M/100/7200=10KHz
    TIM_TimeBaseInitStructure.TIM_Prescaler = 7200 - 1; // PSC
    TIM_TimeBaseInitStructure.TIM_RepetitionCounter = 0;
    TIM_TimeBaseInit(TIM2, &TIM_TimeBaseInitStructure);
    
    // 配置输出比较单元
    TIM_OCInitTypeDef TIM_OCInitStructure;
    TIM_OCStructInit(&TIM_OCInitStructure);
    TIM_OCInitStructure.TIM_OCMode = TIM_OCMode_PWM1;
    TIM_OCInitStructure.TIM_OCPolarity = TIM_OCPolarity_High;
    TIM_OCInitStructure.TIM_OutputState = TIM_OutputState_Enable;
    TIM_OCInitStructure.TIM_Pulse = 0;  // CCR，初始值设为0
    
    // 初始化四个通道
    TIM_OC1Init(TIM2, &TIM_OCInitStructure);
    TIM_OC2Init(TIM2, &TIM_OCInitStructure);
    TIM_OC3Init(TIM2, &TIM_OCInitStructure);
    TIM_OC4Init(TIM2, &TIM_OCInitStructure);
    
    // 使能预装载寄存器
    TIM_OC1PreloadConfig(TIM2, TIM_OCPreload_Enable);
    TIM_OC2PreloadConfig(TIM2, TIM_OCPreload_Enable);
    TIM_OC3PreloadConfig(TIM2, TIM_OCPreload_Enable);
    TIM_OC4PreloadConfig(TIM2, TIM_OCPreload_Enable);
    
    // 使能自动重装载的预装载寄存器
    TIM_ARRPreloadConfig(TIM2, ENABLE);
    
    // 使能定时器
    TIM_Cmd(TIM2, ENABLE);
}

/**
  * 函    数：设置通道1的比较值
  * 参    数：Compare 要设置的比较值，范围：0~100
  * 返 回 值：无
  */
void PWM_SetCompare1(uint16_t Compare)
{
    TIM_SetCompare1(TIM2, Compare);
}

/**
  * 函    数：设置通道2的比较值
  * 参    数：Compare 要设置的比较值，范围：0~100
  * 返 回 值：无
  */
void PWM_SetCompare2(uint16_t Compare)
{
    TIM_SetCompare2(TIM2, Compare);
}

/**
  * 函    数：设置通道3的比较值
  * 参    数：Compare 要设置的比较值，范围：0~100
  * 返 回 值：无
  */
void PWM_SetCompare3(uint16_t Compare)
{
    TIM_SetCompare3(TIM2, Compare);
}

/**
  * 函    数：设置通道4的比较值
  * 参    数：Compare 要设置的比较值，范围：0~100
  * 返 回 值：无
  */
void PWM_SetCompare4(uint16_t Compare)
{
    TIM_SetCompare4(TIM2, Compare);
}

/**
  * 函    数：设置预分频值
  * 参    数：Prescaler 要设置的预分频值
  * 返 回 值：无
  */
void PWM_SetPrescaler(uint16_t Prescaler)
{
    TIM_PrescalerConfig(TIM2, Prescaler, TIM_PSCReloadMode_Immediate);
}

/**
  * 函    数：设置自动重装值
  * 参    数：Autoreload 要设置的自动重装值
  * 返 回 值：无
  */
void PWM_SetAutoreload(uint16_t Autoreload)
{
    TIM_SetAutoreload(TIM2, Autoreload);
} 