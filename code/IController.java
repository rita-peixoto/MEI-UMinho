import Entidades.Aposta;
import Entidades.Apostador;
import Entidades.Evento;
import Entidades.ResultadoPossivel;

import java.util.List;

public interface IController {

    //funcoes auxiliares
    public List<Evento> getEventos();

    public List<ResultadoPossivel> getResultadosPossiveis (String idEvento);

    public List<Aposta> getApostas(String idApostador);

    public Evento getEvento(String evento);

    public Aposta getAposta(String aposta);

    public ResultadoPossivel getResultadoPossivel(String resultadoPossivel);


    //funcoes relativas aos apostadores
    public boolean verificarLoginApostador(String email, String password);

    public boolean validarAposta(String idApostador, Double quantia);

    public Double calcularGanhos(String apostaID, double quantia);

    public boolean alterarConfiguracoes(Apostador a);

    //pode-se so usar a transferir ou usar a levantar e depositar
    public boolean transferirSaldo(String idApostador, double quantia);

    public boolean levantarSaldo(String idApostador, double quantia);

    public boolean depositarSaldo(String idApostador, double quantia);

    //ver como sera implementado
    //public ? getEstatisticas(String idApostador);

    //funcoes relativas aos gestores
    public boolean verificarLoginGestor(String email, String password);

    public boolean adicionarEvento(Evento e);

    public boolean atualizarEvento(String idEvento, String novo_estado);

    public boolean atualizarResultado(String idResultado, boolean ganhou);







}
