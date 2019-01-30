#include <IRremote.h>

double temp = 0, tempExp = 0, tempInd = 11, instruction = 4;
int ledYellow = 13, ledRed = 12, PIN_RECV = 11, mode = 1;
IRrecv irrecv(PIN_RECV);
decode_results results;

inline void yellowOn() { digitalWrite(ledYellow, HIGH); digitalWrite(ledRed, LOW); }
inline void redOn() { digitalWrite(ledRed, HIGH); digitalWrite(ledYellow, LOW); }
inline void Off() { digitalWrite(ledRed, LOW); digitalWrite(ledYellow, LOW); }
inline void On() { digitalWrite(ledRed, HIGH); digitalWrite(ledYellow, HIGH); }

inline void gather() {
  int len = readInt();
  On();
  for (int i = 1; i <= len; i++) {
    delay(1000);
    int reading = analogRead(0);
    temp = reading * 0.0048828125 * 100;
    Serial.println(temp);
  }
//Serial.println(F("Information Gathered."));
  Off(); delay(500); 
  On(); delay(500); 
  Off(); delay(500); 
  On(); delay(500); Off();
}

inline int readInt() {
  int x = 0;
  char ch = '$';
  while (ch != '#') {
    ch = Serial.read();
    if ('0' <= ch && ch <= '9') { x *= 10; x += ch - '0'; }
  }
  return x;
}

void setup() {
  pinMode(ledYellow, OUTPUT);
  pinMode(ledRed, OUTPUT);
  irrecv.enableIRIn();
  Serial.begin(115200);
  gather();
//  tempInd = readInt();
}

void loop() {
   static unsigned long sensorStamp = 0; 
   
//   if (Serial.available()) { instruction = readInt(); }
   
   if (irrecv.decode(&results)) {
      unsigned long hexVal = results.value;
      if (hexVal == 16582903) { 
        Serial.println(hexVal, HEX);  
      } else if (hexVal == 16615543) {
        Serial.println(hexVal, HEX);
      }
     irrecv.resume();
    }
    
   if (millis() - sensorStamp > 1000) {
      sensorStamp = millis();
      int reading = analogRead(0);
      temp = reading * 0.0048828125 * 100;
      Serial.println(temp);
      instruction = readInt();
      /*
        interaction:
          == 1: cooling
          == 2: heating
          == 3: maintaining
          == 4: off
      */
      if (instruction == 1) { yellowOn(); }
      else if (instruction == 2) { redOn(); }
      else if (instruction == 3) { On(); }
      else { Off(); }
      if (instruction != 4) instruction = 3;
    }
}
