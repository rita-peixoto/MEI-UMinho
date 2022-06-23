import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Server extends JFrame implements ActionListener {

    private Map<InetAddress, List<InetAddress>> bootstrapper; // criação de uma estrutura que armazene o ficheiro bootstrapper
    private List<InetAddress> important; // Lista com os nodos cruciais ao funcionamento da rede

    // NODOS QUE FAZEM PARTE DA REDE
    private List<InetAddress> nodos = new ArrayList<>(); //Vai guardar os nodos que fazem parte da rede
    private ReentrantLock lockNodos = new ReentrantLock(); // Lock para aceder a lista que guarda os nodos da rede
    private Condition condNodos = lockNodos.newCondition();

    // NODE
    private InetAddress ip; // identificador de cada nodo
    private Map<InetAddress,String> routing_table = new HashMap<>(); // tabela de encaminhamento de cada nodo (Id nodo => Estado)
    private Map<InetAddress,Integer> cost_table = new HashMap<>();
    private InetAddress prev_node = null; // nodo adjacente anterior
    private DatagramSocket socketFlood;
    private DatagramSocket socketActivate;
    private DatagramSocket socketOverlay;

    private ReentrantLock lock = new ReentrantLock(); //Lock para proteger o acesso à tabela de routing

    // STREAM
    //GUI:
    //----------------
    JLabel label;

    //RTP variables:
    //----------------
    DatagramPacket senddp; //UDP packet containing the video frames (to send)A
    DatagramSocket RTPsocket; //socket to be used to send and receive UDP packet
    int RTP_dest_port = 25000; //destination port for RTP packets
    InetAddress ClientIPAddr; //Client IP address

    static String VideoFileName; //video file to request to the server
    private Thread stream;
    private InetAddress addressStream;
    private boolean prunning = false;

    //Video constants:
    //------------------
    int imagenb = 0; //image nb of the image currently transmitted
    VideoStream video; //VideoStream object used to access video frames
    static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
    static int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
    static int VIDEO_LENGTH = 500; //length of the video in frames

    Timer sTimer; //timer used to send the images at the video frame rate
    byte[] sBuf; //buffer used to store the images to send to the client
    // STREAM

    // NODE
    public void activateListener() throws Exception {

        byte[] inData = new byte[1024];

        DatagramPacket receivePkt = new DatagramPacket(inData, inData.length); // Recebe packet a dizer qual o custo

        while (true) {

            socketActivate.receive(receivePkt);
            byte[] data = receivePkt.getData();
            Packet p = new Packet(data);

            if (p.getTipo() == 2) {
                InetAddress address = receivePkt.getAddress();

                try {
                    lock.lock();

                    if (routing_table.get(address).equals("DESATIVADO")) {
                        routing_table.put(address, "ATIVADO");

                        if(!prunning) {
                            prunning = true;
                            System.out.println("STREAM INICIADA!");
                            this.addressStream = address;
                            stream.start();

                        }
                    }
                } finally {
                    lock.unlock();
                }
                System.out.println("Nodo " + address + "foi ativado");
            } else if (p.getTipo() == 5) { // METER CONDIÇÃO EM Q TODAS AS ROTAS ESTAO DESATIVADAS

                // desativar nodo que mandou mensagem por timeout
                InetAddress address = receivePkt.getAddress();
                routing_table.put(address, "DESATIVADO");
                System.out.println("Nodo " + address + "foi desativado");

                } else {
                    System.out.println("Rcebeu mensagem do tipo errado (Server - Tipo 2 e 5 unicos tipos aceites)");
                }
            }
        }

    public void parseFile() throws IOException {

        // atualizar file location
        String path = "boostrapper.txt";

        BufferedReader reader = new BufferedReader(new FileReader(path));
        String allLines = reader.lines().collect(Collectors.joining());

        String [] nodos = allLines.split(";");

        bootstrapper = new HashMap<>();
        important = new ArrayList<>();

        for(String n : nodos) {
            String [] param = n.split(":");
            String ip_nodo = param[0];
            String [] ips_vizinhos = param[1].split(",");

            InetAddress inet_nodo;

            if (ip_nodo.contains("*")) {
                ip_nodo = ip_nodo.replace("*","");

                inet_nodo = InetAddress.getByName(ip_nodo);

                important.add(inet_nodo);
            } else inet_nodo = InetAddress.getByName(ip_nodo);

            List<InetAddress> vizinhos = new ArrayList<>();

            for(String s : ips_vizinhos) {
                InetAddress viz = InetAddress.getByName(s);
                vizinhos.add(viz);
            }

            bootstrapper.put(inet_nodo,vizinhos);
        }
        reader.close();
    }


    public Server() throws IOException {
        //System.out.println("Há Nodos por ler");

        parseFile();

        InetAddress inetAddress = null;
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for(NetworkInterface netint : Collections.list(nets)) {
            if (!netint.isLoopback() || !netint.isUp()) {
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();

                for(InetAddress inetA : Collections.list(inetAddresses) ) {
                    if (inetA instanceof Inet4Address)
                        inetAddress = inetA;
                }
            }
        }

        this.ip = inetAddress;

        this.socketFlood = new DatagramSocket(1234, this.ip);
        this.socketActivate = new DatagramSocket(5678, this.ip);
        this.socketOverlay = new DatagramSocket(4321, this.ip);

        // Thread Criacao do Overlay
        new Thread(() -> { //thread que se vai encarregar de receber novos nodos e de lhe dar os seus vizinhos
            try {
                startOverlay();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        // Thread para Flooding
        new Thread(() -> { //Thread encarregue de fazer flood para a rede para determinar as tabelas de encaminhamento
            try {
                    startFlood();
                } catch(IOException | InterruptedException e) {
                    e.printStackTrace();
                }
        }).start();

        new Thread(() -> { //Thread encarregue de fazer flood para a rede para determinar as tabelas de encaminhamento
            try {
                activateListener();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }).start();

        stream = new Thread(() -> {
            try {
                stream(this.addressStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    public void startOverlay() throws IOException {

        int r = 0;

        // Para o IP do servidor
        List<InetAddress> adj = bootstrapper.get(this.ip); // Vai buscar vizinhos do servidor

        for (InetAddress x : adj) { // Preenche a tabela de routing do servidor
            routing_table.put(x, "DESATIVADO"); // Inicialmente estão todas as tabelas desativadas
            //System.out.println("vizinho " + x.toString());
        }
        System.out.println("Leu Primeiro Nodo " + this.ip.toString());

        try {
            lockNodos.lock();
            nodos.add(this.ip); // Adiciona nodos que fazem parte da rede
        } finally {
            lockNodos.unlock();
        }

        while (true) { //server está à escuta de nodos atétodo o bootstrapper ser lido

            System.out.println("Leitura de Nodos na rede de Overlay");

            byte[] inData = new byte[1024];
            DatagramPacket receivePkt = new DatagramPacket(inData, inData.length); //fica bloqueado no receive até receber alguma mensagem
            socketOverlay.receive(receivePkt);

            parseFile();

            inData = receivePkt.getData(); //Transformar o pacote em bytes

            Packet p = new Packet(inData);
            InetAddress inet_nodo = receivePkt.getAddress();

            if (p.getTipo() == 3) {

                System.out.println("Leu Nodo " + inet_nodo);

                try {
                    lockNodos.lock();
                    nodos.add(inet_nodo);
                } finally {
                    lockNodos.unlock();
                }

                try {
                    lockNodos.lock();
                    if (nodos.containsAll(important) && r == 0) { // r = 0 para dizer que apenas faz o signal uma vez. Depois, ver cenário em que nodos importantes saem da rede !!!!!!
                        condNodos.signalAll(); // Acorda a thread adormecida quando todos os nodos importantes estiverem ativos
                        System.out.println("A Iniciar Flood");
                        r = 1;
                    }
                } finally {
                    lockNodos.unlock();
                }

                List<InetAddress> listaVizinhos = bootstrapper.get(inet_nodo);

                Packet send = new Packet(7,0, listaVizinhos);

                byte[] dataResponse = send.serialize(); // serializa VIZINHOS

                DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, inet_nodo, 4321);
                socketOverlay.send(pktResponse);
            } else{
                routing_table.put(inet_nodo, "DESATIVADO");
            }
        }
    }

    public void startFlood() throws IOException, InterruptedException {

        try {
            lockNodos.lock(); //Lock ativado para podermos aceder à lista de nodos

            //System.out.println("Thread vai dormir");

            while (!nodos.containsAll(important))
                condNodos.await();  //A thread fica adormecida enquanto não temos os nodos importantes

        } finally{
            lockNodos.unlock();
        }
        while (true) {
            Thread.sleep(50);
            System.out.println("Flood Iniciado");

            List<InetAddress> vizinhos = new ArrayList<>();

            vizinhos = bootstrapper.get(this.ip); // Vai buscar os seus vizinhos

            // Envia mensagem aos seus vizinhos
            for (InetAddress inet : vizinhos) {

                Packet msg = new Packet(1, 1,null);

                byte[] dataResponse = msg.serialize(); // Envia mensagem com o custo 1 a todos
                DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, inet, 1234);
                socketFlood.send(pktResponse);
            }
            Thread.sleep(20000);
        }
    }

    // serializa VIZINHOS
    byte[] serializeList(Object list) throws IOException {

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutput oo = new ObjectOutputStream(bStream);
        oo.writeObject(list);
        oo.close();

        byte[] serializedMessage = bStream.toByteArray();

        return serializedMessage;
    }

    //----------------------------------------------------------------------------------------
    // STREAM


    public void stream(InetAddress ip) throws Exception {
        VideoFileName = "movie.Mjpeg";
        System.out.println("Servidor: parametro não foi indicado. VideoFileName = " + VideoFileName);

        File f = new File(VideoFileName);
        if (f.exists()) {
            //Create a Main object
            Server s = new Server(ip);
            //show GUI: (opcional!)
            //s.pack();
            //s.setVisible(true);
        } else
            System.out.println("Ficheiro de video não existe: " + VideoFileName);

    }

    public Server(InetAddress ip) throws Exception {

        super("Servidor");

        // init para a parte do servidor
        sTimer = new Timer(FRAME_PERIOD, this); //init Timer para servidor
        sTimer.setInitialDelay(0);
        sTimer.setCoalesce(true);
        sBuf = new byte[15000]; //allocate memory for the sending buffer

        try {
            RTPsocket = new DatagramSocket(); //init RTP socket

            ClientIPAddr = ip;

            System.out.println("Servidor: socket " + ClientIPAddr);
            video = new VideoStream(VideoFileName); //init the VideoStream object:
            System.out.println("Servidor: vai enviar video da file " + VideoFileName);

        } catch (SocketException e) {
            System.out.println("Servidor: erro no socket: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Erro await");
        }

        //Handler to close the main window
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                //stop the timer and exit
                sTimer.stop();
                System.exit(0);
            }});

        //GUI:
        label = new JLabel("Send frame #        ", JLabel.CENTER);
        getContentPane().add(label, BorderLayout.CENTER);

        sTimer.start();
    }


    //------------------------
    //Handler for timer
    //------------------------
    public void actionPerformed(ActionEvent e) {

        //if the current image nb is less than the length of the video
        if (imagenb < VIDEO_LENGTH)
        {
            //update current imagenb
            imagenb++;

            try {
                //get next frame to send from the video, as well as its size
                int image_length = video.getnextframe(sBuf);

                //Builds an RTPpacket object containing the frame
                RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb*FRAME_PERIOD, sBuf, image_length);

                //get to total length of the full rtp packet to send
                int packet_length = rtp_packet.getlength();

                //retrieve the packet bitstream and store it in an array of bytes
                byte[] packet_bits = new byte[packet_length];
                rtp_packet.getpacket(packet_bits);

                //send the packet as a DatagramPacket over the UDP socket

                //System.out.println("Destino: " + ClientIPAddr);
                Thread.sleep(5);
                senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);
                RTPsocket.send(senddp);

                //System.out.println("Send frame #"+imagenb);
                //print the header bitstream
                //rtp_packet.printheader();

                //update GUI
                //label.setText("Send frame #" + imagenb);
            }
            catch(Exception ex)
            {
                System.out.println("Exception caught: "+ex);
                System.exit(0);
            }
        }
        else
        {
            imagenb = 0; //image nb of the image currently transmitted
            MJPEG_TYPE = 26; //RTP payload type for MJPEG video
            FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
            VIDEO_LENGTH = 500; //length of the video in frames

            //if we have reached the end of the video file, stop the timer
            System.out.println("Final da stream");
            sTimer.setInitialDelay(0);
            sTimer.setCoalesce(true);
            sBuf = new byte[15000];

            try {
                video = new VideoStream(VideoFileName); //init the VideoStream object:

            } catch (Exception exc) {
                System.out.println("Erro video");
            }

            sTimer.restart();
        }

    }
}





