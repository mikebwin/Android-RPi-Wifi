from gpiozero import LED
from time import sleep

led = LED(18)
led.on()
sleep(1)
led.off()
