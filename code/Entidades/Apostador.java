package Entidades;

import DAO.Dados;
import java.util.*;

public class Apostador implements Dados<Apostador> {

    private String email;
    private String password;
    private String username;
    private Saldo saldo;
    private String idApostador;
    private boolean sessaoIniciada;

    public Apostador(String email, String password, String username, String idApostador) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.idApostador = idApostador;
        this.sessaoIniciada = false;
        this.saldo = new Saldo();
    }

    public Apostador() {

    }

    public Apostador(List<String> l) {
        this.idApostador = l.get(0);
        this.email = l.get(1);
        this.username = l.get(2);
        this.password = l.get(3);
        this.saldo = new Saldo();
        this.saldo.adicionarSaldo(new Moeda(), Float.parseFloat(l.get(4)));
        this.sessaoIniciada = false;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIdApostador() {
        return idApostador;
    }

    public void setIdApostador(String idApostador) {
        this.idApostador = idApostador;
    }

    public boolean isSessaoIniciada() {
        return sessaoIniciada;
    }

    public void setSessaoIniciada(boolean sessaoIniciada) {
        this.sessaoIniciada = sessaoIniciada;
    }

    public void addSaldo(float quantia){
        this.saldo.adicionarSaldo(new Moeda(), quantia);
    }

    public void levantamento(float quantia){
        this.saldo.retirarSaldo(new Moeda(), quantia);
    }

    public void addSaldo(Moeda m, float quantia){
        this.saldo.adicionarSaldo(m, quantia);
    }

    public void levantamento(Moeda m, float quantia){
        this.saldo.retirarSaldo(m, quantia);
    }

    public boolean validaQuantia(float quantia){
        return saldo.isItOkay(new Moeda(), quantia);
    }

    public boolean validaQuantia(Moeda m, float quantia){
        return saldo.isItOkay(m, quantia);
    }

    public float calculaGanhosAposta(float odd, float quantia){
        return odd*quantia;
    }


    @Override
    public String toString() {
        return "Entidades.Apostador{" +
                "email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", username='" + username + '\'' +
                ", idApostador=" + idApostador +
                ", sessaoIniciada=" + sessaoIniciada +
                '}';
    }

    public Dados<Apostador> fromRow(final List<String> l) {
        return new Apostador(l);
    }

    public List<String> toRow() {
        List<String> r = new ArrayList<>();
        r.add(this.idApostador);
        r.add(this.email);
        r.add(this.username);
        r.add(this.password);
        r.add(String.valueOf(this.saldo));
        return r;
    }
}
