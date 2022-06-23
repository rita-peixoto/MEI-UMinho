import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

public class Ott {

    public static void main(String args[]) throws IOException {

        String opcao = args[0];

        if (opcao.compareTo("S") == 0) { // Flag que indica Servidor
            System.out.println("-------- SERVIDOR --------");

            Server s = new Server(); // criação do servidor

        } else if (opcao.compareTo("C") == 0) { // Flag que indica Cliente
            String ipServer = args[1];
            System.out.println("-------- CLIENTE --------");

            try{
                Client c = new Client(InetAddress.getByName(ipServer)); // criação do cliente
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        } else { // Caso não haja uma flag (apenas corre um nodo overlay)

            System.out.println("-------- NODO --------");
            String ipServer = args[0];
            try {
                System.out.println(InetAddress.getByName(ipServer));
                Node n = new Node(InetAddress.getByName(ipServer));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}