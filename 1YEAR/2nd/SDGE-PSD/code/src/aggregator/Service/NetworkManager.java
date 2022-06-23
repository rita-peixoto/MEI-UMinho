package Service;

import Common.AGTag;
import Common.NamedThreadFactory;
import DataLayer.NetworkInterface;
import DataLayer.Snapshot;
import Service.Exceptions.ConnectException;
import Service.Exceptions.InvalidDestination;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.net.BindException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static Common.AGTag.*;
import static Common.StaticUtilities.*;

public class NetworkManager implements Runnable, Consumer<Collection<String>> {

    private static final String protocol = "tcp://";

    /**
     * The additional connections that are made to account for network instability
     */
    public static int C = 0;

    private final ZContext context;
    private final NetworkInterface netI;
    private final ZMQ.Socket pull;

    private final ExecutorService executor;

    private final String address;
    private final byte[] addressBin;
    private final Map<String, ZMQ.Socket> view;
    private final ReadWriteLock viewLock = new ReentrantReadWriteLock();


    public NetworkManager(ZContext context, NetworkInterface netI, String address) throws BindException {
        this.context = context;
        this.netI = netI;

        this.executor = Executors.newCachedThreadPool(new NamedThreadFactory("NetworkHandler"));

        this.address = address;
        this.addressBin = this.address.getBytes(ZMQ.CHARSET);
        this.view = new HashMap<>();

        // Pull Initialization
        this.pull = context.createSocket(SocketType.PULL);
        if (!pull.bind(protocol + address)) {
            throw new BindException("Pull socket couldn't bind to " + this.address);
        } else
            System.out.println("NETWORK: INFO: Network Manager bound to " + address);
    }


    /**
     * Contacts the given node and sends a subscription message
     *
     * @param contact The contact
     * @return True if successful, false otherwise
     */
    public boolean makeContact(String contact) {
        if (contact != null) {
            try (ZMQ.Socket sock = context.createSocket(SocketType.PUSH)) {
                if (sock.connect("tcp://" + contact)) {
                    sock.sendMore(this.addressBin);
                    sock.send(SUBSCRIBE.getBytes());
                    System.out.println("NETWORK: INFO: Contact successful with " + contact);
                    return true;
                }
            }
            System.out.println("NETWORK: ERROR: Failed to contact " + contact);
        }
        return false;
    }

    /**
     * Starts a bidirectional connection with the peer
     *
     * @param peer The peer to connect to
     * @throws ConnectException if the perr is unreachable
     */
    public void connectTo(String peer) throws ConnectException {
        ZMQ.Socket sock = context.createSocket(SocketType.PUSH);
        if (!sock.connect("tcp://" + peer)) {
            sock.close();
            throw new ConnectException("NETWORK: ERROR: Unable to connect to " + peer);
        }
        writeLockWrapping(() -> this.view.put(peer, sock), viewLock);
        send(peer, CONNECTION);
    }

    public void nodeDown(String node) {
        System.out.println("NETWORK: INFO: Node " + node + " is down, discarding..");
        writeLockWrapping(() -> this.view.remove(node), viewLock).close();
    }

    /**
     * Sends a message to the given node
     *
     * @param dest   the destination node
     * @param frames the message's frames
     * @throws InvalidDestination if the node 'dest' isn't registered localy
     */
    public void send(String dest, AGTag tag, byte[]... frames) {
        this.send(dest, tag, Arrays.asList(frames));
    }

    /**
     * Sends a message to the given node
     *
     * @param dest   the destination node
     * @param frames the message's frames
     * @throws InvalidDestination if the node 'dest' isn't registered localy
     */
    public void send(String dest, AGTag tag, List<byte[]> frames) {
        ZMQ.Socket destSock = readLockWrapping(() -> this.view.get(dest), viewLock);
        if (destSock == null) throw new InvalidDestination("Node " + dest + " isn't registered");

        List<byte[]> msg = new ArrayList<>(); // To avoid errors with read only lists
        msg.add(this.addressBin);
        msg.add(tag.getBytes());
        msg.addAll(frames);

        byte[] last = msg.remove(msg.size() - 1);

        try {
            boolean b = msg.stream().map(destSock::sendMore).reduce(true, Boolean::logicalAnd);
            b = b && destSock.send(last);
            if (!b)
                nodeDown(dest);
        } catch (ZMQException e) {
            nodeDown(dest);
        }
    }

    public List<byte[]> recv() {
        List<byte[]> res = new ArrayList<>();

        res.add(pull.recv());
        while (pull.hasReceiveMore()) res.add(pull.recv());

        return res;
    }

    /**
     * Selects a random elements from the view
     *
     * @param count The number os items to select
     * @return The selected items paired with how many times they were picked
     */
    public Map<String, Integer> sampleView(int count) {
        return sampleMultipleEvenly(count, readLockWrapping(view::keySet, viewLock));
    }

    /**
     * Handles CONNECTION messages
     *
     * @param msg The message
     */
    public void ConnHDLR(List<byte[]> msg) {
        String node = new String(msg.get(0));
        ZMQ.Socket sock = context.createSocket(SocketType.PUSH);
        if (sock.connect(protocol + node)) {
            ZMQ.Socket old = writeLockWrapping(() -> this.view.put(node, sock), viewLock);
            if (old != null) {
                System.out.println("NETWORK: WARNING: Migrated connection on node" + node);
                old.close();
            } else System.out.println("NETWORK: INFO: Connection established with incoming node " + node);
        } else {
            pull.disconnect(node);
            System.out.println("NETWORK: ERROR: Failed to establish connection with incoming node " + node);
        }
    }

