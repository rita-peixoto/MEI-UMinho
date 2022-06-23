package Entidades;

import DAO.Dados;

import java.util.*;

public class Participante implements Dados<Participante> {
    private String nome;
    private boolean rank;
    private int pontuacao;

    public Participante(String nome, boolean rank, int pontuacao) {
        this.nome = nome;
        this.rank = rank;
        this.pontuacao = pontuacao;
    }

    public Participante() {

    }

    public Participante(List<String> l) {
        this.nome = l.get(0);
        this.pontuacao = Integer.parseInt(l.get(1));
        this.rank = Boolean.parseBoolean(l.get(2));
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public boolean isRank() {
        return rank;
    }

    public void setRank(boolean rank) {
        this.rank = rank;
    }

    public int getPontuacao() {
        return pontuacao;
    }

    public void setPontuacao(int pontuacao) {
        this.pontuacao = pontuacao;
    }

    @Override
    public String toString() {
        return "Entidades.Participante{" +
                "nome='" + nome + '\'' +
                ", rank=" + rank +
                ", pontuacao=" + pontuacao +
                '}';
    }

    public Dados<Participante> fromRow(final List<String> l) {
        return new Participante(l);
    }

    public List<String> toRow() {
        List<String> r = new ArrayList<>();
        r.add(this.nome);
        r.add(String.valueOf(this.pontuacao));
        r.add(String.valueOf(this.rank));
        return r;
    }
}
