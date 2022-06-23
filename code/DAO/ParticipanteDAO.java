package DAO;

import Entidades.Participante;

import java.util.*;

public class ParticipanteDAO extends DataAcessObject<String, Participante> {
    private static ParticipanteDAO singleton = new ParticipanteDAO();

    public ParticipanteDAO() {
        super(new Participante(), "Participante", Arrays.asList("nome", "pontuacao", "rank"));
    }

    public static ParticipanteDAO getInstance(){
        return ParticipanteDAO.singleton;
    }

    public ArrayList<Participante> values(){
        return (ArrayList<Participante>) super.values();
    }

    public Participante get(final String key) {
        return super.get(key);
    }

    public Participante put(final Participante value) {
        return super.put(value, value.getNome());
    }

    public Participante remove(final String key) {
        return super.remove(key);
    }

    public Set<Participante> search(final String value) {
        return super.search(value, 0);
    }
}
