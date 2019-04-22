# from the hardware device
import requests
import base64

URL = "http://ec2-3-95-178-64.compute-1.amazonaws.com"

# Optimally, this SHOULD NOT be stored in source code. Keep it in a
# .ini file and read from it, or use command line arguments
LOCK_ID = "-LbK-mwrfEHGLmZLybdY"
SECRET = "password1234"

# Base 64 encode the lock id and secret
user_pass = base64.b64encode(
    "{}:{}".format(LOCK_ID, SECRET).encode()).decode('ascii')
response = requests.get(
    URL + '/api/v1/hardware/status',
#    json={
#        'status': "CLOSED",
#    },
    headers={
        'Authorization': 'Basic ' + user_pass,
    },
)

print(response)
print(response.text)
print(response.json())
# Looks like {"status": "OPEN"}

if response.status_code == 200:
    status = response.json()['status']
    print(status)
