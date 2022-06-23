import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface IRasBetFacade {
    void registo(String username, String email, String password, Date dataNascimento, float saldo);
    void logout(int idApostador);
    void login(String email, String password);
    void listarEventos();
    void levantarSaldo(int idApostador, float quantia);
    void depositarSaldo(int idApostador, float quantia);
    void finalizarEvento(int idEvento, String resultado);
    void efetuarAposta(int idApostador, int idResultado, int idEvento);
    void consultarEstatisticas(int idApostador);
    void consultarHistorico(int idApostador);
    void atualizarResultadoEvento();
    void atualizarOdd();
    void atualizarEstadoApostas();
    void alterarConfigConta();
    void adicionarEvento(String nome, List<String> participantes, LocalDateTime data, String desporto);
}
