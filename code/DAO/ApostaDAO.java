package DAO;

import Entidades.Aposta;

import java.util.*;

public class ApostaDAO extends DataAcessObject<Integer, Aposta> {
    private static ApostaDAO singleton = new ApostaDAO();

    public ApostaDAO() {
        super(new Aposta(), "Aposta", Arrays.asList("idAposta", "quantia", "odd", "idEvento"));
    }

    public static ApostaDAO getInstance(){
        return ApostaDAO.singleton;
    }

    public ArrayList<Aposta> values(){
        return (ArrayList<Aposta>) super.values();
    }

    public Aposta get(final int key) {
        return super.get(key);
    }

    public Aposta put(final Aposta value) {
        return super.put(value, value.getIdAposta());
    }

    public Aposta remove(final int key) {
        return super.remove(key);
    }

    public Set<Aposta> search(final int value) {
        return super.search(value, 0);
    }

    public Set<Aposta> searchEvento(final int value) {
        return super.search(value, 3);
    }
}
