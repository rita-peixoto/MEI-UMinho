package PL;

import BL.*;
import Entidades.*;
import GUI.ApostadorObserver;
import Exception.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Controller implements IController{

    public IRasBetFacade rasbet;
    public ApostadorObserver ao;
    private String moeda;

    //id do apostador que fez login . -1 qd nao fez.
    public String apostador_sessao;
    //id do gestor que fez login . -1 qd nao fez.
    public String gestor_sessao;

    public Controller() {
        this.rasbet = new RasBetModel();
        this.apostador_sessao = "-1";
        this.gestor_sessao = "-1";
        this.moeda = "Euro";
    }

    @Override
    public List<String> getEventos() {
        return this.rasbet.listarEventos().stream().map(Evento::toString).collect(Collectors.toList());
    }

    @Override
    public List<String> getResultadosPossiveis(String idEvento) {
        return this.rasbet.listarResultadosPossiveis(idEvento).stream().map(ResultadoPossivel::toString).collect(Collectors.toList());
    }

    @Override
    public List<String> getApostas(String emailApostador) {
        return this.rasbet.listarApostas(emailApostador).stream().map(Aposta::toString).collect(Collectors.toList());
    }

    @Override
    public boolean verificaData(String data){
        boolean isOK = false;
        try {
            Date start = new SimpleDateFormat("dd/MM/yyyy").parse(data);
            Date stop = new Date();

            long diffInMillies = Math.abs(stop.getTime() - start.getTime());
            long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
            if(diff >= 6575){
                isOK = true;
            }
        } catch (Exception e){
            System.out.println("Erro ao inserir a data: {" + e + "}.");
        }
        return isOK;
    }

    @Override
    public boolean validaData(String data) {
        boolean isOK = false;
        try {
            Date inserida = new SimpleDateFormat("dd/MM/yyyy").parse(data);
            Date agora = new Date();
            if(inserida.after(agora)){
                isOK = true;
            }
        } catch (Exception e){
            System.out.println("Erro ao inserir a data: {" + e + "}.");
        }
        return isOK;
    }

    @Override
    public boolean validarMoeda(String nome, String token, Float ratio, Float imposto) {
        return rasbet.validarMoeda(nome, token, ratio, imposto);
    }

    @Override
    public void logoutUtilizador(String codID) {
        this.rasbet.logout(codID);
    }

    @Override
    public boolean mudarUsername(String codID, String user, String pwd) {
        return false;
    }

    @Override
    public boolean mudarEmail(String codID, String email, String pwd) {
        return false;
    }

    @Override
    public boolean mudarPassword(String codID, String pwd1, String pwd) {
        return false;
    }

    @Override
    public List<String> getMoedasDisponiveis() {
        return null;
    }

    @Override
    public boolean setMoeda(String moeda) {
        if(rasbet.containsMoeda(moeda)){
            this.moeda = moeda;
            return true;
        }
        return false;
    }

    @Override
    public String getMoeda(){
        return this.moeda;
    }

    @Override
    public List<String> getListaEventos() {
        return null;
    }

    @Override
    public List<String> getListaEventosAtivos() {
        return null;
    }

    @Override
    public void adicionarEvento(String nome, String data, String equipa1, String equipa2) {

    }

    @Override
    public void cancelarEvento(String s) {

    }

    @Override
    public void supenderEvento(String s) {

    }

    @Override
    public void adicionarApostaIn(String s, String aposta, float odd) {

    }

    @Override
    public void ativarEvento(String s) {

    }

    @Override
    public boolean validaRegisto(String email, String nome, String user, String pwd, String data) {
        return this.rasbet.registo(email,nome,user,pwd,data);
    }

    @Override
    public boolean verificarLoginApostador(String email, String password) {
        return this.rasbet.login(email, password);
    }

    public boolean validarAposta(String idApostador, Float quantia, String moeda){
        return true;
    }

    @Override
    public void addObserver(String observerId) {
        this.rasbet.addObserver(observerId);
    }

    @Override
    public void removeObserver(String emailObserver) {
        this.rasbet.removeObserver(emailObserver);
    }


    //calcular os ganhos de uma aposta
    public Double calcularGanhos(String apostaID, double quantia){
        return -10000000.0;
    }

    @Override
    public boolean alterarConfiguracoes(Apostador a) {
        return false;
    }

    @Override
    public boolean transferirSaldo(String idApostador, double quantia) {
        return false;
    }

    @Override
    public boolean levantarSaldo(String idApostador, double quantia) {
        return false;
    }

    @Override
    public boolean depositarSaldo(String idApostador, double quantia, String moeda) {
        return false;
    }

    @Override
    public boolean verificarLoginGestor(String email, String password) {
        return true;
    }

    @Override
    public boolean adicionarEvento(Evento e) {
        return false;
    }

    @Override
    public boolean adicionarResultadoPossivel(String descricao, boolean ganhou, float odd, String estado, String idResultado, String tipoAposta, String idEvento) {
        //this.rasbet.addResultadoPossivel();
        return false;
    }

    @Override
    public boolean atualizarEvento(String idEvento, String novo_estado) {
        return false;
    }

    @Override
    public boolean atualizarResultado(String idResultado, boolean ganhou) {
        return false;
    }

    @Override
    public String getNomeApostador(String email) throws IsNullException {
        return rasbet.getApostador(email).getUsername();
    }
}
