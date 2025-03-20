#include "OLED.h"

//#define this->W_SCL(GPIOx, pin,x) GPIO_WriteBit(GPIOx, pin, x)
//#define this->W_SDA(GPIOx, pin,x) GPIO_WriteBit(GPIOx, pin, x)

OLED::OLED(GPIO_TypeDef* _SCL_GPIOx, uint16_t _sclPin, GPIO_TypeDef* _SDA_GPIOx, uint16_t _sdaPin) 
			:SCL(_SCL_GPIOx,_sclPin,GPIO_Mode_Out_OD),
		  SDA(_SDA_GPIOx,_sdaPin,GPIO_Mode_Out_OD)
{
        // 初始化为高电平
        OLED_W_SCL(1);
        OLED_W_SDA(1);
				
				Delay_ms(500);

				this->Init();
				

    }
		/**
			* @brief  I2C开始
			* @param  无
			* @retval 无
			*/
		void OLED::I2C_Start(void)
		{
			OLED_W_SDA(1);
			OLED_W_SDA(0);
			OLED_W_SCL(0);
		}

		/**
			* @brief  I2C停止
			* @param  无
			* @retval 无
			*/
		void OLED::I2C_Stop(void)
		{
			OLED_W_SDA(0);
			OLED_W_SCL(1);
			OLED_W_SDA(1);
		}

		/**
			* @brief  I2C发送一个字节
			* @param  Byte 要发送的一个字节
			* @retval 无
			*/
		void OLED::I2C_SendByte(uint8_t Byte)
		{
			uint8_t i;
			for (i = 0; i < 8; i++)
			{
				OLED_W_SDA(Byte & (0x80 >> i));
				OLED_W_SCL(1);
				OLED_W_SCL(0);
			}
			OLED_W_SCL(1);	//额外的一个时钟，不处理应答信号
			OLED_W_SCL(0);
		}

		/**
			* @brief  OLED写命令
			* @param  Command 要写入的命令
			* @retval 无
			*/
		void OLED::WriteCommand(uint8_t Command)
		{
			this->I2C_Start();
			this->I2C_SendByte(0x78);		//从机地址
			this->I2C_SendByte(0x00);		//写命令
			this->I2C_SendByte(Command); 
			this->I2C_Stop();
		}

		/**
			* @brief  OLED写数据
			* @param  Data 要写入的数据
			* @retval 无
			*/
		void OLED::WriteData(uint8_t Data)
		{
			this->I2C_Start();
			this->I2C_SendByte(0x78);		//从机地址
			this->I2C_SendByte(0x40);		//写数据
			this->I2C_SendByte(Data);
			this->I2C_Stop();
		}

		/**
			* @brief  OLED设置光标位置
			* @param  Y 以左上角为原点，向下方向的坐标，范围：0~7
			* @param  X 以左上角为原点，向右方向的坐标，范围：0~127
			* @retval 无
			*/
		void OLED::SetCursor(uint8_t Y, uint8_t X)
		{
			 this->WriteCommand(0xB0 | Y);					//设置Y位置
			 this->WriteCommand(0x10 | ((X & 0xF0) >> 4));	//设置X位置高4位
			 this->WriteCommand(0x00 | (X & 0x0F));			//设置X位置低4位
		}

		/**
			* @brief  OLED清屏
			* @param  无
			* @retval 无
			*/
		void OLED::Clear(void)
		{  
			uint8_t i, j;
			for (j = 0; j < 8; j++)
			{
				 SetCursor(j, 0);
				for(i = 0; i < 128; i++)
				{
					 WriteData(0x00);
				}
			}
		}

		/**
			* @brief  OLED显示一个字符
			* @param  Line 行位置，范围：1~4
			* @param  Column 列位置，范围：1~16
			* @param  Char 要显示的一个字符，范围：ASCII可见字符
			* @retval 无
			*/
		void OLED::ShowChar(uint8_t Line, uint8_t Column, char Char)
		{      	
			uint8_t i;
			 this->SetCursor((Line - 1) * 2, (Column - 1) * 8);		//设置光标位置在上半部分
			for (i = 0; i < 8; i++)
			{
				this->WriteData(OLED_F8x16[Char - ' '][i]);			//显示上半部分内容
			}
			this->SetCursor((Line - 1) * 2 + 1, (Column - 1) * 8);	//设置光标位置在下半部分
			for (i = 0; i < 8; i++)
			{
				this->WriteData(OLED_F8x16[Char - ' '][i + 8]);		//显示下半部分内容
			}
		}

		/**
			* @brief  OLED显示字符串
			* @param  Line 起始行位置，范围：1~4
			* @param  Column 起始列位置，范围：1~16
			* @param  String 要显示的字符串，范围：ASCII可见字符
			* @retval 无
			*/
		void OLED::ShowString(uint8_t Line, uint8_t Column, char *String)
		{
			uint8_t i;
			for (i = 0; String[i] != '\0'; i++)
			{
				this->ShowChar(Line, Column + i, String[i]);
			}
		}

		/**
			* @brief  OLED次方函数
			* @retval 返回值等于X的Y次方
			*/
		uint32_t OLED::Pow(uint32_t X, uint32_t Y)
		{
			uint32_t Result = 1;
			while (Y--)
			{
				Result *= X;
			}
			return Result;
		}

		/**
			* @brief  OLED显示数字（十进制，正数）
			* @param  Line 起始行位置，范围：1~4
			* @param  Column 起始列位置，范围：1~16
			* @param  Number 要显示的数字，范围：0~4294967295
			* @param  Length 要显示数字的长度，范围：1~10
			* @retval 无
			*/
		void OLED::ShowNum(uint8_t Line, uint8_t Column, uint32_t Number, uint8_t Length)
		{
			uint8_t i;
			for (i = 0; i < Length; i++)							
			{
				this->ShowChar(Line, Column + i, Number / this->Pow(10, Length - i - 1) % 10 + '0');
			}
		}

		/**
			* @brief  OLED显示数字（十进制，带符号数）
			* @param  Line 起始行位置，范围：1~4
			* @param  Column 起始列位置，范围：1~16
			* @param  Number 要显示的数字，范围：-2147483648~2147483647
			* @param  Length 要显示数字的长度，范围：1~10
			* @retval 无
			*/
		void OLED::ShowSignedNum(uint8_t Line, uint8_t Column, int32_t Number, uint8_t Length)
		{
			uint8_t i;
			uint32_t Number1;
			if (Number >= 0)
			{
				this->ShowChar(Line, Column, '+');
				Number1 = Number;
			}
			else
			{
				this->ShowChar(Line, Column, '-');
				Number1 = -Number;
			}
			for (i = 0; i < Length; i++)							
			{
				this->ShowChar(Line, Column + i + 1, Number1 / this->Pow(10, Length - i - 1) % 10 + '0');
			}
		}

		/**
			* @brief  OLED显示数字（十六进制，正数）
			* @param  Line 起始行位置，范围：1~4
			* @param  Column 起始列位置，范围：1~16
			* @param  Number 要显示的数字，范围：0~0xFFFFFFFF
			* @param  Length 要显示数字的长度，范围：1~8
			* @retval 无
			*/
		void OLED::ShowHexNum(uint8_t Line, uint8_t Column, uint32_t Number, uint8_t Length)
		{
			uint8_t i, SingleNumber;
			for (i = 0; i < Length; i++)							
			{
				SingleNumber = Number / this->Pow(16, Length - i - 1) % 16;
				if (SingleNumber < 10)
				{
					this->ShowChar(Line, Column + i, SingleNumber + '0');
				}
				else
				{
					this->ShowChar(Line, Column + i, SingleNumber - 10 + 'A');
				}
			}
		}

		/**
			* @brief  OLED显示数字（二进制，正数）
			* @param  Line 起始行位置，范围：1~4
			* @param  Column 起始列位置，范围：1~16
			* @param  Number 要显示的数字，范围：0~1111 1111 1111 1111
			* @param  Length 要显示数字的长度，范围：1~16
			* @retval 无
			*/
		void OLED::ShowBinNum(uint8_t Line, uint8_t Column, uint32_t Number, uint8_t Length)
		{
			uint8_t i;
			for (i = 0; i < Length; i++)							
			{
				this->ShowChar(Line, Column + i, Number / this->Pow(2, Length - i - 1) % 2 + '0');
			}
		}

		/**
			* @brief  OLED初始化
			* @param  无
			* @retval 无
			*/
		void OLED::Init(void)
		{

			this->WriteCommand(0xAE);	//关闭显示
			
			this->WriteCommand(0xD5);	//设置显示时钟分频比/振荡器频率
			this->WriteCommand(0x80);
			
			this->WriteCommand(0xA8);	//设置多路复用率
			this->WriteCommand(0x3F);
			
			this->WriteCommand(0xD3);	//设置显示偏移
			this->WriteCommand(0x00);
			
			this->WriteCommand(0x40);	//设置显示开始行
			
			this->WriteCommand(0xA1);	//设置左右方向，0xA1正常 0xA0左右反置
			
			this->WriteCommand(0xC8);	//设置上下方向，0xC8正常 0xC0上下反置

			this->WriteCommand(0xDA);	//设置COM引脚硬件配置
			this->WriteCommand(0x12);
			
			this->WriteCommand(0x81);	//设置对比度控制
			this->WriteCommand(0xCF);

			this->WriteCommand(0xD9);	//设置预充电周期
			this->WriteCommand(0xF1);

			this->WriteCommand(0xDB);	//设置VCOMH取消选择级别
			this->WriteCommand(0x30);

			this->WriteCommand(0xA4);	//设置整个显示打开/关闭

			this->WriteCommand(0xA6);	//设置正常/倒转显示

			this->WriteCommand(0x8D);	//设置充电泵
			this->WriteCommand(0x14);

			this->WriteCommand(0xAF);	//开启显示
				
			this->Clear();				//OLED清屏
		}
		

