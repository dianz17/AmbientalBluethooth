
#include <SoftwareSerial.h>
#define rojo 13
#define amarillo 12
#define verde 11
#define buzzer 8
#define rx 2
#define tx 3
int mqport9 = A0;
int val = 9;
int limitesensor9y = 350;
int limitesensor9max=450;
SoftwareSerial btm(rx, tx); //  rx tx
int index = 0; 
char c;
char data[3];
boolean flag = false;

void setup() {
  // put your setup code here, to run once:
   pinMode(rojo, OUTPUT);
  pinMode(verde, OUTPUT);
  pinMode(amarillo, OUTPUT);
  pinMode(buzzer, OUTPUT);
  pinMode(rx, INPUT); 
  pinMode(tx, OUTPUT);
  btm.begin(9600); 
  Serial.begin(9600);   
   //Serial.println("Calentando Sensores");
   //delay(20000);
}

void loop() {
  // put your main code here, to run repeatedly:
  sensor();
  if(btm.available() > 0){ 
  btm.write(Serial.read());
    
    /*hile(btm.available() > 0){ 
          sensor();
          //c = btm.read(); 
          delay(10); //Delay required 
          //data[index] = c; 
          //index++; 
         // sensor();
     } 
     data[index] = '\0'; 
     flag = true;   
     
   }  
   if(flag){ 
     sensor(); 
     flag = false; 
     index = 0; 
     data[0]*/
}
}
void sensor(){ 
  int entradasensor9 = analogRead(mqport9);
  Serial.print("\nLectura de Monóxido de Carbono: ");
  Serial.print(entradasensor9);
  Serial.println(" ");
   btm.println(val);
  btm.println(entradasensor9);
  // Checks if it has reached the threshold value
  if (entradasensor9 > limitesensor9y && entradasensor9<limitesensor9max)
  {
    digitalWrite(rojo, LOW);
    digitalWrite(amarillo, HIGH);
    digitalWrite(verde, LOW);
    tone(buzzer, 500);
  }
  else if (entradasensor9 > limitesensor9max)
  {
    digitalWrite(rojo, HIGH);
    digitalWrite(amarillo, LOW);
    digitalWrite(verde, LOW);
    tone(buzzer, 1000);
  }
  else
  {
    digitalWrite(rojo, LOW);
    digitalWrite(verde, HIGH);
    digitalWrite(amarillo, LOW);
    noTone(buzzer);
  }
  delay(2000);

  /*
 char command = data[0]; 
 char inst = data[1]; 
 switch(command){
   case 'L':
         if(inst == 'Y'){ 
           digitalWrite(LED,HIGH); 
           btm.println("Light: ON"); 
         } 
         else if(inst == 'N'){ 
           digitalWrite(LED,LOW); 
           btm.println("Light: OFF"); 
         } 
   break; 
 } */
}
