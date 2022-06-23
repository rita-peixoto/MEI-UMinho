public interface IGestorFacade {
    void adicionarEvento();
    void atualizarOddsEvento();
    void atualizarEstadoApostas();
    void atualizarResultadoEvento(int idEvento, String resultado);
    void finalizarEvento(int idEvento, String resultado);
}
