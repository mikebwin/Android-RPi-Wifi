import RPi.GPIO as GPIO
import time

servoPIN = 23
GPIO.setmode(GPIO.BCM)
GPIO.setup(servoPIN, GPIO.OUT)

p = GPIO.PWM(servoPIN, 50)   # GPIO 23 for PWM with 50Hz
p.start(7.5)    # Initialization
time.sleep(0.5)

# 90 degrees
p.ChangeDutyCycle(2.5)
print(2.5)
time.sleep(2)

p.stop()
GPIO.cleanup()
