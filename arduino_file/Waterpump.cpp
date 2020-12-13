#include "Waterpump.h"

WaterPump::WaterPump(int pin, int pin2) {
  // Use 'this->' to make the difference between the
  // 'pin' attribute of the class and the 
  // local variable 'pin' created from the parameter.
  this->pin = pin;
  this->pin2 = pin2;
  init();
}

void WaterPump::init() {
  pinMode(pin, OUTPUT);
  pinMode(pin2, OUTPUT);
  // Always try to avoid duplicate code.
  // Instead of writing digitalWrite(pin, LOW) here,
  // call the function off() which already does that
  off();
  state = WaterPump_OFF;
}

void WaterPump::on() {
  digitalWrite(pin, HIGH);  // 정방향으로 모터 회전
  digitalWrite(pin2, LOW);
  state = WaterPump_ON;
}

void WaterPump::off() {
  digitalWrite(pin, LOW);  // 정방향으로 모터 회전
  digitalWrite(pin2, LOW);
  state = WaterPump_OFF;
}

byte WaterPump::getState() {
  return state;
}
