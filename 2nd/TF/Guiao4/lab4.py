#!/usr/bin/env python3

"""
Implementação da estratégia otimista de replicação

Autores:

    | Número   | Nome             |
    |----------|------------------|
    | PG46988  | Ana Rita Peixoto |
    | PG46525  | André Gonçalves  |
    | PG47238  | Henrique Neto    |
    | A80943   | Márcia Teixeira  |

"""

import logging
from asyncio import run
from types import SimpleNamespace as sn

from ams import send, receiveAll, reply, replyAbort
from db import DB

logging.getLogger().setLevel(logging.DEBUG)

db = DB()
node_id = ""
node_ids = []

abqueue = {}
abindex = 0
executed = {}
terminating = {}
WSet = set()
commitTS = -1


def broadcast(**body):
    global node_id, node_ids
    for n in node_ids:
        send(node_id, n, **body)


async def beginExecute(msg):
    ctx = await db.begin([k for op, k, v in msg.body.txn], msg.src + '-' + str(msg.body.msg_id))
    rs, wv, res = await db.execute(ctx, msg.body.txn)
    if rs is None and wv is None:  # If the execution was aborted
        replyAbort(msg)
        db.cleanup(ctx)
    else:
        msg_id = send(node_id, 'lin-tso', type='ts')
        executed[msg_id] = sn(msg=msg, ctx=ctx, rs=rs, wv=wv, res=res, commitTS=commitTS)
        db.cleanup(ctx)


async def termination(msg, ts):
    global commitTS, WSet
    aborted = True

    logging.debug(f"Termination State:\n"
                  f" -> ts = {ts}\n"
                  f" -> WSet - {WSet}\n"
                  f" -> readset = {msg.body.rs}\n"
                  f" -> writevalues = {msg.body.wv}\n"
                  f" -> msg.body.commitTs = {msg.body.commitTs}\n"
                  f" -> commitTS = {commitTS}")

    if msg.body.commitTs < commitTS:  # If the transaction is concurrent
        if WSet.isdisjoint(set(msg.body.rs)):
            WSet = WSet.union(set(map(lambda x: x[0], msg.body.wv)))
            aborted = False
    else:
        WSet = set(map(lambda x: x[0], msg.body.wv))
        aborted = False

    if aborted is False:  # If it was not aborted commit and update the commit timestamp
        logging.info(f"Applying transaction with ts {ts}")
        commitTS = ts
        await db.commit(msg.body.ctx, msg.body.wv)
    else:
        logging.info(f"Aborting transaction with ts {ts}")

    if msg.src == msg.dest:  # If it was sent by me
        # Respond to client
        originalmsg = terminating.pop(ts)

        if aborted:
            replyAbort(originalmsg)
        else:
            reply(originalmsg, type='txn_ok', txn=msg.body.res)


async def handle(msg):
    # State
    global db, node_id, node_ids
    global abqueue, abindex

    # Message handlers
    if msg.body.type == 'init':
        node_id = msg.body.node_id
        node_ids = msg.body.node_ids
        logging.info('Node %s initialized', node_id)

        reply(msg, type='init_ok')

    elif msg.body.type == 'txn':
        await beginExecute(msg)

    elif msg.body.type == 'ts_ok':
        execution = executed.pop(msg.body.in_reply_to)
        terminating[msg.body.ts] = execution.msg

        logging.info(f"Broadcasting transaction with ts {msg.body.ts}")

        broadcast(type='abdcast', ctx=execution.ctx, rs=execution.rs, wv=execution.wv, res=execution.res,
                  ts=msg.body.ts, commitTs=execution.commitTS)

    elif msg.body.type == 'abdcast':
        abqueue[msg.body.ts] = msg
        while abindex in abqueue:  # Messages are delivered to termination by the order defined with lin-ts
            msg = abqueue.pop(abindex)
            await termination(msg, msg.body.ts)
            abindex += 1

    else:
        logging.warning('Unknown message type %s', msg.body.type)


# Main loop
run(receiveAll(handle))
