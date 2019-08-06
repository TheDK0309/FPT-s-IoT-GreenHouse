#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <Wire.h>
#include <DHT.h>

#define DHTTYPE DHT11
#define LDR A0
#define DHT11_pin D1
#define motor1 D6
#define motor2 D7
#define enA D5
#define soil_moist D2

int light,soil;
int humid,temp;
int motor_status=0;

const char* ssid = "WiPi";
const char* password = "raspberry";
const char* mqtt_server = "192.168.4.1";
const char* mqtt_username="pi";
const char* mqtt_password="raspberry";

DHT dht;

WiFiClient espClient;
PubSubClient client(espClient);

void setup(){
  Serial.begin(115200);
  pinMode(LDR,INPUT);
  pinMode(DHT11_pin,INPUT);
  pinMode(soil_moist,INPUT);
  
  pinMode(enA, OUTPUT);
  pinMode(motor1, OUTPUT);
  pinMode(motor2, OUTPUT);

  setupwifi();
  
  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);
  
}
void loop(){
  if (!client.connected()) {
    reconnect();
  }
  client.publish("NodeStatus","CONNECTED!!");
  client.subscribe("Motor");

  light=analogRead(LDR);       
  humid=dht.readHumidity();     
  temp=dht.readTemperature();  

  soil=analogRead(soil_moist);
  soil = constrain(m,485,1023);
  soil = map(m,485,1023,100,0); 
  
  client.publish("node1/temp",temp);
  client.publish("node1/humid",humid);
  client.publish("node1/light",light);
  client.publish("node1/soil",soil);
  client.publish("node1/motor_status",motor_status);
  client.loop();
  delay(250);
}
void setupwifi(){
  delay(10);
  
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}
void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  for (int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println();
  if((char)payload[0] == '1'){ 
    Serial.print("Motor ON!");
    motor_status=1;
    clock_wise();
    analogWrite(enA, 127);
  }
  else{ 
    Serial.print("Motor OFF!");
    motor_status=0;
    digitalWrite(motor2, LOW);
    digitalWrite(motor1, LOW);
    analogWrite(enA, 0);
  }
}
void reconnect() {
  // Loop until we're reconnected
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Attempt to connect
    WiFi.mode(WIFI_STA);
    if (client.connect("ESP8266_light",mqtt_username,mqtt_password)) {
      Serial.println("connected");
      
      //client.subscribe("inTopic");
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}
void clock_wise(){
  digitalWrite(motor1, HIGH);
  digitalWrite(motor2, LOW);
}
void anti_clock_wise(){
  digitalWrite(motor2, HIGH);
  digitalWrite(motor1, LOW);
}
