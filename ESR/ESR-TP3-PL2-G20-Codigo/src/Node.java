import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Node {
    protected InetAddress ip; // identificador de cada nodo

    //Tabelas de encaminhamento e custos
    protected Map<InetAddress,String> routing_table = new HashMap<>(); // tabela de encaminhamento de cada nodo (Id nodo => Estado)
    protected Map<InetAddress,Integer> cost_table = new HashMap<>();
    protected InetAddress prev_node = null; // nodo adjacente anterior

    // SOCKETS
    protected DatagramSocket socketFlood;
    protected DatagramSocket socketActivate;
    protected DatagramSocket socketOverlay;
    protected DatagramSocket socketPing;
    protected DatagramSocket socketPingRouter;

    //PING Clientes
    protected Map<InetAddress,Integer> ping_table = new HashMap<>();
    protected ReentrantLock lockPing = new ReentrantLock();
    protected Condition condPing = lockPing.newCondition();

    //PING Routers
    protected Map<InetAddress,Integer> pingRouter_table = new HashMap<>();
    protected ReentrantLock lockPingRouter = new ReentrantLock();
    protected Condition condPingRouter = lockPingRouter.newCondition();

    //STREAM
    //RTP variables:
    DatagramPacket rcvdp; //UDP packet received from the server (to receive)
    DatagramSocket RTPsocket; //socket to be used to send and receive UDP packet
    static int RTP_RCV_PORT = 25000; //port where the client will receive the RTP packets

    Timer cTimer; //timer used to receive data from the UDP socket
    byte[] cBuf; //buffer used to store data received from the server
    //----------------

    //STREAM

    public Node() {
        //this.id = id;
    }

    public Node(InetAddress ipServer) throws IOException,ClassNotFoundException {

        InetAddress inetAddress = null;
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for(NetworkInterface netint : Collections.list(nets)) {
            if (!netint.isLoopback() || !netint.isUp()) {
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for(InetAddress inetA : Collections.list(inetAddresses)) {
                    if (inetA instanceof Inet4Address )
                        inetAddress = inetA;
                }
            }
        }

        System.out.println("Adress Escolhida: " + inetAddress);

        this.ip = inetAddress;

        this.socketFlood = new DatagramSocket(1234, this.ip);
        this.socketActivate = new DatagramSocket(5678, this.ip);
        this.socketOverlay = new DatagramSocket(4321, this.ip);
        this.socketPing = new DatagramSocket(8765, this.ip);
        this.socketPingRouter = new DatagramSocket(9546, this.ip);

        new Thread(() -> { // ipServer => IP do servidor, para criar a rede overlay
            try {
                createOverlay(ipServer);
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> { // criar uma thread que esteja a escuta de floods
            try {
                flood();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> { //criar função que esteja a escuta de ativações
            try {
                activateListener();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> { //criar função que esteja a escuta de pings cliente (cliente morreu)
            try {
                ping();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        //Router
        new Thread(() -> { //criar função que esteja a escuta de pings router (router morreu)
            try {
                pingRouter();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> { //criar função que envia pings router (router morreu)
            try {
                sendPingRouter();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(this::stream).start(); //
    }

    public void createOverlay(InetAddress ipServer) throws IOException, ClassNotFoundException, InterruptedException {

        InetAddress ip = this.ip; // Vê qual é o endereço ip do nodo => Se der problemas, ver mail do stor bruno em CC !!!!

        Packet p = new Packet(3,0,null);

        byte[] dataResponse = p.serialize();

        // Envia um pedido para o servidor (IP Servidor)
        DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, ipServer, 4321);
        socketOverlay.send(pktResponse);

        byte [] inData = new byte[1024];

        DatagramPacket receivePkt = new DatagramPacket(inData, inData.length);
        socketOverlay.receive(receivePkt);

        inData = receivePkt.getData();

        Packet recieve = new Packet(inData);

        //List<InetAddress> vizinhos = deserializeList(inData);

        for(InetAddress x: recieve.getVizinhos()){

            routing_table.put(x,"DESATIVADO");
            //ping_table.put(x,0); //caso ping para Cliente e Router
            System.out.println("vizinho " + x.toString());

            String msgNew = x.toString();

            System.out.println("Envia msg para " + msgNew);

            Packet pnew = new Packet(0,0,null);
            byte[] dataNew = pnew.serialize();

            DatagramPacket newNode = new DatagramPacket(dataNew, dataNew.length, x,4321);
            socketOverlay.send(newNode);

            Thread.sleep(50);
        }

        while(true){

            byte [] data = new byte[1024];

            System.out.println("Espera atualizacao na rede Overlay");
            DatagramPacket receiveOverlay = new DatagramPacket(data, data.length); // escuta q esta atenta a entrada de novos nodos na rede
            socketOverlay.receive(receiveOverlay);

            data = receiveOverlay.getData();
            Packet pReceive = new Packet(data);

            if(pReceive.getTipo()==0) {
                routing_table.put(receiveOverlay.getAddress(), "DESATIVADO");
            }
        }
    }

    public void flood() throws IOException, InterruptedException {

        byte [] inData = new byte[1024];

        DatagramPacket receivePkt = new DatagramPacket(inData, inData.length); // Recebe packet a dizer qual o custo

        while(true) {

	        System.out.println("Começar o flood");

            socketFlood.receive(receivePkt);
            Thread.sleep(50);

            byte[] data = receivePkt.getData();
            Packet p = new Packet(data);

            if(p.getTipo()==1){

                InetAddress origin = receivePkt.getAddress(); // Endereço IP que lhe enviou a mensagem

                int custo = p.getDados();

                System.out.println("Recebi do : " + origin + " com custo : " + custo);


                if (prev_node == null) { // 1ª iteração
                    prev_node = origin;
                    cost_table.put(origin, custo); // Guardar na tabela de custos qual a origem e o custo a partir dessa origem

                    System.out.println("1 ITERACAO: Vou enviar para os meus vizinhos com custo : " + custo);

                    //envia msg aos seus vizinhos
                    for (InetAddress inet : routing_table.keySet()) {
                        if (!inet.equals(prev_node)) {
                            Packet msg = new Packet(1,custo+1,null);
                            byte[] dataResponse = msg.serialize();
                            DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, inet, 1234);
                            socketFlood.send(pktResponse);
                        }
                    }

                } else { //Vezes seguintes a chegar ao nodo
                    int custo_anterior = cost_table.get(prev_node);
                    if (custo <= custo_anterior) { //Atualizar o antecessor
                        prev_node = origin;
                        System.out.println("OUTRAS ITERAÇÕES: Vou enviar para os meus vizinhos com custo : " + custo);

                        // envia msg aos seus vizinhos
                        for (InetAddress inet : routing_table.keySet()) {
                            if (!inet.equals(prev_node)) {
                                Packet msg1 = new Packet(1,custo+1,null);
                                byte[] dataResponse = msg1.serialize();
                                DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, inet, 1234);
                                socketFlood.send(pktResponse);
                            }
                        }
                    }

                    if (cost_table.containsKey(origin)) { // Atualização do valor
                        int custo_antigo_origem = cost_table.get(origin);
                        if (custo_antigo_origem >= custo) cost_table.put(origin, custo);
                    } else { // Inserção do valor
                        cost_table.put(origin, custo);
                    }
                }
            } else{
                System.out.println("Recebeu mensagem do tipo errado (Client - Tipo 1 unico tipo aceite)");
            }
        }

    }

    public void activateListener() throws IOException, ClassNotFoundException {

        byte [] inData = new byte[1024];

        DatagramPacket receivePkt = new DatagramPacket(inData, inData.length); // Recebe packet a dizer qual o custo

        while (true){
                socketActivate.receive(receivePkt);
                byte[] data = receivePkt.getData();
                Packet p = new Packet(data);

                if (p.getTipo() == 2) {

                    if(prev_node != null) {
                        InetAddress address = receivePkt.getAddress();
                        routing_table.put(address, "ATIVADO");
                        System.out.println("Nodo " + address + "foi ativado");

                        Packet msg = new Packet(2, 0,null);

                        byte[] dataResponse = msg.serialize();
                        DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, prev_node, 5678);
                        socketActivate.send(pktResponse);
                    }

                } else if (p.getTipo() == 5) {

                    // desativar nodo que mandou mensagem por timeout
                    InetAddress address = receivePkt.getAddress();
                    routing_table.put(address, "DESATIVADO");

                    System.out.println("Nodo " + address + "foi desativado");

                    // ver se ele se pode desativar a ele proprio - só envia mensagem para trás se todas as suas rotas estão desativadas

                    if (!routing_table.containsValue("ATIVADO")) { // Se a routing_table não contém nenhuma rota ativada

                        // propagar a mensagem aka enviar para previous dele
                        Packet msg = new Packet(5, 0,null);

                        byte[] dataResponse = msg.serialize();
                        DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, prev_node, 5678);
                        socketActivate.send(pktResponse);
                    }
                } else {
                    System.out.println("Recebeu mensagem do tipo errado (Client - Tipo 5 e 2 unicos tipos aceites)");
                }
        }
    }

    public void ping() throws IOException { //Nodo vai receber o ping dos clientes ou dos nodos

        byte [] inData = new byte[1024];

        new Ping(3,ping_table,lockPing,condPing); // verifica se um cliente morreu

        DatagramPacket receivePkt = new DatagramPacket(inData, inData.length); // Recebe packet a dizer qual o custo

        new Thread(() -> {  // Fica a escuta de clientes que morreram para atualizar routing_table
            try {
                lockPing.lock();

                while(true) {

                    while (!ping_table.containsValue(-1)) {
                        condPing.await();
                    }

                    InetAddress address = null;

                    for(InetAddress inet : ping_table.keySet()) {
                        if (ping_table.get(inet) == -1) address = inet;
                    }

                    ping_table.remove(address);
                    routing_table.put(address, "DESATIVADO");

                    System.out.println("Cliente " + address + "foi desativado");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lockPing.unlock();
            }
        }).start();

        while(true){

            try {
                socketPing.receive(receivePkt);
                socketPing.setSoTimeout(6000); // verifica se morreram todos os clientes
                System.out.println("RECEBI PING do " + receivePkt.getAddress());

                try {
                    lockPing.lock();

                    if (ping_table.containsKey(receivePkt.getAddress())) { // iterações seguintes

                        int ping = ping_table.get(receivePkt.getAddress());
                        ping_table.put(receivePkt.getAddress(), ping + 1);

                    } else { // 1º ping que recebe de um dado cliente

                        int max = 1;

                        for (InetAddress p : ping_table.keySet()) {
                            if(ping_table.get(p) > max){
                                max = ping_table.get(p);
                            }
                        }
                        ping_table.put(receivePkt.getAddress(), max);
                    }

                } finally {
                    lockPing.unlock();
                }

            } catch(SocketTimeoutException e) { // verificar se todas as tuas conexoes foram desativadas

                //colocar todas desativadas
                for (InetAddress address : routing_table.keySet()) {
                    routing_table.put(address, "DESATIVADO");
                }

                // eviar mensagem para o previous node a desativar a minha conexão
                Packet msg = new Packet(5, 0, null);
                socketPing.setSoTimeout(0); // verifica se morreram todos os clientes

                byte[] dataResponse = msg.serialize();
                DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, prev_node, 5678);
                socketActivate.send(pktResponse);
            }

        }
    }

    private void pingRouter() throws IOException {

        byte [] inData = new byte[1024];

        new Ping(3,pingRouter_table,lockPingRouter,condPingRouter); // verifica se um router morreu

        DatagramPacket receivePkt = new DatagramPacket(inData, inData.length); // Recebe packet a dizer qual o custo

        new Thread(() -> {  // Fica a escuta de routers que morreram para atualizar routing_table
            try {
                lockPingRouter.lock();

                while(true) {

                    while (!pingRouter_table.containsValue(-1)) {
                        condPingRouter.await();
                    }

                    InetAddress address = null;

                    for(InetAddress inet : pingRouter_table.keySet()) {
                        if (pingRouter_table.get(inet) == -1) address = inet;
                    }

                    pingRouter_table.remove(address);
                    routing_table.put(address, "DESATIVADO"); //Quando deixa de receber pings do router, desativa a sua ligação
                    //routing_table.remove(address);
                    cost_table.remove(address); // Se o nodo morreu, remove-lo da tabela de custos

                    if(prev_node.equals(address)) {

                        Map.Entry<InetAddress, Integer> min = null;
                        for (Map.Entry<InetAddress, Integer> entry : cost_table.entrySet()) {
                            if (min == null || min.getValue() > entry.getValue()) {
                                min = entry;
                            }
                        }

                        prev_node = min.getKey();

                        if (routing_table.containsValue("ATIVADO")) { //só envia mensagem de ativação caso possua alguma rota ativada
                            Packet msg = new Packet(2, 0,null);
                            byte[] dataResponse = msg.serialize(); // Envia mensagem com o custo 1 a todos

                            DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, prev_node, 5678);
                            socketActivate.send(pktResponse);
                        }

                    }

                    System.out.println("Router " + address + "foi removido");
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            } finally {
                lockPingRouter.unlock();
            }
        }).start();

        while(true){

            try {

                socketPingRouter.receive(receivePkt); // Está à escuta de pings de routers
                socketPingRouter.setSoTimeout(6000); // verifica se morreram todos os routers
                //System.out.println("RECEBI PING do " + receivePkt.getAddress());

                try {
                    lockPingRouter.lock();

                    // Adicionar pings à tabela de pings do router
                    if (pingRouter_table.containsKey(receivePkt.getAddress())) { // iterações seguintes

                        int ping = pingRouter_table.get(receivePkt.getAddress());
                        pingRouter_table.put(receivePkt.getAddress(), ping + 1);

                    } else { // 1º ping que recebe de um dado cliente

                        int max = 1;

                        for (InetAddress p : pingRouter_table.keySet()) {
                            if(pingRouter_table.get(p) > max){
                                max = pingRouter_table.get(p);
                            }
                        }
                        pingRouter_table.put(receivePkt.getAddress(), max);
                    }

                } finally {
                    lockPingRouter.unlock();
                }

            } catch(SocketTimeoutException e) { // verificar se todas as tuas conexoes foram desativadas
                // Mensagem de erro
                System.out.println("Todos os nodos (ligados ao router " + ip.toString() + " ) foram desconectados - Timeout ");
            }

        }
    }

    private void sendPingRouter() throws InterruptedException, IOException {

        while(true) {
            Thread.sleep(500); // FAZ PING PARA OS ROUTERS VIZINHOS DE 500 EM 500 milis

            for(InetAddress address : routing_table.keySet()) {

                Packet msg = new Packet(6, 0,null);
                byte[] dataResponse = msg.serialize(); // Envia mensagem com o custo 1 a todos
                //System.out.println("ENVIA PING ROUTER para " + address);

                DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, address, 9546);
                socketPingRouter.send(pktResponse);
            }
        }
    }

    public List<InetAddress> deserializeList(byte[] recBytes) throws IOException, ClassNotFoundException {
        ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(recBytes));
        List messageClass = (List) iStream.readObject();
        iStream.close();
        return messageClass;
    }

    //STREAM NODOS

    public void stream () {

        //init para a parte do cliente
        //--------------------------
        System.out.println("Stream ativada ");
        cTimer = new Timer(50, new Node.nodeTimerListener());
        cTimer.setInitialDelay(0);
        cTimer.setCoalesce(true);
        cBuf = new byte[15000]; //allocate enough memory for the buffer used to receive data from the server
        cTimer.start();

        try {
            // socket e video
            RTPsocket = new DatagramSocket(RTP_RCV_PORT); //init RTP socket (o mesmo para o cliente e servidor)
            RTPsocket.setSoTimeout(5000); // setimeout to 5s
        } catch (SocketException e) {
            System.out.println("Cliente: erro no socket: " + e.getMessage());
        }
    }

    //------------------------------------
    //Handler for timer (para cliente)
    //------------------------------------

    class nodeTimerListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            //Construct a DatagramPacket to receive data from the UDP socket
            rcvdp = new DatagramPacket(cBuf, cBuf.length);

            try{
                //receive the DP from the socket:
                //System.out.println("Espera por pacotes");
                Thread.sleep(5);
                RTPsocket.receive(rcvdp);
                System.out.println("Recebeu um pacote do ip " + rcvdp.getAddress());

                //create an RTPpacket object from the DP
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());

                //print important header fields of the RTP packet received:
                //System.out.println("Got RTP packet with SeqNum # "+rtp_packet.getsequencenumber()+" TimeStamp "+rtp_packet.gettimestamp()+" ms, of type "+rtp_packet.getpayloadtype());

                for(InetAddress inet : routing_table.keySet()) {
                    if (!inet.equals(prev_node)) {
                        if (routing_table.get(inet).equals("ATIVADO")) {
                            //get to total length of the full rtp packet to send
                            int packet_length = rtp_packet.getlength();
                            //System.out.println("A enviar Stream");

                            //retrieve the packet bitstream and store it in an array of bytes
                            byte[] packet_bits = new byte[packet_length];
                            rtp_packet.getpacket(packet_bits);

                            DatagramPacket senddp = new DatagramPacket(packet_bits, packet_length, inet, RTP_RCV_PORT);
                            RTPsocket.send(senddp);

                        }
                    }
                }


            }
            catch (InterruptedIOException iioe){
                System.out.println("Nothing to read");
            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
    }

}