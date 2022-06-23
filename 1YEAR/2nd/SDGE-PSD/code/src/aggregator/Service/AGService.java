package Service;

import Common.NamedThreadFactory;
import Common.Query;
import DataLayer.DataInterface;
import org.zeromq.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static Common.Query.SearchType.GLOBAL;
import static Common.StaticUtilities.guard;
import static Common.StaticUtilities.lockWrapping;

public class AGService implements Runnable {

    private final ZMQ.Socket server;
    private final DataInterface data;
    private final ExecutorService executor;
    private final Lock sockLock = new ReentrantLock();

    public AGService(ZContext ctx, DataInterface data, String serviceAddr) {

        this.executor = Executors.newCachedThreadPool(new NamedThreadFactory("ClientHandler"));

        this.server = ctx.createSocket(SocketType.ROUTER);
        this.server.bind("tcp://" + serviceAddr);

        this.data = data;
    }


    public void receiveReq() {
        // msg[0] = identity ,msg[1] = 0, msg[2] = data
        ZMsg msg = ZMsg.recvMsg(server);
        executor.execute(() -> {
            msg.addLast(handleReq(Query.of(msg.removeLast().getData())));
            lockWrapping(() -> msg.send(server), sockLock);
            msg.destroy();
        });
    }

    public String handleReq(Query query) {
        switch (query.getQueryOp()) {
            case ACTIVE -> {
                if (query.is(GLOBAL))
                    return data.getActiveDevices().toString();
                else
                    return data.getLocalActiveDevices().toString();
            }
            case DEVICES -> {
                if (query.getArgument().isBlank()) {
                    return guard(query.is(GLOBAL), () -> data.getTypeCount().toString(), () -> data.getLocalTypeCount().toString());
                } else {
                    return guard(query.is(GLOBAL), query.getArgument(), t -> data.getTypeCount(t).toString(), t -> data.getLocalTypeCount(t).toString());
                }
            }
            case EVENT -> {
                if (query.getArgument().isBlank()) {
                    return guard(query.is(GLOBAL), () -> data.getEvents().toString(), () -> data.getLocalEvents().toString());
                } else {
                    return guard(query.is(GLOBAL), query.getArgument(), t -> data.getEventCount(t).toString(), t -> data.getLocalEventCount(t).toString());
                }
            }
            case ONLINE -> {
                return guard(query.is(GLOBAL), query.getArgument(), t -> data.isDeviceOnline(t).toString(), t -> data.isDeviceOnlineZone(t).toString());
            }
        }
        return "Invalid Querry";
    }

    @Override
    public void run() {
        System.out.println("Started AGService");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                receiveReq();
            }
        } catch (ZMQException e) {
            if (e.getErrorCode() != 4) {
                System.err.println("Critical Error in AGService involving ZMQ");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Critical Error in AGService");
            e.printStackTrace();
        } finally {
            executor.shutdownNow();
            server.close();
            System.out.println("Stopped AGService");
        }
    }
}
