import Entidades.Aposta;
import Entidades.ResultadoPossivel;

import java.util.List;

public interface IApostadorFacade {
    boolean validaLogin(String email, String password);
    boolean validaRegisto(String email);
    void logout();
    void registaAposta(int idApostador, ResultadoPossivel resultado);
    List<Aposta> consultarHistorico(int idApostador);
    void atualizaConfiguracoesConta(int idApostador, String email, String password);
    void levantarSaldo(int idApostador, float quantia);
    void depositarSaldo(int idApostador, float quantia);
    void consultarEstatisticas(int idApostador);
}
