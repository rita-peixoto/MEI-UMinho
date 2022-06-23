package BL;

import DAO.EventoDAO;
import DAO.ParticipanteDAO;
import DAO.ResultadoPossivelDAO;
import Entidades.Evento;
import Entidades.Participante;
import Entidades.ResultadoPossivel;
import Entidades.Subject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Gestor implements IGestorFacade{

    @Override
    public void atualizarOdds(String idResultado, Float odd) {
        ResultadoPossivel r = ResultadoPossivelDAO.getInstance().get(idResultado);
        r.setOdd(odd);
        ResultadoPossivelDAO.getInstance().put(r);
    }

    @Override
    public void atualizarEstadoApostas(String idEvento, String tipoAposta, String estado) {
        List<ResultadoPossivel> resultadoPossivel = ResultadoPossivelDAO.
                getInstance().searchEvento(idEvento);
        List<ResultadoPossivel> resultadoPossivels = ResultadoPossivelDAO.
                getInstance().searchEvento(idEvento)
                .stream().filter(y -> y.getTipoAposta().equals(tipoAposta))
                .collect(Collectors.toList());
        for(ResultadoPossivel r : resultadoPossivels){
            r.setEstado(estado);
            ResultadoPossivelDAO.getInstance().put(r);
        }
        Subject.getInstance().notifyApostadores();
    }

    @Override
    public void atualizarResultadoEvento(String idEvento, String resultado) {

    }

    @Override
    public void atualizarResultadoPossivel(String idResultado, boolean ganhou) {
        ResultadoPossivel r = ResultadoPossivelDAO.getInstance().get(idResultado);
        r.setGanhou(ganhou);
        ResultadoPossivelDAO.getInstance().put(r);
    }

    public Evento getEvento(String idEvento){
        return EventoDAO.getInstance().get(idEvento);
    }

    @Override
    public void adicionarEvento(Evento e,List<String> participantes,List<String> resultadosPossiveis) {
        EventoDAO.getInstance().put(e);
        for(String participante : participantes){
            e.getParticipantes().addEventosParticipantes(e.getIdEvento(),participante);
        }
        for(String resultado : resultadosPossiveis){
            e.getResultadosPossiveis().addEventosResultados(e.getIdEvento(),resultado);
        }
    }

    @Override
    //INACABADO -> falta observer
    public void finalizarEvento(String idEvento, String resultado) {
        Evento e = EventoDAO.getInstance().get(idEvento);
        List<ResultadoPossivel> resultadoPossivels = ResultadoPossivelDAO.getInstance().searchEvento(idEvento);
        //atualizar os estados da aposta todos para fechado
        for(ResultadoPossivel r : resultadoPossivels){
            r.setEstado("fechado");
            ResultadoPossivelDAO.getInstance().put(r);
        }
        Subject.getInstance().notifyApostadores();
        //resultados.forEach(y -> y.);
    }

    @Override
    public List<Evento> listarEventos() {
        return EventoDAO.getInstance().values();
    }
}