    /**
     * Predicate that dictates if a subscription message should be accepted
     *
     * @param node The node that's trying to subscride
     * @return True if the node is accepted False otherwise
     */
    public boolean acceptSubscription(String node) {
        // We cannot subscribe to ourselves, we cannot accept multiple subscriptions from the same node
        if (node.equals(address) || this.view.containsKey(node)) return false;
        // We are forced to accept if we don't have anyone else to forward the message
        // Else the probability of staying is 1.0 / (1.0 + view.size())
        return view.isEmpty() || Math.random() < (1.0 / (1.0 + view.size()));
    }

    private void handleSubscription(byte[] nodeBin, int pathcount) {
        String node = new String(nodeBin);
        if (readLockWrapping(() -> acceptSubscription(node), viewLock)) {
            connectTo(node);
            pathcount--;
        }
        if (pathcount > 0) { // If there are paths remaining
            this.sampleView(pathcount).forEach((a, b) -> send(a, FWD_SUBSCRIPTION, nodeBin, intToBytes(b)));
        }
    }

    /**
     * Handles FWD_SUBSCRIPTION messages
     *
     * @param msg The message
     */
    public void FWDSubHDLR(List<byte[]> msg) {
        handleSubscription(msg.get(1), bytesToInt(msg.get(2)));
    }

    /**
     * Handles SUBSCRIBE messages
     *
     * @param msg The message
     */
    public void SubHDLR(List<byte[]> msg) {
        handleSubscription(msg.get(0), readLockWrapping(view::size, viewLock) + C);
    }

    /**
     * Handles UNSUBSCRIBE messages
     *
     * @param msg The message
     */
    public void UnsubHDLR(List<byte[]> msg) {
        String node = new String(msg.remove(0));
        writeLockWrapping(() -> view.remove(node), viewLock).close();
        netI.applySnapshot(Snapshot.deserialize(msg));
    }

    /**
     * Handles GOSSIP messages
     *
     * @param msg The message
     */
    public void GossipHDLR(List<byte[]> msg) {
        msg.remove(0);
        Snapshot s = Snapshot.deserialize(msg);
        netI.applySnapshot(s);
    }

    /**
     * Parses a message tag and foward's it to the respective handler
     *
     * @param msg The message
     */
    private void handle(List<byte[]> msg) {
        try {
            AGTag tag = fromBytes(msg.remove(1));
            System.out.println("\u001B[37mNETWORK: DEBUG: Received " + tag + "\u001B[0m");

            switch (tag) {
                case FWD_SUBSCRIPTION -> this.FWDSubHDLR(msg);
                case SUBSCRIBE -> this.SubHDLR(msg);
                case UNSUBSCRIBE -> this.UnsubHDLR(msg);
                case CONNECTION -> this.ConnHDLR(msg);
                case GOSSIP -> this.GossipHDLR(msg);
                default -> System.out.println("NETWORK: Ignoring invalid tag (" + tag + ')');
            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println("NETWORK: Ignoring invalid message");
        } catch (Exception e) {
            System.out.println("NETWORK: WELL SHIT");
            e.printStackTrace(System.err);
        }
    }

    public void shutdown() {
        Snapshot snapshot = netI.getNullifyingSnapshot();
        writeLockWrapping(() -> {
            var bs = snapshot.serialize();
            view.keySet().forEach(s -> send(s, UNSUBSCRIBE, bs));
            view.values().forEach(ZMQ.Socket::close);
            view.clear();
        }, viewLock);
        pull.close();
    }

    @Override
    public void run() {
        this.accept(null);
    }

    @Override
    public void accept(Collection<String> contacts) {
        System.out.println("Started NetworkManager");
        SnapshotHeartbeat snpHRT = new SnapshotHeartbeat(contacts);
        try {
            snpHRT.start();
            while (!Thread.currentThread().isInterrupted()) {
                List<byte[]> msg = recv();
                executor.execute(() -> handle(msg));
            }
        } catch (ZMQException e) {
            if (e.getErrorCode() != 4) {
                System.err.println("NETWORK:  Critical Error in NetworkManager involving ZMQ");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("NETWORK: Critical Error in NetworkManager");
            e.printStackTrace();
        } finally {
            executor.shutdownNow();
            snpHRT.destroy();
            shutdown();
            System.out.println("Stopped NetworkManager");
        }
    }

    public class SnapshotHeartbeat extends TimerTask {

        private final Timer timer;

        private final Collection<String> contacts;
        private boolean success, warned;
        private int round = Integer.MAX_VALUE;

        public SnapshotHeartbeat(Collection<String> contacts) {
            timer = new Timer();
            this.contacts = contacts;
            warned = true;
            success = false;
        }

        private boolean attemptContact() {
            if (contacts == null) return false;
            return makeContact(contacts.stream().findAny().orElse(null));
        }

        public void start() {
            timer.scheduleAtFixedRate(this, new Date(), 500);
        }

        public void destroy() {
            timer.cancel();
        }


        public void run() {
            try {
                if (readLockWrapping(view::isEmpty, viewLock)) {
                    if (!success) {
                        if (!warned) {
                            System.out.println("NETWORK: WARNING: This node has entered isolation");
                            warned = true;
                        }
                        if (attemptContact()) {
                            warned = false;
                            success = true;
                        }
                    }
                } else {
                    success = false;
                    Snapshot snapshot = netI.getSnapshot();
                    var bs = snapshot.serialize();

                    Set<String> s = readLockWrapping(view::keySet, viewLock);
                    s.forEach(n -> send(n, GOSSIP, bs));

                    if (round++ > 20) {
                        System.out.println("\u001B[96mNETWORK: DEBUG: PERIODIC SNAPSHOT DISPLAY:\n\u001B[37m" + snapshot + "\u001B[0m");
                        System.out.println("NETWORK: INFO: The current view is " + s);
                        round = 0;
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
