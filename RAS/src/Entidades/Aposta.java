package Entidades;

import DAO.Dados;
import DAO.ResultadoPossivelDAO;

import java.util.*;

public class Aposta implements Dados<Aposta> {

    private String idAposta;
    private float quantia;
    private float oddFixa;
    private String idResultado;
    private String idApostador;
    private ResultadoPossivelDAO resultadosPossiveis;

    public Aposta(float quantia, float oddFixa, String idResultado, String idAposta, String idA) {
        this.quantia = quantia;
        this.oddFixa = oddFixa;
        this.idResultado = idResultado;
        this.idAposta = idAposta;
        this.idApostador = idA;
        this.resultadosPossiveis = ResultadoPossivelDAO.getInstance();
    }

    public Aposta() {

    }

    public Aposta(List<String> l) {
        this.idAposta = l.get(0);
        this.quantia = Float.parseFloat(l.get(1));
        this.oddFixa = Float.parseFloat(l.get(2));
        this.idResultado = l.get(3);
        this.idApostador = l.get(4);
        this.resultadosPossiveis = ResultadoPossivelDAO.getInstance();
    }

    public ResultadoPossivelDAO getResultadosPossiveis() {
        return resultadosPossiveis;
    }

    public float getOddFixa() {
        return oddFixa;
    }

    public void setOddFixa(float oddFixa) {
        this.oddFixa = oddFixa;
    }

    public String getIdResultado() {
        return idResultado;
    }

    public void setIdResultado(String idResultado) {
        this.idResultado = idResultado;
    }

    public String getIdAposta() {
        return idAposta;
    }

    public void setIdAposta(String idAposta) {
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
                ", idResultado=" + idResultado +
                ", idAposta=" + idAposta +
                '}';
    }

    public Dados<Aposta> fromRow(final List<String> l) {
        return new Aposta(l);
    }

    public List<String> toRow() {
        List<String> r = new ArrayList<>();
        r.add(this.idAposta);
        r.add(String.valueOf(this.quantia));
        r.add(String.valueOf(this.oddFixa));
        r.add(this.idResultado);
        r.add(this.idApostador);
        return r;
    }
}
