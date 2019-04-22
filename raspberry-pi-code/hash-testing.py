import bcrypt

password = '123456'
if isinstance(password, str):
    password = bytes(password, 'utf-8')
hashedpw = str(bcrypt.hashpw(password, bcrypt.gensalt()), 'utf-8')
attempt = '123466'
if isinstance(attempt, str):
    attempt = bytes(attempt, 'utf-8')
if isinstance(hashedpw, str):
    hashedpw = bytes(hashedpw, 'utf-8')

correct = bcrypt.checkpw(attempt, hashedpw)
print(correct)
