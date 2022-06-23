package Entidades;

import DAO.Dados;
import java.util.*;

public class Aposta implements Dados<Aposta> {

    private int idAposta;
    private float quantia;
    private float oddFixa;
    private int idEvento;

    public Aposta(float quantia, float oddFixa, int idEvento, int idAposta) {
        this.quantia = quantia;
        this.oddFixa = oddFixa;
        this.idEvento = idEvento;
        this.idAposta = idAposta;
    }

    public Aposta() {

    }

    public Aposta(List<String> l) {
        this.idAposta = Integer.parseInt(l.get(0));
        this.quantia = Float.parseFloat(l.get(1));
        this.oddFixa = Float.parseFloat(l.get(2));
        this.idEvento = Integer.parseInt(l.get(3));
    }

    public float getOddFixa() {
        return oddFixa;
    }

    public void setOddFixa(float oddFixa) {
        this.oddFixa = oddFixa;
    }

    public int getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(int idEvento) {
        this.idEvento = idEvento;
    }

    public int getIdAposta() {
        return idAposta;
    }

    public void setIdAposta(int idAposta) {
        this.idAposta = idAposta;
    }

    public float getQuantia() {
        return quantia;
    }

    public void setQuantia(float quantia) {
        this.quantia = quantia;
    }

    @Override
    public String toString() {
        return "Entidades.Aposta{" +
                "quantia=" + quantia +
                ", oddFixa=" + oddFixa +
                ", idEvento=" + idEvento +
                ", idAposta=" + idAposta +
                '}';
    }

    public Dados<Aposta> fromRow(final List<String> l) {
        return new Aposta(l);
    }

    public List<String> toRow() {
        List<String> r = new ArrayList<>();
        r.add(String.valueOf(this.idAposta));
        r.add(String.valueOf(this.quantia));
        r.add(String.valueOf(this.oddFixa));
        r.add(String.valueOf(this.idEvento));
        return r;
    }
}
