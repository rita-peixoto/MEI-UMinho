package BL;

import Entidades.*;
import GUI.ApostadorObserver;
import Exception.IsNullException;
import java.time.LocalDateTime;
import java.util.List;

public interface IRasBetFacade {
    boolean registo(String email, String nome, String user, String pwd, String data);
    void logout(String idApostador);
    boolean login(String email, String password);
    List<Evento> listarEventos();
    List<ResultadoPossivel> listarResultadosPossiveis(String idEvento);
    List<Aposta> listarApostas(String emailApostador);

    /**
     * Pede um apostador com base no email. Note-se que a funcao search devolve um set mas so se dá return de 1 apostador por que snao podem haver 2 emails identicos.
     * @param email email do utilizador a ser validado
     * */
    Apostador getApostador(String email) throws IsNullException;

    /**
     *  Metodo que
     * @return
     * */
    void levantarSaldo(String idApostador, float quantia);

    /**
     *  Metodo que
     * @return
     * */
    void depositarSaldo(String idApostador, float quantia);

    /**
     *  Metodo que
     * @return
     * */
    void transferirSaldo(String idApostador, float quantia, String idMoeda);

    /**
     * Valida se uma moeda pode ou não ser inserida no sistema
     * @param nome nome da moeda
     * @param token token representativo da moeda
     * @param imposto imposto aplicado ao cambio desta moeda
     * @param ratio ratio da moeda em relação ao euro
     * @return Retorna se a moeda pode ou não ser inserida no sistema.
     * */
    boolean validarMoeda(String nome, String token, Float ratio, Float imposto);

    /**
     * Finaliza o evento, pondo todos os resultados possiveis com o estado "fechado"
     * @param idEvento id do evento a fechar
     * @param resultado resultado final. para ja nao faz nada.
     * */
    void finalizarEvento(String idEvento, String resultado);

    /**
     *  Metodo que
     * @return
     * */
    boolean validaAposta(String email, Float quantia, String moeda);

    /**
     *  Metodo que
     * @param
     * */
    void registaAposta(String emailApostador, String idResultado, float quantia, String moeda);

    /**
     *  Metodo que
     * @param
     * */
    void consultarEstatisticas(String idApostador);

    /**
     *  Metodo que
     * @param
     * @return
     * */
    List<Aposta> consultarHistorico(String idApostador);

    /**
     *  Metodo que
     * @param
     * @param
     * */
    void atualizarResultadoEvento(String idEvento, String resultado);

    /**
     * Indica se um resultado possivel se cumpriu ou nao
     * @param idResultado id do resultado possivel
     * @param ganhou valor booleano que indica se o resultado possivel se cumpriu ou nao
     * */
    void atualizarResultadoPossivel(String idResultado, boolean ganhou);

    /**
     * Atualiza a odd de um resultado
     * @param idResultado id do resultado possivel
     * @param odd odd nova
     * */
    void atualizarOdd(String idResultado, Float odd);

    /**
     * Atualiza todas as apostas de um evento de um certo tipo
     * @param eventoId id do resultado possivel
     * @param tipo tipo da aposta a ser alterada
     * @param estado Novo estado das apostas.
     * */
    void atualizarEstadoApostas(String eventoId, String estado, String tipo);

    /**
     * Altera certos parametros da conta do apostador
     * @param idApostador id do apostador
     * @param username novo username
     * @param email novo email
     * @param password nova password
     * */
    void alterarConfigConta(String idApostador,String username, String email, String password);

    /**
     *  Metodo que
     * @param
     * */
    void addResultadoPossivel(ResultadoPossivel p);

    /**
     *  Metodo que
     * @param
     * */
    void addParticipante(Participante p);

    /**
     *  Metodo que
     * @param
     * */
    void adicionarEvento(String nome, List<String> participantes, LocalDateTime data, String desporto, String estado, List<String> resultadosPossiveis);

    //observer

    /**
     *  Metodo que
     * @param
     * */
    void addObserver(String oserverId);

    /**
     *  Metodo que
     * @param
     * */
    void removeObserver(String emailObserver);


    /**
     *  Metodo que
     * @param
     * @return
     * */
    boolean containsMoeda(String moeda);
}
