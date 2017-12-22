#include <LiquidCrystal_I2C.h>
#define LEDS 13
LiquidCrystal_I2C LCD(0x20, 16, 2);
void setup() {
    LCD.init();
    Serial1.begin(9600); //Bluetooth
    Serial.begin(9600); //Serial 
    digitalWrite(LEDS, LOW);
}

char btBuffer[1024]; //1024 characters
int index = 0;
int messageLength = 0;

void loop() {
  if(Serial1.available()){
      char c = (char)Serial1.read();
   
      if(c == '1')
      digitalWrite(LEDS, HIGH);
      else if(c == '2') digitalWrite(LEDS, LOW);
      
      messageLength++;
      if(messageLength > 1024) {
        Serial.println("MESSAGE CHARACTERS > 1024. Only 1024 will be displayed");
        messageLength = 0;
      }
      if(c == '\n'){
        writeToSerial();
      }else{
        if(index < 1024){
          btBuffer[index] = c;
          index++;
        }
      }
  }
}

void writeToSerial(){
        Serial.write(btBuffer);
        Serial.write('\n');
        Serial.write("number of characters is ");
        Serial.print(index);
        Serial.write('\n');

          LCD.clear();
           for(int i=0; i<index; i++){
              LCD.print(btBuffer[i]);
           }

        for(int i=0; i<1024; i++){
          btBuffer[i] = NULL;
        }
        index = 0;
       
}

