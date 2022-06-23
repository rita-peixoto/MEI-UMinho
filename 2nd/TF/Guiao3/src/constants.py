from random import randint

READ = 0
WRITE = 1
CAS = 2

HEARTBEAT_PERIOD = 120 / 1000.0


def TIME_OUT():
    return randint(150, 300) / 1000.0
