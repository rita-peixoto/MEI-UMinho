import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;


import Query.*;

public class Client {

    /* to alter charachters color in terminal */
    public static final String TEXT_RED = "\u001B[31m";
    public static final String TEXT_BLACK = "\u001B[30m";
    public static final String TEXT_GREEN = "\u001B[32m";
    public static final String TEXT_BLUE = "\u001B[34m";
    public static final String TEXT_RESET = "\u001B[0m";
    public static final String TEXT_PURPLE = "\u001B[35m";
    public static final String TEXT_CYAN = "\u001B[36m";
    public static final String TEXT_YELLOW = "\u001B[33m";
    public static final String TEXT_WHITE = "\u001B[37m";

    ZMQ.Socket reqSock; // ZMQ REQ socket to send requestos to aggregator
    ZMQ.Socket notSock; // ZMQ SUB socket to (un)subscribe and receive subscripted msgs/notifications from aggregator

    /**
     * Notification receiver
     *  blocking thread that waits subscripted notification messages from aggregator
     *  and when received prints it in user terminal
     */
    public static class NotificationRecv implements Runnable {

        ZContext ctx;
        ZMQ.Socket notSock;

        /**
         * @param ctx client ZMW context
         * @param sock client notification socket
         */
        public NotificationRecv(ZContext ctx, ZMQ.Socket sock) {
            this.ctx = ctx;
            this.notSock = sock;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try{
                    ZMsg msg = ZMsg.recvMsg(this.notSock);        
                    System.out.println(TEXT_YELLOW + msg.getLast().toString() + TEXT_RESET);
                    msg.destroy();
                }catch (org.zeromq.ZMQException e){
                    break;
                }
            }
        }
    }

    /**
     * subscribe or unsubscribe client
     * @param tokens arguments
     * @param subscribe if subscribe = true then subscribe else unsubscribe
     */
    public void subscription(String[] tokens, boolean subscribe) {
        String subscription = "";

        switch (tokens.length < 1 ? "" : tokens[0]) {
            // 1 type - notify when there is no device with type online in zone
            case "1" -> subscription = tokens.length < 2 ? "" : tokens[1];
            // 2 (type) - notify when record was reached; if given type when record for number of devices with type else record for all devices
            case "2" -> subscription = tokens.length < 2 ? "record" : "record" + tokens[1];
            // 3 X - number of devices in zone increased X
            case "3" -> subscription = (tokens.length >= 2 && validRange(tokens[1])) ? "aumento" + tokens[1] : "";
            // 4 X - number of devices in zone decreased X 
            case "4" -> subscription = (tokens.length >= 2 && validRange(tokens[1])) ? "decrescimo" + tokens[1] : "";
        }

        if (subscription.equals(""))
            System.out.println(TEXT_RED + "Invalid." + TEXT_RESET);
        else
            if (subscribe) 
                this.notSock.subscribe(subscription.getBytes(ZMQ.CHARSET));
            else 
                this.notSock.unsubscribe(subscription.getBytes(ZMQ.CHARSET));
    }

    /**
     * returns true if string is in [0, 10, 20, ..., 100]
     * @param s value given by client
     * @return true if valid
     */
    public boolean validRange(String s){
        boolean res = false;
        try{
            int number = Integer.parseInt(s);
            res = (0 <= number && number <= 100) && (number % 10 == 0); 
        }
        catch (NumberFormatException ex){
            res = false;
        }
        return res;
    }

    /**
     * try parse input received from client and if successful sends it to aggregator
     * @param tokens tokens of string with clients input
     * @param pos global or zonal query type
     */
    public void sendQuery(String[] tokens, Query.SearchType pos) {
        Query query = null;

        // creates query
        switch (tokens.length < 1 ? "" : tokens[0]) {
            case "ONLINE" -> query = tokens.length < 2 ? null : new Query(pos, Query.QueryOp.ONLINE, tokens[1]);
            case "DEVICES" -> query = new Query(pos, Query.QueryOp.DEVICES, tokens.length > 1 ? tokens[1] : "");
            case "ACTIVE" -> query = new Query(pos, Query.QueryOp.ACTIVE,"");
            case "EVENT" -> query = new Query(pos, Query.QueryOp.EVENT, tokens.length > 1 ? tokens[1] : "");
        }

        if (query == null) // query wasnt created
            System.out.println(TEXT_RED + "Invalid." + TEXT_RESET);
        else {
            this.reqSock.send(query.toByteArray(), 0);
            System.out.println(this.reqSock.recvStr(0));
        }
    }

    /**
     * parses client input and if it is a valid query or (un)subscription sends it to agregator
     * @param line client input string
     *  Query ::= [G | Z] (ONLINE username | DEVICES [type]| ACTIVE | EVENT [type]) 
     *  (Un)Subscribe ::= (SUBSCRIBE | UNSUBSCRIBE) (1 type | 2 [type] | 3 X | 4 X)
     */
    public void execute(String line) {
        String[] tokens = line.split(" ");

        // separates types of subscriptions and queries according wiht first argument
        switch (tokens.length < 1 ? "" : tokens[0]) {
            // subscribe or unsubscribe 
            case "SUBSCRIBE" -> this.subscription(Arrays.copyOfRange(tokens, 1, tokens.length), true);
            case "UNSUBSCRIBE" -> this.subscription(Arrays.copyOfRange(tokens, 1, tokens.length), false);
            // global ("G") or zonal ("Z") query
            case "G" -> this.sendQuery(Arrays.copyOfRange(tokens, 1, tokens.length), Query.SearchType.GLOBAL);
            case "Z" -> this.sendQuery(Arrays.copyOfRange(tokens, 1, tokens.length), Query.SearchType.ZONAL);
            // empty input
            case "" -> {}
            // default is zonal query
            default -> this.sendQuery(tokens, Query.SearchType.ZONAL);
        }
    }

    // run loop
    public void run(String[] args) throws IOException {

        String initialInfo = "Type \"help\" for more information.\n";
        // help menu
        String help = """
                Subscriptions options:
                subscriptions - get possible subscriptions
                SUBSCRIBE notification [args] - to subscribe to notification whith args
                UNSUBSCRIBE notification [args] - to unsubscribe to notification whith args

                Possible query commands:
                ONLINE username - answers true if username is online else false
                DEVICES [type] - answers the number of devices with type online
                ACTIVE - answers the number of active dispositves\s
                EVENT [type] - answers the number of events of type
                When arguments is not given returns global state.
                For zonal or global search put Z or G before the command, the default is zonal search.
                """;
        // menu with possible subscriptions
        String subscriptions = """
                1 type - notify when there is no device with type online in zone
                2 [type] - notify when record was reached
                3 X - number of devices in zone increased to X %
                4 X - number of devices in zone decreased to X %
                """;

        // reads client input
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));

        // connect to aggregator
        try (ZContext ctx = new ZContext()) {

            this.reqSock = ctx.createSocket(SocketType.REQ);
            this.notSock = ctx.createSocket(SocketType.SUB);

            this.reqSock.connect(args.length > 1 ? args[1] : "tcp://*:8001");

            this.notSock.connect(args.length > 0 ? args[0] : "tcp://*:8003");

            NotificationRecv not = new NotificationRecv(ctx, this.notSock);
            Thread notThread = new Thread(not);
            notThread.start();

            String line = "";

            System.out.print(TEXT_BLUE + "CLIENT APP\n" + TEXT_RESET);
            System.out.print(initialInfo);

            while (!line.equals("quit")) {

                System.out.print(TEXT_CYAN + "> " + TEXT_RESET);
                if ((line = reader.readLine()) == null)
                    break;

                switch (line) {
                    case "help" -> System.out.print(help);
                    case "subscriptions" -> System.out.print(subscriptions);
                    default -> this.execute(line);
                }
            }

            notThread.interrupt();

            this.notSock.close();
            this.reqSock.close();

            System.out.println("bye.");

        } finally {
            System.exit(-1);
        }
    }

    /**
    * @param args args[0] notification server address; args[1] request server address
    */
    public static void main(String[] args) throws IOException {
        (new Client()).run(args);
    }

}
