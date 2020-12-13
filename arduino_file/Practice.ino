//코드

#include <ArduinoBearSSL.h>
#include <ArduinoECCX08.h>
#include <ArduinoMqttClient.h>
#include <WiFiNINA.h> // change to #include <WiFi101.h> for MKR1000

//문
#include <Stepper.h>

#include "arduino_secrets.h"
#include "DHT.h"
#define DHTPIN 2     // Digital pin connected to the DHT sensor
#define DHTTYPE DHT11   // DHT 11
DHT dht(DHTPIN, DHTTYPE);

//#define LED_1_PIN 5
#define WaterPump_1_PIN1 10
#define WaterPump_1_PIN2 6

#include <ArduinoJson.h>
//#include "Led.h"
#include "Waterpump.h"

/////// Enter your sensitive data in arduino_secrets.h
const char ssid[]        = SECRET_SSID;
const char pass[]        = SECRET_PASS;
const char broker[]      = SECRET_BROKER;
const char* certificate  = SECRET_CERTIFICATE;

WiFiClient    wifiClient;            // Used for the TCP socket connection
BearSSLClient sslClient(wifiClient); // Used for SSL/TLS connection, integrates with ECC508
MqttClient    mqttClient(sslClient);

unsigned long lastMillis = 0;

//Led led1(LED_1_PIN);
WaterPump waterpump1(WaterPump_1_PIN1, WaterPump_1_PIN2);

/////////////////////////////////////////////
int CDS = A1;   // 조도 센서 모듈 연결한 핀

//워터펌프
//int AA = 10;  // AA를 연결한 디지털 핀
//int AB = 6;   // AB를 연결한 디지털 핀

//프로펠러 propeller
int A = 8;
int B = 9;

//수위조절센서
int water_pin = A0; 

//LED
int led = 5;

//문
Stepper motor(2048, 1, 3, 2, 4); //IN1, IN3, IN2, IN4


/////////////////////////////////////////////

void setup() {


  //문
  motor.setSpeed(10); 
  
  // 조도 센서를 입력 핀으로 설정
  pinMode(CDS, INPUT);  

  //워터펌프
  pinMode(WaterPump_1_PIN1, OUTPUT); 
  pinMode(WaterPump_1_PIN2, OUTPUT);  

  //프로펠러
  pinMode(A,OUTPUT);
  pinMode(B,OUTPUT); 

  //수위조절
  pinMode( A0,INPUT); 

 //LED
 pinMode( led, OUTPUT); 
 


  
  Serial.begin(115200);
  while (!Serial);

  dht.begin();

  if (!ECCX08.begin()) {
    Serial.println("No ECCX08 present!");
    while (1);
  }

  // Set a callback to get the current time
  // used to validate the servers certificate
  ArduinoBearSSL.onGetTime(getTime);

  // Set the ECCX08 slot to use for the private key
  // and the accompanying public certificate for it
  sslClient.setEccSlot(0, certificate);

  // Optional, set the client id used for MQTT,
  // each device that is connected to the broker
  // must have a unique client id. The MQTTClient will generate
  // a client id for you based on the millis() value if not set
  //
  // mqttClient.setId("clientId");

  // Set the message callback, this function is
  // called when the MQTTClient receives a message
  mqttClient.onMessage(onMessageReceived);
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    connectWiFi();
  }

  if (!mqttClient.connected()) {
    // MQTT client is disconnected, connect
    connectMQTT();
  }

  // poll for new MQTT messages and send keep alives
  mqttClient.poll();

  // publish a message roughly every 5 seconds.
  if (millis() - lastMillis > 5000) {
    lastMillis = millis();
    char payload[512];
    getDeviceStatus(payload);
    sendMessage(payload);
  }
}

unsigned long getTime() {
  // get the current time from the WiFi module  
  return WiFi.getTime();
}

void connectWiFi() {
  Serial.print("Attempting to connect to SSID: ");
  Serial.print(ssid);
  Serial.print(" ");

  while (WiFi.begin(ssid, pass) != WL_CONNECTED) {
    // failed, retry
    Serial.print(".");
    delay(5000);
  }
  Serial.println();

  Serial.println("You're connected to the network");
  Serial.println();
}

void connectMQTT() {
  Serial.print("Attempting to MQTT broker: ");
  Serial.print(broker);
  Serial.println(" ");

  while (!mqttClient.connect(broker, 8883)) {
    // failed, retry
    Serial.print(".");
    delay(5000);
  }
  Serial.println();

  Serial.println("You're connected to the MQTT broker");
  Serial.println();

  // subscribe to a topic
  mqttClient.subscribe("$aws/things/MyMKRWiFi1010/shadow/update/delta");
}

