#!/usr/bin/env python3

# Simple 'lin-kv' workload in Python for Maelstrom

from math import ceil
from random import sample

from ms import *

logging.getLogger().setLevel(logging.DEBUG)

# Node and peer info
node_id = None  # The node id
node_ids = {}  # The peer's node ids

# Node Data
data = {}  # key -> (value, timestamp : int)
locked_keys = {}  # key -> Lock
requests = []  # Request Queue.

# Current request info (it should be an object tbh)
current = None
answer = None
quorum = set()
remaining = 0


class Lock:
    n_readers = 0

    def __init__(self, read=None, write=None):
        if read:
            self.n_readers += 1 if read is True else 0 if read is False else read
        elif write:
            self.n_readers = -1

    def acquire_read(self):
        if self.n_readers >= 0:
            self.n_readers += 1
            return True
        return False

    def acquire_write(self):
        if self.n_readers == 0:
            self.n_readers = -1
            return True
        return False

    def release(self):
        if self.n_readers > 0:
            self.n_readers -= 1
        else:
            self.n_readers = 0


def random_quorum(size):
    global node_ids
    return sample(node_ids, size)


def get_msg():
    global current, requests
    if current is None and requests:
        return requests.pop(0)
    else:
        return receive()


def clear():
    global current, remaining, quorum, answer
    new_base()
    current = None
    remaining = 0
    if len(quorum) > 0:
        logging.critical(f"LOCKED NODES WILL BE LOST {quorum}")
    quorum.clear()
    answer = None


def handle_linkv(msg):
    global current, remaining
    if current:  # Se já existir um pedido atual
        requests.append(msg)
    else:  # Caso não exista um current
        current = msg

        if current.body.type == 'read':
            quorum_r = random_quorum(ceil((len(node_ids) + 1) / 2))
            remaining = len(quorum_r)
            sendToNodes(node_id, quorum_r, type='lock_read', key=current.body.key)
        elif current.body.type in ['write', 'cas']:
            size = ceil((len(node_ids) + 1) / 2)
            quorum_w = random_quorum(size)
            remaining = len(quorum_w)
            sendToNodes(node_id, quorum_w, type='lock_write', key=current.body.key)


def handle(msg):
    global data, current, answer, remaining, quorum, locked_keys

    # -------- Server Side --------
    if msg.body.type in ['lock_read', 'lock_write']:
        key = msg.body.key

        if key in locked_keys:
            ret = False
            if msg.body.type == 'lock_read':
                ret = locked_keys[key].acquire_read()
            elif msg.body.type == 'lock_write':
                ret = locked_keys[key].acquire_write()
            if not ret:
                reply(msg, type='lock_fail', key=key)
        else:
            locked_keys[key] = Lock(read=msg.body.type == 'lock_read', write=msg.body.type == 'lock_write')
            data[key] = (None, 0)
        reply(msg, type='lock_ok', key=key, value=data[key])

    elif msg.body.type == 'release':
        locked_keys[msg.body.key].release()

    elif msg.body.type == 'do_write':
        data[msg.body.key] = msg.body.value
        locked_keys[msg.body.key].release()
        reply(msg, type="done")

    # ------ Middleware Side ------

    elif msg.body.type == 'lock_fail':
        if up_to_date(msg):
            for node in quorum:
                send(node_id, node, type='release', key=msg.body.key)
            quorum.clear()
            reply(current, type="error", code=11, text=f"Key {current.body.key} is unavailable.")
            clear()

    elif msg.body.type == 'lock_ok':
        if not up_to_date(msg):
            send(node_id, msg.src, type='release', key=msg.body.key)
        else:
            if answer is None or answer[1] < msg.body.value[1]:
                answer = msg.body.value
            quorum.add(msg.src)

            if (remaining := remaining - 1) <= 0:
                key = current.body.key
                type_ = current.body.type
                value, timestamp = answer or (None, 0)

                if type_ == 'read':
                    reply(current, type='read_ok', value=value)
                    sendToNodes(node_id, quorum, type='release', key=key)
                    clear()
                elif type_ == 'write':
                    new_value = current.body.value
                    sendToNodes(node_id, quorum, type='do_write', key=key, value=(new_value, timestamp + 1))
                    remaining = len(quorum)
                elif type_ == 'cas':
                    from_, to_ = getattr(current.body, 'from'), current.body.to
                    if answer is not None and value == from_:
                        sendToNodes(node_id, quorum, type='do_write', key=key, value=(to_, timestamp + 1))
                        remaining = len(quorum)
                    else:
                        sendToNodes(node_id, quorum, type='release', key=key)

                        if value is None:
                            reply(current, type='error', code=20, text=f"Key {key} is not registered")
                        else:
                            reply(current, type='error', code=22, text=f"CAS Fail {key}->{value} from {from_} to {to_}")
                        clear()
                quorum.clear()

    elif msg.body.type == "done":
        if up_to_date(msg) and (remaining := remaining - 1) <= 0:
            reply(current, type='cas_ok' if current.body.type == 'cas' else 'write_ok')
            quorum.clear()
            clear()

    else:
        logging.error(f'Unknown message type {msg.body.type}')


if __name__ == '__main__':
    while msg := get_msg():
        if msg.body.type == 'init':
            node_id = msg.body.node_id  # Nodo atual
            node_ids = msg.body.node_ids  # Todos os nodos ativos
            logging.info(f'Node {node_id} initialized {node_ids}')
            reply(msg, type='init_ok')
        elif msg.body.type in ['read', 'write', 'cas']:
            handle_linkv(msg)
        else:
            handle(msg)
