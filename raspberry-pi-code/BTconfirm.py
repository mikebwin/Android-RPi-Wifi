import RPi.GPIO as GPIO
import time

def btPasswordConfirm(verification):
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
            attempt = ''
            lastAttempt = time.time()
            while(True):
                for j in range(4):
                    GPIO.output(COL[j], 0)

                    for i in range(4):
                        if GPIO.input(ROW[i]) == 0:
                            print(MATRIX[i][j])
                            while(GPIO.input(ROW[i]) == 0):
                                pass
                            num = str(MATRIX[i][j])
                            attempt += num
                            if num == '*':
                                attempt = ''
                            if num == '#' or num == 'A' or num == 'B' or num == 'C' or num == 'D':
                                attempt = ''
                            if len(attempt) == len(verification):
                                return verification == attempt

                        if time.time() - lastAttempt >= 20:
                            return False

                    GPIO.output(COL[j], 1)
        except KeyboardInterrupt:
            print("Cleaning")
            GPIO.cleanup()
