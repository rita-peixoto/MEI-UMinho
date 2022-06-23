package PL;

import Entidades.Aposta;
import Entidades.Apostador;
import Entidades.Evento;
import Entidades.ResultadoPossivel;
import GUI.ApostadorObserver;
import Exception.*;
import java.util.List;

public interface IController {

    /**
     *  Metodo que
     * @return
     * */
    List<String> getEventos();

    /**
     *  Metodo que
     * @return
     * */
    List<String> getResultadosPossiveis (String idEvento);

    /**
     *  Metodo que
     * @return
     * */
    List<String> getApostas(String idApostador);

    /**
     *  Metodo que
     * @return
     * */
    boolean validaRegisto(String email, String nome, String user, String pwd, String data);

    /**
     *  Metodo que
     * @return
     * */
    boolean verificarLoginApostador(String email, String password);

    /**
     *  Metodo que
     * @return
     * */
    boolean validarAposta(String idApostador, Float quantia, String moeda);

    /**
     *  Metodo que
     * @return
     * */
    void addObserver(String observerId);

    /**
     *  Metodo que
     * @return
     * */
    void removeObserver(String emailObserver);

    /**
     *  Metodo que
     * @return
     * */
    Double calcularGanhos(String apostaID, double quantia);

    /**
     *  Metodo que
     * @return
     * */
    boolean alterarConfiguracoes(Apostador a);

    /**
     *  Metodo que
     * @return
     * */
    boolean transferirSaldo(String idApostador, double quantia);

    /**
     *  Metodo que
     * @return
     * */
    boolean levantarSaldo(String idApostador, double quantia);

    /**
     *  Metodo que
     * @return
     * */
    boolean depositarSaldo(String idApostador, double quantia, String moeda);

    /**
     *  Metodo que
     * @return
     * */
    boolean verificarLoginGestor(String email, String password);

    /**
     *  Metodo que
     * @return
     * */
    boolean adicionarEvento(Evento e);

    /**
     *  Metodo que
     * @return
     * */
    boolean adicionarResultadoPossivel(String descricao, boolean ganhou, float odd, String estado, String idResultado, String tipoAposta, String idEvento);

    /**
     *  Metodo que
     * @return
     * */
    boolean atualizarEvento(String idEvento, String novo_estado);

    /**
     *  Metodo que
     * @return
     * */
    boolean atualizarResultado(String idResultado, boolean ganhou);

    /**
     *  Metodo que
     * @return
     * */
    String getNomeApostador(String email) throws IsNullException;

    /**
     *  Metodo que
     * @return
     * */
    boolean verificaData(String data);

    /**
     *  Metodo que
     * @return
     * */
    boolean validaData(String data);

    /**
     *  Metodo que
     * @return
     * */
    boolean validarMoeda(String nome, String token, Float ratio, Float imposto);

    /**
     *  Metodo que
     * @return
     * */
    void logoutUtilizador(String codID);

    /**
     *  Metodo que
     * @return
     * */
    boolean mudarUsername(String codID, String user, String pwd);

    /**
     *  Metodo que
     * @return
     * */
    boolean mudarEmail(String codID, String email, String pwd);

    /**
     *  Metodo que
     * @return
     * */
    boolean mudarPassword(String codID, String pwd1, String pwd);

    /**
     *  Metodo que
     * @return
     * */
    List<String> getMoedasDisponiveis();

    /**
     *  Metodo que
     * @return
     * */
    boolean setMoeda(String moeda);

    /**
     *  Metodo que
     * @return
     * */
    String getMoeda();

    /**
     *  Metodo que
     * @return Lista de Eventos formatada
     * */
    List<String> getListaEventos();

    /**
     *  Metodo que
     * @return Lista de Eventos formatada
     * */
    List<String> getListaEventosAtivos();

    /**
     *  Metodo que permite a criação de eventos
     * @param nome Nome do Evento
     * @param data Data do Evento
     * @param equipa1 Equipa participante 1
     * @param equipa2 Equipa participante 2
     * */
    void adicionarEvento(String nome, String data, String equipa1, String equipa2);

    /**
     *  Metodo que cancela um Evento
     * @param s id do Evento
     * */
    void cancelarEvento(String s);

    /**
     *  Metodo que suspende um Evento
     * @param s id do Evento
     * */
    void supenderEvento(String s);

    /**
     *  Metodo que suspende um Evento
     * @param s id do Evento
     * @param aposta Descição da Aposta
     * @param odd Odd da Aposta
     * */
    void adicionarApostaIn(String s, String aposta, float odd);

    /**
     *  Metodo que ativa um Evento suspenso
     * @param s id do Evento
     * */
    void ativarEvento(String s);
}
