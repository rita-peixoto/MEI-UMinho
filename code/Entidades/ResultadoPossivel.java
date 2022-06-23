package Entidades;

import DAO.Dados;
import java.util.*;

public class ResultadoPossivel implements Dados<ResultadoPossivel>{

    private String idResultado;
    private String descricao;
    private boolean ganhou;
    private float odd;
    private String estado;
    private String tipoAposta;

    public ResultadoPossivel(String descricao, boolean ganhou, float odd, String estado, String idResultado, String tipoAposta) {
        this.descricao = descricao;
        this.ganhou = ganhou;
        this.odd = odd;
        this.estado = estado;
        this.idResultado = idResultado;
        this.tipoAposta = tipoAposta;
    }

    public ResultadoPossivel(){

    }

    public ResultadoPossivel(List<String> l){
        this.idResultado = l.get(0);
        this.odd = Float.parseFloat(l.get(1));
        this.tipoAposta = l.get(2);
        this.ganhou = Boolean.parseBoolean(l.get(3));
        this.descricao = l.get(4);
        this.estado = l.get(5);
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public boolean isGanhou() {
        return ganhou;
    }

    public void setGanhou(boolean ganhou) {
        this.ganhou = ganhou;
    }

    public float getOdd() {
        return odd;
    }

    public void setOdd(float odd) {
        this.odd = odd;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getIdResultado() {
        return idResultado;
    }

    public void setIdResultado(String idResultado) {
        this.idResultado = idResultado;
    }

    public String getTipoAposta() {
        return tipoAposta;
    }

    public void setTipoAposta(String tipoAposta) {
        this.tipoAposta = tipoAposta;
    }

    @Override
    public String toString() {
        return "Entidades.ResultadoPossivel{" +
                "descricao='" + descricao + '\'' +
                ", ganhou=" + ganhou +
                ", odd=" + odd +
                ", estado='" + estado + '\'' +
                ", idResultado=" + idResultado +
                ", tipoAposta='" + tipoAposta + '\'' +
                '}';
    }

    public Dados<ResultadoPossivel> fromRow(final List<String> l) {
        return new ResultadoPossivel(l);
    }

    public List<String> toRow() {
        List<String> r = new ArrayList<>();
        r.add(this.idResultado);
        r.add(String.valueOf(this.odd));
        r.add(String.valueOf(this.tipoAposta));
        r.add(String.valueOf(this.ganhou));
        r.add(this.descricao);
        r.add(this.estado);
        return r;
    }
}
