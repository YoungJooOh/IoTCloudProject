#include <Arduino.h>

#define WaterPump_OFF 0
#define WaterPump_ON 1

class WaterPump {
  private:
    int pin;
    int pin2;
    byte state;

  public:
    WaterPump(int pin, int pin2);
    void init();
    void on();
    void off();
    byte getState();
};
