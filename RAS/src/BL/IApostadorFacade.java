package BL;

import Entidades.Aposta;
import Entidades.Apostador;
import Exception.IsNullException;

import java.util.List;

public interface IApostadorFacade {

    /**
     *  Metodo que
     * @param
     * @return
     * */
    boolean registo(String email, String nome, String user, String pwd, String data);

    /**
     *  Metodo que
     * @param
     * @return
     * */
    boolean validaLogin(String email, String password);

    /**
     *  Metodo que
     * @param
     * @return
     * */
    boolean validaRegisto(String email);

    /**
     *  Metodo que
     * @param
     * */
    void logout();

    /**
     *  Metodo que
     * @param
     * @return
     * */
    Apostador getApostador(String email) throws IsNullException;

    /**
     *  Metodo que
     * @param
     * */
    void registaAposta(float quantia, float oddFixa, String idEvento, String idAposta, String idA);

    /**
     *  Metodo que
     * @param
     * */
    List<Aposta> consultarHistorico(String idApostador);

    /**
     *  Metodo que
     * @param
     * */
    void atualizaConfiguracoesConta(String idApostador, String username, String email, String password);

    /**
     *  Metodo que
     * @param
     * */
    void levantarSaldo(String idApostador, float quantia);

    /**
     *  Metodo que
     * @param
     * */
    void depositarSaldo(String idApostador, float quantia);

    /**
     *  Metodo que
     * @param
     * */
    void transferirSaldo(String idApostador, float quantia, String idMoeda);

    /**
     *  Metodo que
     * @param
     * */
    void consultarEstatisticas(String idApostador);
}
