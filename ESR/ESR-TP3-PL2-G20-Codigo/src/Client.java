import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.Enumeration;


public class Client extends Node {

    //STREAM
    //GUI
    //----
    JFrame f = new JFrame("Cliente de Testes");
    JButton setupButton = new JButton("Setup");
    JButton playButton = new JButton("Play");
    JButton pauseButton = new JButton("Pause");
    JButton tearButton = new JButton("Teardown");
    JPanel mainPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JLabel iconLabel = new JLabel();
    ImageIcon icon;

    public Client (InetAddress ipServer) throws IOException, ClassNotFoundException {

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
        this.socketPing = new DatagramSocket(8765, this.ip);

        new Thread(() -> { // ipServer => IP do servidor, para criar a rede overlay
            try {
                createOverlay(ipServer);
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                flood();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                pingClient();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                activate();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(this::stream).start();
    }

    private void pingClient() throws IOException, InterruptedException {

        while(true) {
            Thread.sleep(500);
            if(prev_node != null) { // ver eficiencia da solução
                Packet msg = new Packet(4, 0,null);
                byte[] dataResponse = msg.serialize(); // Envia mensagem com o custo 1 a todos
                //System.out.println("ENVIA PING para " + prev_node);

                DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, prev_node, 8765);
                socketActivate.send(pktResponse);
            }
        }
    }

    public void activate() throws IOException, InterruptedException {

        while(true) {
            Thread.sleep(500);
            if(prev_node != null) { // ver eficiencia da solução
                Packet msg = new Packet(2, 0,null);
                byte[] dataResponse = msg.serialize(); // Envia mensagem com o custo 1 a todos

                DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, prev_node, 5678);
                socketActivate.send(pktResponse);
                //break;
            }
        }
    }

    // STREAM

    //--------------------------
    //Constructor
    //--------------------------
    public void stream () {

        //build GUI
        //--------------------------

        //Frame
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        //Buttons
        buttonPanel.setLayout(new GridLayout(1,0));
        buttonPanel.add(setupButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(tearButton);

        // handlers... (so dois)
        playButton.addActionListener(new playButtonListener());
        tearButton.addActionListener(new tearButtonListener());

        //Image display label
        iconLabel.setIcon(null);

        //frame layout
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        iconLabel.setBounds(0,0,380,280);
        buttonPanel.setBounds(0,280,380,50);

        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.setSize(new Dimension(390,370));
        f.setVisible(true);

        //init para a parte do cliente
        //--------------------------
        cTimer = new Timer(20, new clientTimerListener());
        cTimer.setInitialDelay(0);
        cTimer.setCoalesce(true);
        cBuf = new byte[15000]; //allocate enough memory for the buffer used to receive data from the server

        try {
            // socket e video
            RTPsocket = new DatagramSocket(RTP_RCV_PORT); //init RTP socket (o mesmo para o cliente e servidor)
            RTPsocket.setSoTimeout(5000); // setimeout to 5s
        } catch (SocketException e) {
            System.out.println("Cliente: erro no socket: " + e.getMessage());
        }
    }

    //------------------------------------
    //Handler for buttons
    //------------------------------------

    //Handler for Play button
    //-----------------------
    class playButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){

            //System.out.println("Play Button pressed !");
            //start the timers ...
            cTimer.start();
        }
    }

    //Handler for tear button
    //-----------------------
    class tearButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){

            //System.out.println("Teardown Button pressed !");
            //stop the timer
            cTimer.stop();
            //exit
            System.exit(0);
        }
    }

    //------------------------------------
    //Handler for timer (para cliente)
    //------------------------------------

    class clientTimerListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            //Construct a DatagramPacket to receive data from the UDP socket
            rcvdp = new DatagramPacket(cBuf, cBuf.length);

            try{
                //receive the DP from the socket:
                Thread.sleep(5);
                RTPsocket.receive(rcvdp);

                //create an RTPpacket object from the DP
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());

                //print important header fields of the RTP packet received:
                //System.out.println("Got RTP packet with SeqNum # "+rtp_packet.getsequencenumber()+" TimeStamp "+rtp_packet.gettimestamp()+" ms, of type "+rtp_packet.getpayloadtype());

                //print header bitstream:
                //rtp_packet.printheader();

                //get the payload bitstream from the RTPpacket object
                int payload_length = rtp_packet.getpayload_length();
                byte [] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                //get an Image object from the payload bitstream
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Image image = toolkit.createImage(payload, 0, payload_length);

                //display the image as an ImageIcon object
                icon = new ImageIcon(image);
                iconLabel.setIcon(icon);
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