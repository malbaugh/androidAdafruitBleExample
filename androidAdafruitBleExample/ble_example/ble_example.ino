#include <Adafruit_BluefruitLE_SPI.h>
#include <Adafruit_BLEBattery.h>

// These are the pins for an Arduino UNO board. Change as needed
#define BLUEFRUIT_SPI_CS               8
#define BLUEFRUIT_SPI_IRQ              7
#define BLUEFRUIT_SPI_RST              4
#define BLUEFRUIT_SPI_SCK              13
#define BLUEFRUIT_SPI_MISO             12
#define BLUEFRUIT_SPI_MOSI             11

// SETUP ADAFRUIT BLUEFRUITLE SPI FRIEND
Adafruit_BluefruitLE_SPI ble(BLUEFRUIT_SPI_CS, BLUEFRUIT_SPI_IRQ, BLUEFRUIT_SPI_RST);

// A couple global variables to keep track of time
unsigned long StartTime;
unsigned long TimeReference;

int count = 0;

/* ACCELEROMETER SERVICE ITEMS
 * ----------------- */
int32_t accServiceId;
int32_t xAccCharId;
int32_t yAccCharId;
int32_t zAccCharId;
int32_t xGyroCharId;
int32_t yGyroCharId;
int32_t zGyroCharId;
int32_t accTimeCharId;

/*  These are just some fake values I created so you can get some values over 
 *  the connection. In reality you would setup the AGM and get data from the 
 *  AGM to send via BLE
 */
int AccX = 0;
int AccY = 0;
int AccZ = 0;
int GyroX = 0;
int GyroY = 0;
int GyroZ = 0;

/* BATTERY SERVICE ITEMS
 * ----------------- */
int BatteryLevel = 100;

/*  This sets up the battery service for us. 
 *  You could also set it up using the UUID 
 *  like I do acceleromter service. Check out the 
 *  bluefruit documentation for examples on 
 *  how to do this as setting up a standardized  
 *  service is slightly different than
 *  a custom 128 bit UUID.
 */
Adafruit_BLEBattery battery(ble);


/* Function: error
 * ---------------------------------
 * small helper function
 * 
 * returns: n/a - void. Error message is printed serially
 */
void error(const __FlashStringHelper*err) {
  Serial.println(err);
  while (1);
}


/* Function: updateIntCharacteristic
 * ---------------------------------
 * This function updates given integer characteristics and emitts
 * the updated values via the BLE module
 * 
 * nameOfChar = the name of the characteristic to be emitted as a String
 * characteristic = the value of the characteristic - must be an integer
 * serviceId = the id that this characteristic belongs to
 * 
 * returns: n/a - void
 */
void updateIntCharacteristic(String nameOfChar, int counter, int characteristic, int32_t charId) {

  Serial.print("Byte size of ");
  Serial.print(nameOfChar);
  Serial.print(" : ");
  Serial.println(sizeof(characteristic));

  String msg = String(characteristic) + "," + String(counter);
  
  ble.print( F("AT+GATTCHAR=") );
  ble.print( charId );
  ble.print( F(",") );
  ble.println(msg);

  Serial.print("Actual value of ");
  Serial.print(nameOfChar);
  Serial.print(" : ");
  Serial.println(characteristic);
  if ( !ble.waitForOK() ) Serial.println(F("Failed to get response!"));
  Serial.println("");
  Serial.println("");
}


/* Function: emittAccelerometerData
 * ---------------------------------
 * prepares accelerometer data and passes it along to 
 * be emitted by the BLE module
 * 
 * xAcc = the x axis accelerometer reading
 * yAcc = the y axis accelerometer reading
 * zAcc = the z axis accelerometer reading
 * xGyro = the x axis gyroscope reading
 * yGyro = the y axis gyroscope reading
 * zGyro = the z axis gyroscope reading
 * accTimeReference = the time reference for this accelerometer reading
 * 
 * returns: n/a - void
 */
void emittAccelerometerData(int xAcc, int yAcc, int zAcc, int xGyro, int yGyro, int zGyro, unsigned long accTimeReference, int counter) {
  updateIntCharacteristic("x acceleration", counter, xAcc, xAccCharId);
  updateIntCharacteristic("y acceleration", counter, yAcc, yAccCharId);
  updateIntCharacteristic("z acceleration", counter, zAcc, zAccCharId);
  updateIntCharacteristic("x gyroscope", counter, xGyro, xGyroCharId);
  updateIntCharacteristic("y gyroscope", counter, yGyro, yGyroCharId);
  updateIntCharacteristic("z gyroscope", counter, zGyro, zGyroCharId);
  updateIntCharacteristic("accelerometer time", counter, int(accTimeReference), accTimeCharId);
}


