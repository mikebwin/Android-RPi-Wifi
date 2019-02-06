import os
import glob
import time
from bluetooth import *
import subprocess

os.system("sudo hciconfig hci0 piscan")
os.system("sudo hciconfig hci0 name smartbox_pi_demo")
os.system("sudo sdptool add --channel=1 SP")

# while True:
#	print(read_temp())	
#	time.sleep(1)


server_sock = BluetoothSocket(RFCOMM)
server_sock.bind(("", PORT_ANY))
server_sock.listen(1)

port = server_sock.getsockname()[1]

uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

advertise_service(server_sock, "smartlock_pi_demo",
                  service_id=uuid,
                  service_classes=[uuid, SERIAL_PORT_CLASS],
                  profiles=[SERIAL_PORT_PROFILE],
                  #                   protocols = [ OBEX_UUID ]
                  )
while True:
	print("Waiting for connection on RFCOMM channel %d" % port)

	client_sock, client_info = server_sock.accept()
	print("Accepted connection from ", client_info)

	try:
		data = client_sock.recv(1024)
		while data != "quit":
			if len(data) == 0:
				client_sock.send("empty string")

			#                print(data)
			#               cmd_array = data.split()

			# if statements here
			if data != "quit":
				print data
				cmd = data.split()
				out = subprocess.Popen(cmd,stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

				stdout, stderr = out.communicate()

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
