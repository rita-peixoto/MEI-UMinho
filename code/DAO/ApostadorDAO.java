package DAO;

import Entidades.Apostador;
import java.util.*;

public class ApostadorDAO extends DataAcessObject<String, Apostador>{
    private static ApostadorDAO singleton = new ApostadorDAO();

    public ApostadorDAO() {
        super(new Apostador(), "Apostador", Arrays.asList("idApostador", "email", "username", "password", "saldo"));
    }

    public static ApostadorDAO getInstance(){
        return ApostadorDAO.singleton;
    }

    public ArrayList<Apostador> values(){
        return (ArrayList<Apostador>) super.values();
    }

    public Apostador get(final String key) {
        return super.get(key);
    }

    public Apostador put(final Apostador value) {
        return super.put(value, value.getIdApostador());
    }

    public Apostador remove(final String key) {
        return super.remove(key);
    }

    public Set<Apostador> search(final String value) {
        return super.search(value, 0);
    }

    public Set<Apostador> searchEmail(final String value) {
        return super.search(value, 1);
    }

    public Set<Apostador> searchUsername(final String value) {
        return super.search(value, 2);
    }
}
