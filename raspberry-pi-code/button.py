from gpiozero import LED, Button
from time import sleep

led = LED(18)
button = Button(25)

button.wait_for_press()
led.on()
sleep(1)
led.off()