void setup() {
  // SETUP BLE
  delay(500);
  boolean success;
  Serial.begin(115200);
  if ( !ble.begin(false) ) error(F("Couldn't find Bluefruit, make sure it's in CoMmanD mode & check wiring?")); // Set to false for silent and true for debug
  if (! ble.factoryReset() ) error(F("Couldn't factory reset"));
  ble.echo(false);
  ble.info();
  if (! ble.sendCommandCheckOK(F("AT+GAPDEVNAME=BLE Arduino Hardware")) ) error(F("Could not set device name?"));

  // SETUP ACCELEROMETER SERVICE & CHARACTERISTICS
  success = ble.sendCommandWithIntReply( F("AT+GATTADDSERVICE=UUID128=00-00-00-01-62-7E-47-E5-A3-fC-DD-AB-D9-7A-A9-66"), &accServiceId);
  if (! success) error(F("Could not add accelerometer service"));
  success = ble.sendCommandWithIntReply( F("AT+GATTADDCHAR=UUID128=00-00-00-02-62-7E-47-E5-A3-fC-DD-AB-D9-7A-A9-66, PROPERTIES=0x2, MIN_LEN=1, MAX_LEN=20,VALUE=5,DATATYPE=1"), &xAccCharId);
  if (! success) error(F("Could not add x acc characteristic"));
  success = ble.sendCommandWithIntReply( F("AT+GATTADDCHAR=UUID128=00-00-00-03-62-7E-47-E5-A3-fC-DD-AB-D9-7A-A9-66, PROPERTIES=0x2, MIN_LEN=1, MAX_LEN=20,VALUE=5,DATATYPE=1"), &yAccCharId);
  if (! success) error(F("Could not add y acc characteristic"));
  success = ble.sendCommandWithIntReply( F("AT+GATTADDCHAR=UUID128=00-00-00-04-62-7E-47-E5-A3-fC-DD-AB-D9-7A-A9-66, PROPERTIES=0x2, MIN_LEN=1, MAX_LEN=20,VALUE=5,DATATYPE=1"), &zAccCharId);
  if (! success) error(F("Could not add z acc characteristic"));
  success = ble.sendCommandWithIntReply( F("AT+GATTADDCHAR=UUID128=00-00-00-05-62-7E-47-E5-A3-fC-DD-AB-D9-7A-A9-66, PROPERTIES=0x2, MIN_LEN=1, MAX_LEN=20,VALUE=5,DATATYPE=1"), &xGyroCharId);
  if (! success) error(F("Could not add x gyro characteristic"));
  success = ble.sendCommandWithIntReply( F("AT+GATTADDCHAR=UUID128=00-00-00-06-62-7E-47-E5-A3-fC-DD-AB-D9-7A-A9-66, PROPERTIES=0x2, MIN_LEN=1, MAX_LEN=20,VALUE=5,DATATYPE=1"), &yGyroCharId);
  if (! success) error(F("Could not add y gyro characteristic"));
  success = ble.sendCommandWithIntReply( F("AT+GATTADDCHAR=UUID128=00-00-00-07-62-7E-47-E5-A3-fC-DD-AB-D9-7A-A9-66, PROPERTIES=0x2, MIN_LEN=1, MAX_LEN=20,VALUE=5,DATATYPE=1"), &zGyroCharId);
  if (! success) error(F("Could not add z gyro characteristic"));
  success = ble.sendCommandWithIntReply( F("AT+GATTADDCHAR=UUID128=00-00-00-08-62-7E-47-E5-A3-fC-DD-AB-D9-7A-A9-66, PROPERTIES=0x2, MIN_LEN=1, MAX_LEN=20,VALUE=5,DATATYPE=1"), &accTimeCharId);
  if (! success) error(F("Could not add acc time characteristic"));

  // SETUP BATTERY SERVICE & CHARACTERISTICS
  battery.begin(true); // Note: this executes a ble.reset() automatically. If you don't use the battery.begin(true); command, you will need to use the ble.reset() command at the end. See the bluefruit documentation
  
  Serial.println();
}


void loop() {
  // Update the battery level measurement
  battery.update(BatteryLevel);

  StartTime = millis();
  TimeReference = millis();

  count = 0;
  // Imaginary BIS Sweep here...
  for (int i = 0; i < 5; i++) {
    TimeReference = millis();

    AccX = random(1, 100);
    AccY = random(1, 100);
    AccZ = random(1, 100);
    GyroX = random(1, 100);
    GyroY = random(1, 100);
    GyroZ = random(1, 100);
    emittAccelerometerData(AccX,AccY,AccZ,GyroX,GyroY,GyroZ, (TimeReference - StartTime),count);
    
    count++;
    Serial.println(count);
  }
  
  // Imaginary battery drain...
  BatteryLevel--;
  if (BatteryLevel == 0) {
    BatteryLevel = 100;
  }
}
