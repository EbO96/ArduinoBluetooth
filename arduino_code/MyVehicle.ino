#include <SoftwareSerial.h>
#define pwmMotorA 9 //Left wheel speed
#define pwmMotorB 10 //Right wheel speed
#define InMotorA1 4  
#define InMotorA2 3
#define InMotorB1 13
#define InMotorB2 8
#define STBY 7 
#define rx 0 //Bluetooth
#define tx 1 //Bluetooth
SoftwareSerial  bluetooth = SoftwareSerial(rx, tx);
short vehicleSpeed = 255; //PWM value

void setup() {
  bluetooth.begin(9600);
  pinMode(rx,INPUT);
  pinMode(tx, OUTPUT);
  pinMode(STBY, OUTPUT);
  digitalWrite(STBY, HIGH);
  pinMode(pwmMotorA, OUTPUT); 
  pinMode(InMotorA1, OUTPUT); 
  pinMode(InMotorA2, OUTPUT); 
  pinMode(pwmMotorB, OUTPUT); 
  pinMode(InMotorB1, OUTPUT);
  pinMode(InMotorB2, OUTPUT);
}

char myBuffer[4];
short counter = 0;
char vehicleMove = 's';

void loop() {
  //Move vehicle when bluetooth data are available
  if(bluetooth.available()){
    char c = (char)bluetooth.read(); //Read char value from bluetooth serial

    if(c != '\n'){
        myBuffer[counter] = c;
        counter++;
     }else{
        counter = 0;
        vehicleSpeed = 0;
        vehicleMove = myBuffer[0];
        
        if(myBuffer[1] != NULL && myBuffer[3] != NULL) {
          vehicleSpeed += ((myBuffer[1]-48)*100);
          myBuffer[1] = NULL;
        }
        if(myBuffer[2] != NULL) {
          vehicleSpeed += ((myBuffer[2]-48)*10);
          myBuffer[2] = NULL;
        }
        if(myBuffer[3] != NULL) {
          vehicleSpeed += (myBuffer[3]-48);
          myBuffer[3] = NULL;
        }
     
     }
    
    if(vehicleMove == 'f'){//Forward
      moveForward();
    }else if(vehicleMove == 'b'){//Back
      moveBack();
    }else if(vehicleMove == 'l'){//Left
      moveLeft();
    }else if(vehicleMove == 'r'){//Right
      moveRight();
    }else if(vehicleMove == 's'){//Stop
      stopVehicle();
    }
  }
}

void moveForward(){
  analogWrite(pwmMotorA,vehicleSpeed);
  digitalWrite(InMotorA1, LOW);
  digitalWrite(InMotorA2, HIGH);
  analogWrite(pwmMotorB,vehicleSpeed);
  digitalWrite(InMotorB1, LOW); 
  digitalWrite(InMotorB2, HIGH);
}

void moveBack(){
  analogWrite(pwmMotorA,vehicleSpeed);
  digitalWrite(InMotorA1, HIGH);
  digitalWrite(InMotorA2, LOW);
  analogWrite(pwmMotorB,vehicleSpeed);
  digitalWrite(InMotorB1, HIGH); 
  digitalWrite(InMotorB2, LOW);
}

void moveLeft(){
  analogWrite(pwmMotorA,vehicleSpeed);
  digitalWrite(InMotorA1, LOW);
  digitalWrite(InMotorA2, HIGH);
  analogWrite(pwmMotorB,vehicleSpeed);
  digitalWrite(InMotorB1, HIGH); 
  digitalWrite(InMotorB2, LOW);
}

void moveRight(){
  analogWrite(pwmMotorA,vehicleSpeed);
  digitalWrite(InMotorA1, HIGH);
  digitalWrite(InMotorA2, LOW);
  analogWrite(pwmMotorB,vehicleSpeed);
  digitalWrite(InMotorB1, LOW); 
  digitalWrite(InMotorB2, HIGH);
}

void stopVehicle(){
  analogWrite(pwmMotorA,0);
  analogWrite(pwmMotorB,0);
  digitalWrite(InMotorA1, HIGH);
  digitalWrite(InMotorA2, HIGH);
  digitalWrite(InMotorB1, HIGH); 
  digitalWrite(InMotorB2, HIGH);
}

