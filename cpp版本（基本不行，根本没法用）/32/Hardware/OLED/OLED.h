#ifndef OLED_H
#define OLED_H


//#ifdef __cplusplus
//extern "C" {
//#endif
//#include "stm32f10x.h"
//#ifdef __cplusplus
//}
//#endif

#include "stm32f10x.h"
#include "pivot.h"
#include "OLED_Font.h"
#include "IO_Core.h" 

class OLED {

public:
    OLED(GPIO_TypeDef* _SCL_GPIOx, uint16_t _sclPin, 
				 GPIO_TypeDef* _SDA_GPIOx, uint16_t _sdaPin);
		void Init(void);
		void Clear(void);

		void ShowChar(uint8_t Line, uint8_t Column, char Char);
		void ShowString(uint8_t Line, uint8_t Column, char *String);
		void ShowNum(uint8_t Line, uint8_t Column, uint32_t Number, uint8_t Length);
		void ShowSignedNum(uint8_t Line, uint8_t Column, int32_t Number, uint8_t Length);
		void ShowHexNum(uint8_t Line, uint8_t Column, uint32_t Number, uint8_t Length);
		void ShowBinNum(uint8_t Line, uint8_t Column, uint32_t Number, uint8_t Length);

		// 修正后的SCL/SDA控制函数
		inline void OLED_W_SDA(uint8_t x) {
//				GPIO_WriteBit(SDA_GPIOx, sdaPin, (BitAction)x);
				SDA.Write(x);
		}
		inline void OLED_W_SCL(uint8_t x) {
//				GPIO_WriteBit(SCL_GPIOx, sclPin, (BitAction)x);
				SCL.Write(x);
		}
		
		private:
    // 私有成员变量
		GPIO SCL;
		GPIO SDA;

		
		void WriteData(uint8_t Data);
		void WriteCommand(uint8_t Command);
		void I2C_SendByte(uint8_t Byte);
		void I2C_Stop(void);
		void I2C_Start(void);
		void SetCursor(uint8_t Y, uint8_t X);
		uint32_t Pow(uint32_t X, uint32_t Y);


};


#endif
