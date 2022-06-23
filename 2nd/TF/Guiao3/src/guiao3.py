#!/usr/bin/env python3
import threading
import time
from collections import Counter

from constants import *
from ms import *

logging.getLogger().setLevel(10)
logging.addLevelName(5, 'DEBUG-EXTRA')

# Node and peer info
node_id = None  # The node id
node_ids = []  # All node ids
peer_ids = []  # The peer nodes
majority = 0  # servers majority

# State
# Persistent

# Node Data
data = {}

current_term = 0
voted_for = None
log = list()
log_lock = threading.Lock()

# Volatile
commit_index = 0
last_applied = 0

leader_id = None
is_leader = False

# Volatile on leader
next_index = dict()
match_index = dict()

# votes counter
votes = 0
ldr_event = threading.Event()


# ----------------------------- Init -----------------------------

def init(msg):
    global node_id, peer_ids, node_ids, leader_id, is_leader, majority

    node_id = msg.body.node_id  # Nodo atual
    node_ids = msg.body.node_ids  # Todos os nodos ativos
    peer_ids = [x for x in node_ids if x not in node_id]
    majority = len(node_ids) // 2 + 1

    logging.info(f'Node {node_id} initialized {node_ids}')
    logging.debug(f"Peers are {peer_ids}")

    reply(msg, type='init_ok')


# ---------------------------- Lin-kv ----------------------------

def handle_linkv(msg):
    if is_leader:
        append_entry((msg.src, msg.body.msg_id), ser_linkv(msg))
    else:
        error11(msg)


def ser_linkv(msg):
    return {
        'read': lambda x: (READ, x.key),
        'write': lambda x: (WRITE, (x.key, x.value)),
        'cas': lambda x: (CAS, (x.key, getattr(x, 'from'), x.to)),
    }[msg.body.type](msg.body)


def append_entry(origin, operation):
    log_lock.acquire()
    try:
        log.append((current_term, origin, operation))
    finally:
        log_lock.release()


def apply_entry(index):
    entry_term, origin, operation = log[index - 1]
    logging.log(5, f"Appling Entry {operation}")

    rpl = sn()
    src, rpl.in_reply_to = origin
    op, args = operation
    if op == WRITE:
        key, value = args
        rpl.type = 'write_ok'
        data[key] = value
    elif op == CAS:
        key, fro, to = args
        value = data.get(key, None)
        if fro is value:
            rpl.type = 'cas_ok'
            data[key] = to
        else:
            rpl.type = 'error'
            rpl.code, rpl.text = (20, 'Invalid Key') if value is None else (12, f"{fro} is not {value}")
    elif op == READ:
        rpl.type = 'read_ok'
        rpl.value = data.get(args, None)

    logging.log(5, f"NEW DATA -> {data}")

    if is_leader and entry_term == current_term:
        send(node_id, src, **vars(rpl))


# ------------------------ Append Entries ------------------------


def append_entries():
    global current_term, node_id, commit_index, next_index, log, peer_ids

    logging.log(5, "Enviando mensagens append_entries")

    log_lock.acquire()
    try:
        for node in peer_ids:
            logging.log(5, f"Envia append_entries para {node} com next_index {next_index[node]}")
            send(node_id, node,
                 type='append_entries',
                 term=current_term,
                 leader_id=node_id,
                 prev_log_index=next_index[node] - 1,
                 prev_log_term=log[next_index[node] - 2][0] if len(log) > 0 else 0,
                 entries=log[next_index[node] - 1:],
                 leader_commit=commit_index)
    finally:
        log_lock.release()


def append_entries_reply(msg):
    global commit_index, log, leader_id, voted_for, votes, is_leader, current_term, ldr_event

    if msg.body.term >= current_term:
        is_leader, leader_id = False, msg.body.leader_id
        current_term = msg.body.term
        ldr_event.set()

    success = msg.body.term >= current_term and msg.body.prev_log_index <= len(log) and (
            msg.body.prev_log_index == 0 or log[msg.body.prev_log_index - 1][0] == msg.body.prev_log_term)  # 1 2

    if len(msg.body.entries) > 0:
        if success:
            log = log[:msg.body.prev_log_index]
            for entry in msg.body.entries:  # 3. e 4. atualizar o log
                log.append(entry)

    if msg.body.leader_commit > commit_index:  # 5.
        update_state(min(msg.body.leader_commit, len(log)))

    reply(msg, type='append_entries_ok', success=success, term=current_term,
          last_entry=len(log) if success else max(msg.body.prev_log_index - 1, 0))


def update_state(new_commit_index):
    global commit_index, last_applied

    logging.log(5, "Update State")

    for entry_index in range(last_applied, new_commit_index):
        apply_entry(entry_index + 1)

    # Os match_index e next_index do lider tem que ser atualizados?

    commit_index = last_applied = new_commit_index


