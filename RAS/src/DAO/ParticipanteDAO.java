package DAO;

import Entidades.Participante;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class ParticipanteDAO extends DataAcessObject<String, Participante> {
    private static ParticipanteDAO singleton = new ParticipanteDAO();

    public ParticipanteDAO() {
        super(new Participante(), "Participante", Arrays.asList("idParticipante", "pontuacao", "rank"));
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
        return super.put(value, value.getIdParticipante());
    }

    public Participante remove(final String key) {
        return super.remove(key);
    }

    public List<Participante> search(final String value) {
        return super.search(value, 0).stream().toList();
    }

    public void addEventosParticipantes(String idEvento, String idParticipante){
        try {
        Connection connection = BaseDados.getConnection();
        String stm = "DELETE FROM eventoParticipantes WHERE Evento = '" + idEvento+"' AND participante = '" + idParticipante+"';" ;
            PreparedStatement pst = null;
            pst = connection.prepareStatement(stm);
            pst.executeUpdate();
            connection.commit();
        stm = "INSERT INTO eventoParticipantes VALUES(" +
                "'"+idEvento+"', '"+ idParticipante+"');";
            pst = connection.prepareStatement(stm);
            pst.executeUpdate();
            connection.commit();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
