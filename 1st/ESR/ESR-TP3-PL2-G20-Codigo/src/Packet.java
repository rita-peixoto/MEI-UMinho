import java.io.*;
import java.net.InetAddress;
import java.util.List;

public class Packet implements Serializable {
    private int tipo; // identifica o tipo da mensagem:  tipo 1 - flood | tipo 2 - activate | tipo 3 - overlay
    // | tipo 0 - atualização de overlay | tipo 4 - ping Cliente | tipo 5 - ping Cliente Prunning | tipo 6 - ping router send | 7 - envio de vizinhos
    private int dados; // dados do tipo custo a enviar: custo | 0 - activate
    private List<InetAddress> vizinhos; // vizinhos de um determinado nodo

    public Packet(byte[] recBytes) {

        try {
            Packet msg = deserialize(recBytes);
            this.dados = msg.getDados();
            this.tipo = msg.getTipo();
            this.vizinhos = msg.getVizinhos();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Packet(int tipo, int dados, List<InetAddress> vizinhos) {
        this.tipo = tipo;
        this.dados = dados;
        this.vizinhos = vizinhos;
    }

    public List<InetAddress> getVizinhos() {
        return vizinhos;
    }

    public int getTipo() {
        return tipo;
    }

    public int getDados() {
        return dados;
    }

    byte[] serialize() throws IOException {

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutput oo = new ObjectOutputStream(bStream);
        oo.writeObject(this);
        oo.close();
        byte[] serializedMessage = bStream.toByteArray();
        return serializedMessage;
    }

    public Packet deserialize(byte[] recBytes) throws IOException, ClassNotFoundException {
        ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(recBytes));
        Packet messageClass = (Packet) iStream.readObject();
        iStream.close();

        return messageClass;
    }
}