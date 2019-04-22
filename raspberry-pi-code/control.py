import RPi.GPIO as GPIO
import time
import base64
import requests
import pickle
import bcrypt
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
        self.passcodeLength = 6
        self.passcodes = []
        self.otpasscodes = []
        self.uses = []
        self.checkingBackendStatus = False
        #self.readLockID()
        #self.passcodes.append(PermanentPasscode('1234'))
        #self.passcodes.append(OneTimePasscode('5678'))
        #self.passcodes.append(TemporaryPasscode('1357', time.time(), time.time()+60))
        #self.passcodes.append(RepeatPasscode('2468', 0, (12+7)*3600+36*60, [False, False, False, True, False, False, True]))
        self.readPasscodes()
        self.readUses()
        """self.addPasscode(PermanentPasscode(self.hash_passcode('123456')))
        oneCode = OneTimePasscode(self.hash_passcode('567890'))
        tempCode = TemporaryPasscode(self.hash_passcode('135791'), time.time(), time.time()+60)
        self.removePasscode(tempCode)
        self.addPasscode(tempCode)
        self.removePasscode(oneCode)
        self.addPasscode(oneCode)"""

    def hash_passcode(self, password):
        if isinstance(password, str):
            password = bytes(password, 'utf-8')
        hashedpw = str(bcrypt.hashpw(password, bcrypt.gensalt()), 'utf-8')
        return hashedpw
    
    def readLockID(self):
        file = open("LockID.dat", "rb")
        stuff = pickle.load(file)
        LOCK_ID = stuff[0]
        SECRET = stuff[1]
        file.close()

    def writeLockID(self):
        file = open("LockID.dat", "wb")
        stuff = [LOCK_ID, SECRET]
        pickle.dump(stuff, file)
        file.close()

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
        data = pickle.load(file)
        self.passcodes = data[0]
        self.otpasscodes = data[1]
        file.close()

    def writePasscodes(self):
        file = open("Passcode-data.dat", "wb")
        pickle.dump([self.passcodes, self.otpasscodes], file)
        file.close()

    def addPasscode(self, passcode):
        if passcode.getType() == 'ONETIME':
            self.otpasscodes.append(passcode)
        else:
            self.passcodes.append(passcode)
        self.writePasscodes()

    def removePasscode(self, passcode):
        if passcode in self.passcodes:
            self.passcodes.remove(passcode)
        elif passcode in self.otpasscodes:
            self.otpasscodes.remove(passcode)
        self.writePasscodes()

    def changePasscode(self, oldPasscode, newPasscode):
        self.removePasscode(oldPasscode)
        self.addPasscode(newPasscode)
        self.writePasscodes()

    def updatePasscodes(self, newPasscodes, newOTPasscodes):
        self.passcodes = newPasscodes
        self.otpasscodes = newOTPasscodes
        self.writePasscodes()

    def clearPasscodes(self):
        self.passcodes = []
        self.otpasscodes = []
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
        self.registerEvent('HARDWARE_LOCK_CLOSED')

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
        self.registerEvent('HARDWARE_LOCK_OPENED')

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
        #print(LOCK_ID)
        #print(SECRET)
        user_pass = base64.b64encode(
            "{}:{}".format(LOCK_ID, SECRET).encode()).decode('ascii')
        response = requests.get(
            URL + '/api/v1/hardware/status',
            headers={
                'Authorization': 'Basic ' + user_pass,
            },
        )

        print(response)
        if response.status_code == 200:
            status = response.json()['status']
            if status == 'OPEN_REQUESTED':
                self.unlock()
                self.uses.append(Usage(PhoneUnlock(), time.time()))
                self.writeUses()

        self.checkingBackendStatus = False

    def registerEvent(self, event):
        # Base 64 encode the lock id and secret
        user_pass = base64.b64encode(
            "{}:{}".format(LOCK_ID, SECRET).encode()).decode('ascii')
        response = requests.post(
            URL + '/api/v1/hardware/events',
            json={
                'event': event,
            },
            headers={
                'Authorization': 'Basic ' + user_pass,
            },
        )
        print(response)

    def getBackendPasswords(self):
         # Base 64 encode the lock id and secret
        #print(LOCK_ID)
        #print(SECRET)
        user_pass = base64.b64encode(
            "{}:{}".format(LOCK_ID, SECRET).encode()).decode('ascii')
        response = requests.get(
            URL + '/api/v1/hardware/sync',
            headers={
                'Authorization': 'Basic ' + user_pass,
            },
        )
        if response.status_code == 200:
            oldOTP = self.otpasscodes
            otp = response.json()['otp']
            unlimited = response.json()['permanent']

            self.clearPasscodes()

            for pw in otp:
                hashed = pw['hashedPassword']
                newPasscode = OneTimePasscode(hashed)
                if newPasscode not in oldOTP:
                    self.addPasscode(OneTimePasscode(hashed))
                else:
                    for oldPasscode in oldOTP:
                        if newPasscode == oldPasscode:
                            self.addPasscode(oldPasscode)
                            break
            for pw in unlimited:
                hashed = pw['hashedPassword']
                expiration = pw['expiration']
                if expiration != -1:
                    creation = pw['createdAt']
                    self.addPasscode(TemporaryPasscode(hashed, creation, expiration))
                else:
                    activeDays = pw['activeDays']
                    if activeDays == []:
                        self.addPasscode(PermanentPasscode(hashed))
                    else:
                        days = [False,False,False,False,False,False,False]
                        if 'MONDAY' in activeDays:
                            days[0] = True
                        if 'TUESDAY' in activeDays:
                            days[1] = True
                        if 'WEDNESDAY' in activeDays:
                            days[2] = True
                        if 'THURSDAY' in activeDays:
                            days[3] = True
                        if 'FRIDAY' in activeDays:
                            days[4] = True
                        if 'SATURDAY' in activeDays:
                            days[5] = True
                        if 'SUNDAY' in activeDays:
                            days[6] = True

                        activeTimes = pw['activeTimes']
                        if activeTimes == []:
                            self.addPasscode(RepeatPasscode(hashed, 0, 24*3600, days))
                        else:
                            hour1 = int(activeTimes[0][0:2])
                            minute1 = int(activeTimes[0][3:5])
                            hour2 = int(activeTimes[1][0:2])
                            minute2 = int(activeTimes[1][3:5])
                            startTime = hour1*3600 + minute1*60
                            endTime = hour2*3600 + minute2*60
                            self.addPasscode(RepeatPasscode(hashed, startTime, endTime, days))
                

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

                            thread2 = Thread(target = self.getBackendPasswords, args = [])
                            thread2.start()

                    GPIO.output(COL[j], 1)
        except KeyboardInterrupt:
            print("Cleaning")
            GPIO.cleanup()

    def checkValid(self, attempt):
        for passcode in self.passcodes:
            if passcode.isActive(time.time()):
                if self.validateAttempt(passcode.getPasscode(), attempt):
                    passcode.use()
                    self.writePasscodes()
                    self.uses.append(Usage(passcode, time.time()))
                    self.writeUses()
                    
                    print("Unlock")
                    self.unlock()
                    return
        for passcode in self.otpasscodes:
            if passcode.isActive(time.time()):
                if self.validateAttempt(passcode.getPasscode(), attempt):
                    passcode.use()
                    self.writePasscodes()
                    self.uses.append(Usage(passcode, time.time()))
                    self.writeUses()
                    
                    print("Unlock")
                    self.unlock()
                    return
        self.uses.append(Usage(IncorrectPasscode(attempt), time.time()))
        self.writeUses()
        return

    def validateAttempt(self, hashed, attempt):
        if isinstance(attempt, str):
            attempt = bytes(attempt, 'utf-8')
        if isinstance(hashed, str):
            hashed = bytes(hashed, 'utf-8')
        return bcrypt.checkpw(attempt, hashed)

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