def append_entries_ok(msg):
    global commit_index, match_index, next_index, majority, current_term, is_leader, leader_id

    if msg.body.term > current_term:
        is_leader, leader_id = False, None
        current_term = msg.body.term
        return

    if msg.body.success:
        match_index[msg.src] = msg.body.last_entry

    next_index[msg.src] = msg.body.last_entry + 1

    counter = Counter(list(match_index.values()) + [len(log)])  # Conta qnts vezes aparece cada value do dicionario
    max_key = max(counter, key=int)  # Encontra a chave maior do dicionario
    count = counter[max_key]

    while count < majority:
        counter.pop(max_key)
        max_key = max(counter, key=int)
        count += counter[max_key]

    update_state(max_key)


# ------------------------ Leader Election ------------------------
def request_vote():
    global current_term, votes, is_leader, peer_ids, voted_for, leader_id
    is_leader = False
    leader_id = None

    current_term += 1  # ... increments its current term
    votes = 1  # ... votes for itself
    voted_for = node_id  # ... votes for itself

    for node in peer_ids:
        if len(log) > 0:
            lastLogTerm, _, _ = log[len(log) - 1]
        else:
            lastLogTerm = 0  # term of candidates last log entry

        send(node_id, node, type='request_vote', term=current_term, lastLogIndex=len(log), lastLogTerm=lastLogTerm)


def request_vote_reply(msg):
    global current_term, node_id, voted_for, ldr_event, votes, is_leader, log, leader_id

    logging.log(5, f"log = {log}")
    logging.log(5, f"current_term = {current_term}")
    logging.log(5, f"len(log) = {len(log)}")

    voteGranted = False

    # Os nodos sabem que comecou uma eleicao quando o term e maior do que o current term
    if msg.body.term > current_term:  # Se comecou uma nova eleicao
        is_leader, current_term = False, msg.body.term
        voted_for = leader_id = None
        ldr_event.clear()  # Old leader will ceise to be aknowlaged

    logging.log(5, f"voted_for = {voted_for}")

    if voted_for in [None, msg.src] and \
            msg.body.lastLogTerm >= (0 if len(log) == 0 else log[-1][0]) and msg.body.lastLogIndex >= len(log):
        voteGranted, voted_for = True, msg.src
        ldr_event.set()  # Acknowledge the voted canditate

    reply(msg, type='request_vote_ok', term=current_term, voteGranted=voteGranted)


def request_vote_ok(msg):
    global votes, leader_id, is_leader, majority, voted_for, next_index, match_index, current_term

    if msg.body.term > current_term:  # If I'm applying for an outdated term
        current_term, votes = msg.body.term, 0  # cancel the election
        voted_for = leader_id = None
        ldr_event.set()  # and start a timeout for the possible leader

    # Else if I was granted the vote for the term I am applying to
    elif msg.body.voteGranted and msg.body.term == current_term:
        votes += 1

        # If I won the election
        if votes >= majority:
            is_leader, leader_id = True, node_id
            votes = 0

            for node in peer_ids:
                next_index[node] = len(log) + 1
                match_index[node] = 0

            logging.log(5, "Sou Lider")

            threading.Thread(target=append_entries_heart).start()


# ---------------------------- Triggers ----------------------------

def start_timeout_job():
    timeout_thread = threading.Thread(target=election_timeout)
    timeout_thread.start()
    logging.debug("Timeout Job Submited")


# ---------------------------- Workers -----------------------------

def election_timeout():
    global ldr_event, is_leader, votes, voted_for

    while True:
        time.sleep(TIME_OUT())

        if not ldr_event.is_set() and not is_leader:  # Se nao recebeu append entries ou requests, e se nao for lider
            logging.log(5, "Election Started")
            request_vote()
        ldr_event.clear()


def append_entries_heart():
    global is_leader
    logging.debug("HeartBeat Started")
    while is_leader:
        append_entries()

        time.sleep(HEARTBEAT_PERIOD)


# ----------------------------- Main ------------------------------


# Basic structure
handlers = {
    'init': init,
    'read': handle_linkv,
    'write': handle_linkv,
    'cas': handle_linkv,
    'append_entries': append_entries_reply,
    'append_entries_ok': append_entries_ok,
    'request_vote': request_vote_reply,
    'request_vote_ok': request_vote_ok,
}


def main():
    global handlers
    start_timeout_job()

    for msg in receiveAll():
        try:
            if msg.body.type == 'request_vote_ok':
                handlers[msg.body.type](msg)
            else:
                handlers[msg.body.type](msg)
        except KeyError:
            logging.error(f"Invalid handler for {msg.body.type}")


if __name__ == '__main__':
    exitOnError(main)
