package Service;

import DataLayer.DataInterface;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

public class CollectorSink implements Runnable {

    private final ZMQ.Socket collSock;
    private final DataInterface data;


    public CollectorSink(ZContext ctx, DataInterface data, String collAddr) {
        this.collSock = ctx.createSocket(SocketType.PULL);
        this.collSock.bind("tcp://" + collAddr);

        this.data = data;
    }


    @Override
    public void run() {
        System.out.println("Started CollectorSink");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String str = this.collSock.recvStr();
                String[] tokens = str.split(":");

                String state = tokens[0]; // ACTIVE; IDLE; ONLINE; DISCONNECTED; LOST
                if (tokens.length > 1) {
                    String username = tokens[1];
                    String type = tokens[2];

                    switch (state) {
                        case "ONLINE" -> {
                            data.addOnlineDevice(username, type);
                            data.addActiveUser();
                        }
                        case "ACTIVE" -> data.addActiveUser();
                        case "IDLE" -> data.removeActiveUser();
                        case "DISCONNECTED", "LOST" -> {
                            data.removeOnlineDevice(username, type);
                            data.removeActiveUser();
                        }
                    }
                } else {
                    // writes in data
                    data.addEvent(tokens[0]);
                }
            }
        } catch (ZMQException e) {
            if (e.getErrorCode() != 4) {
                System.err.println("Critical Error in CollectorSink involving ZMQ");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Critical Error in CollectorSink");
            e.printStackTrace();
        } finally {
            collSock.close();
            System.out.println("Stopped CollectorSink");
        }
    }
}
