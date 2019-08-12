#include <DHT.h>
#include <TimerOne.h>
#include <SoftwareSerial.h>
#define DHTPIN 2 //nhiet do do am
#define DHTTYPE DHT11

#define ENABLE_DEVICE   0x01
#define DISABLE_DEVICE  0x02
#define DEVICE_DEFAULT  0x00

int flag_device = DEVICE_DEFAULT;

char cID[5] = {'c', 'K', 'F', '1', '3'};

SoftwareSerial mySerial(6, 7); // RX, TX

int LDR =1;   //quang tro
int moist=0;  //do am dat
//int battery=2;
int led = 25;

int M0 = 3;
int M1 = 4;
int Motor_Pin=10;
int con1=9,con2=8;
int status=0;

DHT dht(DHTPIN,DHTTYPE);
void setup() {
  Serial.begin(9600);
  dht.begin();
  
  pinMode(LED_BUILTIN, OUTPUT);
  pinMode(LDR, INPUT); 
  pinMode(moist, INPUT);
  pinMode(M0, OUTPUT);
  pinMode(M1, OUTPUT);
  pinMode(EN,OUTPUT);
  pinMode(con1,OUTPUT);
  pinMode(con2,OUTPUT);
  digitalWrite(M0, LOW);
  digitalWrite(M1, LOW);
    
  
  digitalWrite(con1, LOW);
  digitalWrite(con2, HIGH);
  
  mySerial.begin(38400);

  Timer1.initialize(100000); //100ms
  Timer1.attachInterrupt(callback);
}

byte buf[20];
void UpdateData(byte stt, int l, int m, int h, int t,int status)
{
  buf[0] = 0xFE;
  buf[1] = 0x0A;
  buf[2] = 0x14;
  buf[3] = 0x3E;

  buf[4] = 0xFF;
  buf[5] = 0x05;

  buf[6] = stt; //status connect

  //Quang tro
  buf[7] = (l >> 8)  & 255;
  buf[8] = l & 255;
  //Do am dat
  buf[9] = (m >> 8)  & 255;
  buf[10] = m & 255;
  //Do am
  buf[11] = (h >> 8)  & 255;
  buf[12] = h & 255;
  //Do am
  buf[13] = (t >> 8)  & 255;
  buf[14] = t & 255;
  buf[15] = status;
}
int l, m;
int h,t;
void GetData(void)
{
  l=analogRead(LDR);        //quang tro
  m=analogRead(moist);      //Do am dat
  h=dht.readHumidity();     //Do am
  t=dht.readTemperature();  //Nhiet do
  m = constrain(m,485,1023);
  m = map(m,485,1023,100,0); 
}
void SendData(byte buff[], int len)
{
  //mySerial.write(buff, sizeof(buf));
  for(int i = 0; i < len; i++)
  {
    mySerial.print(buff[i]);
    mySerial.print(' '); 
  }
  mySerial.println();
}

void SendDataTest(byte buff[], int len)
{
  //mySerial.write(buff, sizeof(buf));
  for(int i = 0; i < len; i++)
  {
    Serial.print(buff[i]);
    Serial.print(' '); 
  }
  Serial.println();
}
void flashLED(int t)
{
  digitalWrite(LED_BUILTIN, HIGH);    // turn the LED on (HIGH is the voltage level)
  delay(t);                           // wait for a second
  digitalWrite(LED_BUILTIN, LOW);     // turn the LED off by making the voltage LOW
  delay(t);
}
int  serIn;             // var that will hold the bytes-in read from the serialBuffer
char serInString[100];  // array that will hold the different bytes  100=100characters;
                        // -> you must state how long the array will be else it won't work.
int  serInIndx  = 0;    // index of serInString[] in which to insert the next incoming byte
int  serOutIndx = 0;    // index of the outgoing serInString[] array;

void readSerialString () {
  if(flag_device == DISABLE_DEVICE || flag_device == DEVICE_DEFAULT)
  {
    int sb;   
    if(Serial.available()) { 
       while (Serial.available()){ 
          sb = Serial.read();             
          serInString[serInIndx] = sb;
          serInIndx++;
       }
    }
    if(mySerial.available()) { 
       while (mySerial.available()){ 
          sb = mySerial.read();             
          serInString[serInIndx] = sb;
          serInIndx++;
       }
    }
  }  
}

void CheckSerialString() {
  if( serInIndx > 0) {
    for(serOutIndx=0; serOutIndx < serInIndx; serOutIndx++) {
      Serial.print( serInString[serOutIndx] );
    }
	if( serInString[0]==1){
		 analogWrite(Motor_Pin,127);
		 status=1;
	}
	if(serInString[0]==0){
		analogWrite(Motor_Pin,0);
		status=0;
	}
    if( cID[0] == serInString[0]
        && cID[1] == serInString[1]
        && cID[2] == serInString[2]
        && cID[3] == serInString[3]
        && cID[4] == serInString[4])
    {
      flag_device = ENABLE_DEVICE;
      mySerial.println("ENABLE_DEVICE");
    }
    else
    {
      flag_device = DISABLE_DEVICE;
      mySerial.println("ENABLE_FALSE");
    }     
    serOutIndx = 0;
    serInIndx  = 0;
  }
}
void callback()
{
    //digitalWrite(LED_BUILTIN, digitalRead(LED_BUILTIN) ^ 1);
    readSerialString();
    CheckSerialString();
}
void loop() {
  
    GetData();
    
   if(flag_device == ENABLE_DEVICE){
      Serial.println(">SendOK");
      UpdateData(0x01, l, m, h, t,status);
      SendData(buf, 15);
      delay(2000);
    }
    else if(flag_device == DISABLE_DEVICE || flag_device == DEVICE_DEFAULT)
    {
        UpdateData(0x00, l, m, h, t,status);
        SendData(buf, 7);
        Serial.println(">Sleep");
        delay(3000);
    }
	
	}
}
