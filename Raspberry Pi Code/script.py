import os
import glob
import time
from bluetooth import *
import subprocess

from control import Controller

os.system("sudo hciconfig hci0 piscan")
os.system("sudo hciconfig hci0 name smartbox_pi_demo")
os.system("sudo sdptool add --channel=1 SP")


server_sock = BluetoothSocket(RFCOMM)
server_sock.bind(("", PORT_ANY))
server_sock.listen(1)

port = server_sock.getsockname()[1]

uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

advertise_service(server_sock, "smartlock_pi_demo",
                  service_id=uuid,
                  service_classes=[uuid, SERIAL_PORT_CLASS],
                  profiles=[SERIAL_PORT_PROFILE]
                  )

connect_confirm = False
hardcoded_confirmation = "3025373"

while True:
    print("Waiting for connection on RFCOMM channel %d" % port)

    client_sock, client_info = server_sock.accept()

    confirmation = Controller.btPasswordConfirm(verification=hardcoded_confirmation)

    if confirmation:
        print("Accepted connection from ", client_info)

        try:
            data = client_sock.recv(1024)
            while data != "quit":
                if len(data) == 0:
                    client_sock.send("empty string")

                # if statements here
                if data != "quit":
                    print data
                    cmd = data.split()
                    out = None

                #this does a grep call to find SSID and authentication
                if data == "sudo iwlist wlan0 scanning":
                        out = subprocess.Popen(cmd,stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
                        print "WIFI LIST"
                        out = subprocess.Popen(['grep', '-E', "SSID|Authentication"], stdin=out.stdout, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

                # this will have to change - with whatever android sends for essid + password
                if "iwconfig wlan0 essid" in data:
                        print "WIFI INPUT"
                        # while loops and key index in case ESSID and password are multi words
                        key_index = cmd.index("key")
                        i = 3
                        essid = ""
                        while i < key_index:
                                essid += cmd[i]
                                if i != key_index - 1:
                                        essid += " "
                                i += 1
                        password = ""
                        i = key_index + 1
                        while i < len(cmd):
                                password += cmd[i]
                                if i != len(cmd)- 1:
                                        password += " "
                                i += 1
                        password = password.replace("s:", "")
                        new_cmd = "wpa_passphrase \"" + essid + "\" \"" + password + "\" | sudo tee -a /etc/wpa_supplicant/wpa_supplicant.conf > /dev/null"
                        out = subprocess.Popen(new_cmd, stdin=subprocess.PIPE, shell=True)
                        cmd = new_cmd.split()
                        out = subprocess.Popen("wpa_cli -i wlan0 reconfigure", stdin=subprocess.PIPE, shell=True)
                        out=None
                        stdout = "ok"


                #here we get what was printed to screen
                if out is not None:
                        stdout, stderr = out.communicate()


                #################################################################
                # below here for any cleaning you want to do with screen output #
                # before it is sent back to the android app                     #
                #################################################################


                # if it was a call to get the SSID and security, we'll parse through
                # and get it into a format we want on the Android app
                stdout1 = ""
                if data == "sudo iwlist wlan0 scanning":
                    #go through and delete all the spaces before ESSID and authentication
                    for line in stdout.splitlines():
                        essid_index = line.find("ESSID")
                        auth_index = line.find("Authentication")
                        if essid_index < 0:
                                stdout1 = stdout1 + line[auth_index:] + "\n"
                        else:
                                stdout1 = stdout1 + line[essid_index:] + "\n"
                    stdout = stdout1
                    stdout = stdout.replace("ESSID:", "E:")
                    stdout = stdout.replace("\"", "")
                    stdout = stdout.replace("Authentication Suites (1) :", "A:")
                    str_list = stdout.split("\n")
                    str_dict = dict()
                    index = 0
                    while index < len(str_list):
                        another = str_list[index]
                        if len(another) < 1 or another is None:
                            index += 1
                            continue
                        if index + 1 < len(str_list) and "A:" in str_list[index+1]:
                            authen = str_list[index+1]
                            str_dict.update({another:authen})
                            index += 1
                        else:
                            str_dict.update({another:"None"})
                        index += 1
                    stdout = ""
                    for key, value in str_dict.iteritems():
                        stdout = stdout + "\n" + key + "\n" + value


                    print stdout
                    client_sock.send(stdout)

                if data == "quit":
                    print "quitting"
                    break

                data = client_sock.recv(1024)

        except IOError:
            pass

        except KeyboardInterrupt:
            client_sock.close()
            server_sock.close()
            break
