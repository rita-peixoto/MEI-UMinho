package DAO;

import Entidades.ResultadoPossivel;

import java.util.*;

public class ResultadoPossivelDAO extends DataAcessObject<String, ResultadoPossivel> {
    private static ResultadoPossivelDAO singleton = new ResultadoPossivelDAO();

    public ResultadoPossivelDAO() {
        super(new ResultadoPossivel(), "ResultadoPossivel", Arrays.asList("idResultadoPossivel", "odd", "tipoAposta", "ganhou", "descricao", "estado"));
    }

    public static ResultadoPossivelDAO getInstance(){
        return ResultadoPossivelDAO.singleton;
    }

    public ArrayList<ResultadoPossivel> values(){
        return (ArrayList<ResultadoPossivel>) super.values();
    }

    public ResultadoPossivel get(final String key) {
        return super.get(key);
    }

    public ResultadoPossivel put(final ResultadoPossivel value) {
        return super.put(value, value.getIdResultado());
    }

    public ResultadoPossivel remove(final String key) {
        return super.remove(key);
    }

    public Set<ResultadoPossivel> search(final String value) {
        return super.search(value, 0);
    }

    public Set<ResultadoPossivel> searchTipo(final String value) {
        return super.search(value, 2);
    }

    public Set<ResultadoPossivel> searchGanhou(final String value) {
        return super.search(value, 3);
    }

    public Set<ResultadoPossivel> searchEstado(final String value) {
        return super.search(value, 5);
    }
}
