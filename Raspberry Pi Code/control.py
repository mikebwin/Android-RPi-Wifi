import RPi.GPIO as GPIO
import time
import base64
import requests
import pickle
from threading import Thread
from Passcodes import *#PermanentPasscode, OneTimePasscode, TemporaryPasscode, RepeatPasscode, IncorrectPasscode

URL = "http://ec2-3-95-178-64.compute-1.amazonaws.com"
waitTime = 5

# Optimally, this SHOULD NOT be stored in source code. Keep it in a
# .ini file and read from it, or use command line arguments
LOCK_ID = "-LbK-mwrfEHGLmZLybdY"
SECRET = "password1234"

class Controller:

    def __init__(self):
        self.passcodeLength = 4
        self.passcodes = []
        self.uses = []
        self.checkingBackendStatus = False
        #self.passcodes.append(PermanentPasscode('1234'))
        #self.passcodes.append(OneTimePasscode('5678'))
        #self.passcodes.append(TemporaryPasscode('1357', time.time(), time.time()+60))
        #self.passcodes.append(RepeatPasscode('2468', 0, (12+7)*3600+36*60, [False, False, False, True, False, False, True]))
        self.readPasscodes()
        self.readUses()
        oneCode = OneTimePasscode('5678')
        tempCode = TemporaryPasscode('1357', time.time(), time.time()+60)
        self.removePasscode(tempCode)
        self.addPasscode(tempCode)
        self.removePasscode(oneCode)
        self.addPasscode(oneCode)

    def readUses(self):
        file = open("Usage-data.dat", "rb")
        self.uses = pickle.load(file)
        file.close()

    def writeUses(self):
        file = open("Usage-data.dat", "wb")
        pickle.dump(self.uses, file)
        file.close()
        
    def readPasscodes(self):
        file = open("Passcode-data.dat", "rb")
        self.passcodes = pickle.load(file)
        file.close()

    def writePasscodes(self):
        file = open("Passcode-data.dat", "wb")
        pickle.dump(self.passcodes, file)
        file.close()

    def addPasscode(self, passcode):
        self.passcodes.append(passcode)
        self.writePasscodes()

    def removePasscode(self, passcode):
        self.passcodes.remove(passcode)
        self.writePasscodes()

    def changePasscode(self, oldPasscode, newPasscode):
        self.removePasscode(oldPasscode)
        self.addPasscode(newPasscode)
        self.writePasscodes()

    def updatePasscodes(self, newPasscodes):
        self.passcodes = newPasscodes
        self.writePasscodes()

    def clearPasscodes(self):
        self.passcodes = []
        self.writePasscodes()

    def lock(self):
        servoPIN = 23
        GPIO.setup(servoPIN, GPIO.OUT)

        p = GPIO.PWM(servoPIN, 50)   # GPIO 23 for PWM with 50Hz
        p.start(7.5)    # Initialization
        time.sleep(0.5)

        # 90 degrees
        p.ChangeDutyCycle(2.5)
        print(2.5)
        time.sleep(2)

        p.stop()

        self.writeBackendStatus("CLOSED")

    def unlock(self):
        servoPIN = 23
        GPIO.setup(servoPIN, GPIO.OUT)

        p = GPIO.PWM(servoPIN, 50)   # GPIO 23 for PWM with 50Hz
        p.start(2.5)    # Initialization
        time.sleep(0.5)

        # 0 degrees
        p.ChangeDutyCycle(7.5)
        print(7.5)
        time.sleep(2)

        p.stop()

        self.writeBackendStatus("OPEN")

    def writeBackendStatus(self, status):
        # Base 64 encode the lock id and secret
        user_pass = base64.b64encode(
            "{}:{}".format(LOCK_ID, SECRET).encode()).decode('ascii')
        response = requests.put(
            URL + '/api/v1/hardware/status',
            json={
                'status': status,
            },
            headers={
                'Authorization': 'Basic ' + user_pass,
            },
        )

        print(response)
        print(response.text)

    def checkBackendStatus(self):
        # Base 64 encode the lock id and secret
        user_pass = base64.b64encode(
            "{}:{}".format(LOCK_ID, SECRET).encode()).decode('ascii')
        response = requests.get(
            URL + '/api/v1/hardware/status',
            headers={
                'Authorization': 'Basic ' + user_pass,
            },
        )

        print(response)
        print(response.json())
        if response.status_code == 200:
            status = response.json()['status']
            if status == 'OPEN_REQUESTED':
                self.unlock()
                self.uses.append(Usage(PhoneUnlock(), time.time()))
                self.writeUses()

        self.checkingBackendStatus = False

    def waitForPasscode(self):
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
                                self.lock()
                                attempt = ''
                            if num == '#' or num == 'A' or num == 'B' or num == 'C' or num == 'D':
                                attempt = ''
                            if len(attempt) == self.passcodeLength:
                                thread = Thread(target = self.checkValid, args = [attempt])
                                thread.start()
                                #if self.isValid(attempt):
                                #    print("Unlock")
                                #    self.unlock()
                                attempt = ''
                            time.sleep(0.3)

                        if ~self.checkingBackendStatus and time.time() - lastAttempt >= waitTime:
                            lastAttempt = time.time()
                            self.checkingBackendStatus = True
                            thread = Thread(target = self.checkBackendStatus, args = [])
                            thread.start()

                    GPIO.output(COL[j], 1)
        except KeyboardInterrupt:
            print("Cleaning")
            GPIO.cleanup()

    def btPasswordConfirm(self, verification):
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
                                self.lock()
                                attempt = ''
                            if num == '#' or num == 'A' or num == 'B' or num == 'C' or num == 'D':
                                attempt = ''
                            if len(attempt) == self.passcodeLength:
                                return verification == attempt

                        if time.time() - lastAttempt >= 20:
                            return False

                    GPIO.output(COL[j], 1)
        except KeyboardInterrupt:
            print("Cleaning")
            GPIO.cleanup()

    def checkValid(self, attempt):
        for passcode in self.passcodes:
            if passcode.isActive(time.time()):
                if attempt == passcode.getPasscode():
                    passcode.use()
                    self.writePasscodes()
                    self.uses.append(Usage(passcode, time.time()))
                    self.writeUses()
                    
                    print("Unlock")
                    self.unlock()
                    #return True
        self.uses.append(Usage(IncorrectPasscode(attempt), time.time()))
        self.writeUses()
        #return False

class Usage:

    def __init__(self, passcode, timestamp):
        self.passcode = passcode
        self.timestamp = timestamp

    def getPasscode(self):
        return self.passcode

    def getTimestamp():
        return self.timestamp

if __name__ == "__main__":
    Controller().waitForPasscode()

