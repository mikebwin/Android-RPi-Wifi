import RPi.GPIO as GPIO
import time

GPIO.setmode(GPIO.BCM)

MATRIX = [
        [1,2,3,"A"],
        [4,5,6,"B"],
        [7,8,9,"C"],
        ["*",0,"#","D"]
]

ROW = [26,19,13,6] # BCM numbering
COL = [22,27,17,4] # BCM numbering

for j in range(4):
    GPIO.setup(COL[j], GPIO.OUT)
    GPIO.output(COL[j], 1)

for i in range(4):
    GPIO.setup(ROW[i], GPIO.IN, pull_up_down = GPIO.PUD_UP)

try:
    passcode = '1234'
    attempt = ''
    while(True):
        for j in range(4):
            GPIO.output(COL[j], 0)

            for i in range(4):
                if GPIO.input(ROW[i]) == 0:
                    print(MATRIX[i][j])
                    while(GPIO.input(ROW[i]) == 0):
                        pass
                    attempt += str(MATRIX[i][j])
                    if len(attempt) == len(passcode):
                        if attempt == passcode:
                            print("Unlock")
                            servoPIN = 23
                            GPIO.setup(servoPIN, GPIO.OUT)
                            
                            p = GPIO.PWM(servoPIN, 50)   # GPIO 23 for PWM with 50Hz
                            p.start(2.5)    # Initialization
                            time.sleep(0.5)

                            # 0 degrees
                            p.ChangeDutyCycle(7.5)
                            time.sleep(2)

                            p.stop()
                        else:
                            attempt = ''
                    time.sleep(0.3)

            GPIO.output(COL[j], 1)
except KeyboardInterrupt:
    GPIO.cleanup()

            
