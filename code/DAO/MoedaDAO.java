package DAO;

import Entidades.Moeda;

import java.util.*;

public class MoedaDAO extends DataAcessObject<String, Moeda>{

    private static MoedaDAO singleton = new MoedaDAO();

    public MoedaDAO() {
        super(new Moeda(), "Moeda", Arrays.asList("nome", "token", "ratio", "imposto"));
    }

    public static MoedaDAO getInstance(){
        return MoedaDAO.singleton;
    }

    public ArrayList<Moeda> values(){
        return (ArrayList<Moeda>) super.values();
    }

    public Moeda get(final String key) {
        return super.get(key);
    }

    public Moeda put(final Moeda value) {
        return super.put(value, value.getNome());
    }

    public Moeda remove(final String key) {
        return super.remove(key);
    }

    public Set<Moeda> search(final String value) {
        return super.search(value, 0);
    }

    public Set<Moeda> searchToken(final String value) {
        return super.search(value, 1);
    }
}
