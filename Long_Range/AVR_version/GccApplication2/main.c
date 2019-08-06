#include <avr/io.h>
#include <stdlib.h>
#include <stdio.h>
#define DHTpin PD2
#define SoilPin PC0
#define LightPin PC1
#define motor1 PD7
#define motor2 PD6
#define ENA PB1

void Request();
void Response();
uint8_t Receive_data();
void ADC_Init();
int ADC_Read(int);
void UART_init(long);
unsigned char UART_RxChar();
void UART_TxChar(char);
void UART_SendString(char *);
uint8_t c=0,I_RH,D_RH,I_Temp,D_Temp,CheckSum;
int motor_status = 0;
byte buf[20];
uint8_t soil, light,temp,humid;
char c;
int main(void)
{
    DDRC &= ~ (1<<SoilPin)|(1<<PC1); //soil moisture and light sensor as input
	DDRC |= (1<<motor1) | (1<<motor2); //motor pins as output
	DDRD &= ~ (1<<DHT11_PIN); //DHT11 sensor pin as input
	DDRB |= (1<<PB1); //ENable pin of L298N as ouput
	
	// Set Fast PWM, TOP in ICR1, Clear OC1A on compare match, clk/64 
	TCCR1A = (1<<WGM11)|(1<<COM1A1);
	TCCR1B = (1<<WGM12)|(1<<WGM13)|(1<<CS10)|(1<<CS11);
	
    while (1) 
    {
		Request();		// send start pulse 
		Response();		// receive response 
		I_RH=Receive_data();	// store first eight bit in I_RH 
		D_RH=Receive_data();	// store next eight bit in D_RH
		I_Temp=Receive_data();	// store next eight bit in I_Temp 
		D_Temp=Receive_data();	// store next eight bit in D_Temp 
		CheckSum=Receive_data();// store next eight bit in CheckSum 
		if ((I_RH + D_RH + I_Temp + D_Temp) == CheckSum){
			humid=I_RH;
			temp=I_Temp;
			
		}
		soil=100-((ADC_Read(1))*100.00)/1023.00;  //soil moisture value in %
		light=ADC_Read(1);  //light sensor value
		
		buf[0] = 0xFE;
		buf[1] = 0x0A;
		buf[2] = 0x14;
		buf[3] = 0x3E;
		buf[4] = 0xFF;
		buf[5] = 0x05;
		buf[6] = 0x05;

		buf[7] = (light >> 8)  & 255;
		buf[8] = light & 255;
		
		buf[9] = (soil >> 8)  & 255;
		buf[10] = soil & 255;
		
		buf[11] = (humid >> 8)  & 255;
		buf[12] = humid & 255;
		
		buf[13] = (temp >> 8)  & 255;
		buf[14] = temp & 255;
		
		buf[15] = motor_status;
		
		UART_init(115200);
		UART_SendString(buf);
		c=UART_RxChar();
		if(c=="1"){
			PORTD |= (1<<motor1);
			PORTD &=~ (1<<motor2);
			OCR1A =127;
			motor_status=1;
		}
		if(c=="0"){
			PORTD &=~ (1<<motor2) | (1<< motor1);
			OCR1A = 0;
			motor_status=0;
		}
    }
}

void Request()				// Microcontroller send start pulse/request 
{
	//DDRD |= (1<<DHTpin);
	PORTD &= ~(1<<DHTpin);	/// set to low pin 
	_delay_ms(20);			// wait for 20ms 
	PORTD |= (1<<DHTpin);	// set to high pin 
}

void Response()				// receive response from DHT11 
{
	DDRD &= ~(1<<DHTpin);
	while(PIND & (1<<DHTpin));
	while((PIND & (1<<DHTpin))==0);
	while(PIND & (1<<DHTpin));
}

uint8_t Receive_data()			// receive data 
{	
	for (int q=0; q<8; q++)
	{
		while((PIND & (1<<DHTpin)) == 0);  // check received bit 0 or 1 
		_delay_us(30);
		if(PIND & (1<<DHTpin))// if high pulse is greater than 30ms 
		c = (c<<1)|(0x01);	// then its logic HIGH 
		else			// otherwise its logic LOW 
		c = (c<<1);
		while(PIND & (1<<DHTpin));
	}
	return c;
}
void ADC_Init(){
	ADCSRA =0x87;
	
}
int ADC_Read(int channel){
	int Ain,AinLow;
	
	ADMUX=ADMUX|(channel & 0x0f);	// Set input channel to read 

	ADCSRA |= (1<<ADSC);		// Start conversion 
	while((ADCSRA&(1<<ADIF))==0);	// Monitor end of conversion interrupt 
	
	_delay_us(10);
	AinLow = (int)ADCL;		// Read lower byte
	Ain = (int)ADCH*256;		// Read higher 2 bits and Multiply with weight 
	Ain = Ain + AinLow;				
	return(Ain);			// Return digital value
}
void UART_init(long USART_BAUDRATE){
	UCSRB |= (1 << RXEN) | (1 << TXEN);	// Turn on transmission and reception 
	UCSRC |= (1 << URSEL) | (1 << UCSZ0) | (1 << UCSZ1);// Use 8-bit char size 
	UBRRL = BAUD_PRESCALE;			// Load lower 8-bits of the baud rate 
	UBRRH = (BAUD_PRESCALE >> 8);		// Load upper 8-bits
}
unsigned char UART_RxChar(){
	while ((UCSRA & (1 << RXC)) == 0);// Wait till data is received 
	return(UDR);		// Return the byte 
}
void UART_TxChar(char){
}
void UART_SendString(char *){
	unsigned char j=0;
	
	while (str[j]!=0)		/* Send string till null */
	{
		UART_TxChar(str[j]);	
		j++;
	}
}