void getDeviceStatus(char* payload) {
  // Read temperature as Celsius (the default)
  //float t = dht.readTemperature();

  // Read led status
  //const char* led = (led1.getState() == LED_ON)? "ON" : "OFF";

  
  // 조도 센서값 저장
  int CDS = analogRead(A1);  

  //수위 값 저장 
  int water = analogRead(A0); 

 
  
  const char* waterpump = (waterpump1.getState() == WaterPump_ON)? "ON" : "OFF";

  // make payload for the device update topic ($aws/things/MyMKRWiFi1010/shadow/update)
  //sprintf(payload,"{\"state\":{\"reported\":{\"temperature\":\"%0.2f\",\"LED\":\"%s\"}}}",t,led);
  
  sprintf(payload,"{\"state\":{\"reported\":{\"CDS\":\"%d\",\"water\":\"%d\",\"waterpump\":\"%s\",\"propeller\":\"%s\",\"motor\":\"%s\"}}}",CDS,water,waterpump,"OFF","OFF");


  if(CDS > 700)
  {               
     // 워터펌프 5초간 작동 
    digitalWrite(WaterPump_1_PIN1, HIGH); 
    digitalWrite(WaterPump_1_PIN2, LOW);
    delay(5000);
    
    if(analogRead(A0) > 400)
    {
      Serial.println(analogRead(A0));

      //모터정지
      digitalWrite(WaterPump_1_PIN1, LOW);  
      digitalWrite(WaterPump_1_PIN2, LOW);
      sprintf(payload,"{\"state\":{\"reported\":{\"CDS\":\"%d\",\"water\":\"%d\",\"waterpump\":\"%s\",\"propeller\":\"%s\"}}}",CDS,water,"success","OFF");
      Serial.println("워터펌프 정상작동");
      
      delay(5000);

      //프로펠러 5초간 작동 후 정지
      digitalWrite(A,HIGH); 
      digitalWrite(B,LOW);
      delay(5000); //5초 후
      digitalWrite(A,LOW);
      digitalWrite(B,LOW);
      sprintf(payload,"{\"state\":{\"reported\":{\"CDS\":\"%d\",\"water\":\"%d\",\"waterpump\":\"%s\",\"propeller\":\"%s\",\"motor\":\"%s\"}}}",CDS,water,"success","success","OFF");
      Serial.println("프로펠러 정상작동");

      //완료상태를 led로 알려줌
      digitalWrite(led, HIGH);
      delay(5000);
      digitalWrite(led, LOW);

      motor.step(2048); //오른쪽으로 한바퀴
      

    
    }
    else
    {
      digitalWrite(WaterPump_1_PIN1, LOW);  // 모터 정지
      digitalWrite(WaterPump_1_PIN2, LOW);
      sprintf(payload,"{\"state\":{\"reported\":{\"CDS\":\"%d\",\"water\":\"%d\",\"waterpump\":\"%s\",\"propeller\":\"%s\",\"motor\":\"%s\"}}}",CDS,water,"error","OFF","OFF");
      Serial.println("워터펌프 고장");
      
    }   
    
  }

  delay(5000);

}

void sendMessage(char* payload) {
  char TOPIC_NAME[]= "$aws/things/MyMKRWiFi1010/shadow/update";
  
  Serial.print("Publishing send message:");
  Serial.println(payload);
  mqttClient.beginMessage(TOPIC_NAME);
  mqttClient.print(payload);
  mqttClient.endMessage();
}


void onMessageReceived(int messageSize) {
  // we received a message, print out the topic and contents
  Serial.print("Received a message with topic '");
  Serial.print(mqttClient.messageTopic());
  Serial.print("', length ");
  Serial.print(messageSize);
  Serial.println(" bytes:");

  // store the message received to the buffer
  char buffer[512] ;
  int count=0;
  while (mqttClient.available()) {
     buffer[count++] = (char)mqttClient.read();
  }
  buffer[count]='\0'; // 버퍼의 마지막에 null 캐릭터 삽입
  Serial.println(buffer);
  Serial.println();

  // JSon 형식의 문자열인 buffer를 파싱하여 필요한 값을 얻어옴.
  // 디바이스가 구독한 토픽이 $aws/things/MyMKRWiFi1010/shadow/update/delta 이므로,
  // JSon 문자열 형식은 다음과 같다.
  // {
  //    "version":391,
  //    "timestamp":1572784097,
  //    "state":{
  //        "LED":"ON"
  //    },
  //    "metadata":{
  //        "LED":{
  //          "timestamp":15727840
  //         }
  //    }
  // }
  //
  DynamicJsonDocument doc(1024);
  deserializeJson(doc, buffer);
  JsonObject root = doc.as<JsonObject>();
  JsonObject state = root["state"];
  //워터펌프
  const char* waterpump = state["waterpump"];
  Serial.println(waterpump);

  
  char payload[512];

  if (strcmp(waterpump,"ON")==0) {
    waterpump1.on();
    sprintf(payload,"{\"state\":{\"reported\":{\"waterpump\":\"%s\"}}}","ON");
    sendMessage(payload);
    
  } else if (strcmp(waterpump,"OFF")==0) {
     waterpump1.off();
    sprintf(payload,"{\"state\":{\"reported\":{\"waterpump\":\"%s\"}}}","OFF");
    sendMessage(payload);
  }


  /*
  if (strcmp(led,"ON")==0) {
    led1.on();
    sprintf(payload,"{\"state\":{\"reported\":{\"LED\":\"%s\"}}}","ON");
    sendMessage(payload);
    
  } else if (strcmp(led,"OFF")==0) {
    led1.off();
    sprintf(payload,"{\"state\":{\"reported\":{\"LED\":\"%s\"}}}","OFF");
    sendMessage(payload);
  }
  */
}
