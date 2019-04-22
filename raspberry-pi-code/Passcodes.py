import time

class PermanentPasscode:
    
    def __init__(self, passcode):
        self.passcode = passcode

    def getType(self):
        return "PERMANENT"

    def getPasscode(self):
        return self.passcode

    def isActive(self, time):
        return True

    def use(self):
        return

    def __eq__(self, other):
        return self.getType() == other.getType() and self.getPasscode() == other.getPasscode()

    def __ne__(self, other):
        return self != other
        
class OneTimePasscode:

    def __init__(self, passcode):
        self.passcode = passcode
        self.used = False

    def getType(self):
        return "ONETIME"

    def getPasscode(self):
        return self.passcode

    def isActive(self, time):
        return not self.used

    def use(self):
        self.used = True

    def __eq__(self, other):
        return self.getType() == other.getType() and self.getPasscode() == other.getPasscode()

    def __ne__(self, other):
        return self != other

class TemporaryPasscode:

    def __init__(self, passcode, startTime, endTime):
        self.passcode = passcode
        self.startTime = startTime
        self.endTime = endTime

    def getType(self):
        return "TEMPORARY"

    def getPasscode(self):
        return self.passcode

    def isActive(self, time):
        return time >= self.startTime and time <= self.endTime

    def use(self):
        return

    def __eq__(self, other):
        return self.getType() == other.getType() and self.getPasscode() == other.getPasscode()

    def __ne__(self, other):
        return self != other

class RepeatPasscode:

    def __init__(self, passcode, startTime, endTime, days):
        # startTime, endTime - time in seconds since midnight
        # days - a length 7 array of booleans indicating active days
        self.passcode = passcode
        self.startTime = startTime
        self.endTime = endTime
        self.days = days

    def getType(self):
        return "REPEAT"

    def getPasscode(self):
        return self.passcode

    def isActive(self, pTime):
        tTime = time.localtime(pTime)
        if self.days[tTime.tm_wday]:
            sTime = tTime.tm_hour * 3600 + tTime.tm_min * 60 + tTime.tm_sec
            if sTime >= self.startTime and sTime <= self.endTime:
                return True
        return False

    def use(self):
        return

    def __eq__(self, other):
        return self.getType() == other.getType() and self.getPasscode() == other.getPasscode()

    def __ne__(self, other):
        return self != other

class IncorrectPasscode:

    def __init__(self, passcode):
        self.passcode = passcode

    def getType(self):
        return "INCORRECT"

    def getPasscode(self):
        return self.passcode

    def isActive(self, time):
        return False

    def use(self):
        return

    def __eq__(self, other):
        return self.getType() == other.getType() and self.getPasscode() == other.getPasscode()

    def __ne__(self, other):
        return self != other

class PhoneUnlock:

    def __init(self):
        self.passcode = "0"

    def getType(self):
        return "PHONE"

    def getPasscode(self):
        return self.passcode

    def isActive(self, time):
        return False

    def use(self):
        return

    def __eq__(self, other):
        return self.getType() == other.getType() and self.getPasscode() == other.getPasscode()

    def __ne__(self, other):
        return self != other      
