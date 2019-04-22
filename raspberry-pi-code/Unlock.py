import RPi.GPIO as GPIO
import time

servoPIN = 23
GPIO.setmode(GPIO.BCM)
GPIO.setup(servoPIN, GPIO.OUT)

p = GPIO.PWM(servoPIN, 50)   # GPIO 23 for PWM with 50Hz
p.start(2.5)    # Initialization
time.sleep(0.5)

# 0 degrees
p.ChangeDutyCycle(7.5)
print(7.5)
time.sleep(2)

p.stop()
GPIO.cleanup()
