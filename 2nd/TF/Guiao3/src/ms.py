# Minimal support for Maelstrom node programs

import logging
from json import loads, dumps
from os import _exit
from sys import stdin
from types import SimpleNamespace as sn

msg_id = 0  # The id of the last msg taht was sent
base_msg_id = 0  # The oldest 'up to date' message id.


# Replaces the base mesage value of the message id with the current one
# Any old msg id will now be seen as outdated
def new_base():
    global base_msg_id, msg_id
    base_msg_id = msg_id


# Checks if the reply is refereing to an up-to-date request
# Messages which aren't replys will be seen as up-to-date and will return True
def up_to_date(reply):  # Verifica se a mensagem e atualizada
    try:
        return reply.body.in_reply_to > base_msg_id
    except AttributeError:
        return True


# Publishes a message labeling its origin as 'src', it's destination as 'dest' and it's body as 'body'
def send(src, dest, **body):
    global msg_id
    data = dumps(sn(dest=dest, src=src, body=sn(msg_id=(msg_id := msg_id + 1), **body)), default=vars)
    logging.debug("Sending %s", data)
    print(data, flush=True)


# Sends a reply with a general reply body
def reply(request, **body):
    send(request.dest, request.src, in_reply_to=request.body.msg_id, **body)


# Iterates over all incoming messages, as soon as tey are available
def receiveAll():
    while data := stdin.readline():
        logging.debug("Received %s", data.strip())
        yield loads(data, object_hook=lambda x: sn(**x))


def receive():
    data = stdin.readline()
    if data is not None:
        logging.debug("Received %s", data.strip())  # sao imprimidos no servidor web
        return loads(data, object_hook=lambda x: sn(**x))
    return data


def sendToNodes(src, nodes, **body):
    for node in nodes:
        send(src, node, **body)


def nop(msg):
    logging.debug(f"Ignoring {msg}")


def error11(msg):
    reply(msg, type="error", code=11, text="Unavailable")


def exitOnError(fn, *args):
    try:
        fn(*args)
    except:
        logging.critical("Fatal exception", exc_info=True)
        _exit(1)


def restartOnError(fn, *args):
    try:
        fn(*args)
    except:
        logging.critical("Fatal exception, restarting ...", exc_info=True)
        restartOnError(fn, *args)
