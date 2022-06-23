package BL;

import DAO.*;
import Entidades.*;
import GUI.ApostadorObserver;
import Exception.IsNullException;

import java.time.LocalDateTime;
import java.util.List;

public class RasBetModel implements IRasBetFacade {
    public IGestorFacade gestor;
    public IApostadorFacade apostadorFacade;

    public RasBetModel() {
        this.gestor = new Gestor();
        this.apostadorFacade = new GestaoApostador();
    }


    //feito
    //nota: GestaoApostador chama um metodo da ApostaDAO para gerar id da aposta unico
    public void registaAposta(String emailApostador, String idResultado, float quantia, String moeda) {
        //remover saldo da conta do utilisador
        Apostador a = null;
        try {
            a = this.getApostador(emailApostador);
        } catch (IsNullException e) {
            e.printStackTrace();
        }
        Moeda m = MoedaDAO.getInstance().get(moeda);
        a.levantamento(m,quantia);
        ApostadorDAO.getInstance().put(a);
        ResultadoPossivel r = ResultadoPossivelDAO.getInstance().get(idResultado);
        //adicionar aposta
        this.apostadorFacade.registaAposta(quantia,r.getOdd(),idResultado,"", a.getIdApostador());
    }

    public boolean containsMoeda(String s){
        return MoedaDAO.getInstance().containsKey(s);
    }

    @Override
    public boolean registo(String email, String nome, String user, String pwd, String data) {
        return apostadorFacade.registo(email,nome,user,pwd,data);
    }

    @Override
    public void logout(String idApostador) {
        apostadorFacade.logout();
    }

    @Override
    public boolean login(String email, String password) {
        return apostadorFacade.validaLogin(email, password);
    }

    @Override
    //feito
    public List<Evento> listarEventos() {
        return this.gestor.listarEventos();
    }

    @Override
    public List<ResultadoPossivel> listarResultadosPossiveis(String idEvento) {
        return ResultadoPossivelDAO.getInstance().searchEvento(idEvento);
    }

    @Override
    public List<Aposta> listarApostas(String emailApostador) {
        List<Aposta> ret = null;
        try {
            Apostador a = this.getApostador(emailApostador);
            ret = ApostaDAO.getInstance().searchApostador(a.getIdApostador());
        } catch (IsNullException e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public Apostador getApostador(String email) throws IsNullException {
        Apostador a = this.apostadorFacade.getApostador(email);
        if (a != null) {
            Saldo s = ApostadorDAO.getInstance().getSaldo(a.getIdApostador());
            a.setSaldo(s);
        }
        return a;
    }


    @Override
    //aquela cena de dar update.
    public void levantarSaldo(String idApostador, float quantia) {
        this.apostadorFacade.levantarSaldo(idApostador,quantia);
    }

    //aquela cena de dar update.
    @Override
    public void depositarSaldo(String idApostador, float quantia) {
        this.apostadorFacade.depositarSaldo(idApostador, quantia);
    }

    @Override
    public void transferirSaldo(String idApostador, float quantia, String idMoeda) {
        this.apostadorFacade.transferirSaldo(idApostador,quantia,idMoeda);
    }

    @Override
    public boolean validarMoeda(String nome, String token, Float ratio, Float imposto) {
        boolean isOK = false;
        if(!MoedaDAO.getInstance().containsKey(nome)){
            MoedaDAO.getInstance().put(new Moeda(nome, token, ratio, imposto));
            isOK = true;
        }
        return isOK;
    }

    @Override
    public void finalizarEvento(String idEvento, String resultado) {
        this.gestor.finalizarEvento(idEvento,resultado);
    }

    @Override
    public boolean validaAposta(String email, Float quantia, String moeda) {
        Apostador b = null;
        try {
            b = this.apostadorFacade.getApostador(email);
        } catch (IsNullException e) {
            e.printStackTrace();
        }
        boolean res = false;
        Apostador a;
        Moeda m = MoedaDAO.getInstance().get(moeda);
        if (b != null && m != null) {
            a = ApostadorDAO.getInstance().get(b.getIdApostador());
            res = a.getSaldo().isItOkay(m, quantia);
        }
        return res;
    }


    //definir
    @Override
    public void consultarEstatisticas(String idApostador) {

    }

    @Override
    public List<Aposta> consultarHistorico(String idApostador) {
        return this.apostadorFacade.consultarHistorico(idApostador);
    }

    @Override
    //aquela cena de substituir -> falta por resultado final
    public void atualizarResultadoEvento(String idEvento, String resultado) {
    }

    public void atualizarResultadoPossivel(String idResultado, boolean ganhou) {
        this.gestor.atualizarResultadoPossivel(idResultado,ganhou);
    }

    @Override
    public void atualizarOdd(String idResultado, Float odd) {
        this.gestor.atualizarOdds(idResultado,odd);
    }


    @Override
    public void atualizarEstadoApostas(String eventoId, String estado, String tipo) {
        this.gestor.atualizarEstadoApostas(eventoId,tipo,estado);
    }


    @Override
    public void alterarConfigConta(String idApostador,String username, String email,  String password) {
        this.apostadorFacade.atualizaConfiguracoesConta(idApostador,username,email,password);
    }

    @Override
    public void addResultadoPossivel(ResultadoPossivel p) {
        System.out.println(p.toString());
        ResultadoPossivelDAO.getInstance().put(p);
    }

    @Override
    public void addParticipante(Participante p) {
        ParticipanteDAO.getInstance().put(p);
    }

    @Override
    //ver cena de ides unicos
    public void adicionarEvento(String nome, List<String> participantes, LocalDateTime data, String desporto,String estado, List<String> resultadosPossiveis) {
        Evento e = new Evento(estado,desporto, EventoDAO.getInstance().generateUniqueId(), nome,data.toString());
        this.gestor.adicionarEvento(e,participantes,resultadosPossiveis);
    }

    @Override
    public void addObserver(String idObserver) {
        try {
            Apostador a = this.getApostador(idObserver);
            ApostadorObserver ao = new ApostadorObserver(a);
            Subject.getInstance().addApostador(ao);
        } catch (IsNullException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void removeObserver(String emailObserver) {
        try {
            Apostador a = this.getApostador(emailObserver);
            ApostadorObserver ao = new ApostadorObserver(a);
            Subject.getInstance().removeApostador(ao);
        } catch (IsNullException e) {
            e.printStackTrace();
        }
    }


}
