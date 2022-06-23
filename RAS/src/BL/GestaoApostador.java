
package BL;

import DAO.ApostaDAO;
import DAO.ApostadorDAO;
import Encriptacao.Encriptacao;
import Entidades.*;
import Exception.IsNullException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GestaoApostador implements IApostadorFacade {

    public boolean registo(String email, String nome, String user, String pwd, String data) {
        boolean isOK = false;
        String id = email + nome + user;
        String codId = Encriptacao.encrypt(id);
        String password = Encriptacao.encrypt(pwd);

        if(ApostadorDAO.getInstance().searchEmail(email).isEmpty()){
            isOK = true;
            try {
                ApostadorDAO.getInstance().put(new Apostador(email, nome, password, user, codId, new SimpleDateFormat("dd/MM/yyyy").parse(data)),codId);
            } catch (ParseException e) {
                System.out.println("Erro ao inserir a data: {" + e + "}.");
            }
        }
        return isOK;
    }

    @Override
    public boolean validaLogin(String email, String password) {
        List<Apostador> apostadores = ApostadorDAO.getInstance().searchEmail(email);
        boolean isOK = false;
        for (Apostador a:apostadores) {
            if (a.getPassword().equals(password))
                return true;
        };
        return isOK;
    }

    @Override
    public boolean validaRegisto(String email) {
        return ApostadorDAO.getInstance().searchEmail(email).isEmpty();
    }

    @Override
    //para remover
    public void logout() {

    }

    @Override
    public void registaAposta(float quantia, float oddFixa, String idResultado, String idAposta, String idA) {
        //remover saldo
        Aposta aposta = new Aposta(quantia,oddFixa,idResultado,ApostaDAO.getInstance().generateUniqueId(), idA);
        ApostaDAO.getInstance().put(aposta);

    }

    @Override
    public List<Aposta> consultarHistorico(String idApostador) {
        List<Aposta> apostas = new ArrayList<>(ApostaDAO.getInstance().searchApostador(idApostador));
        return apostas;
    }

    @Override
    public void atualizaConfiguracoesConta(String idApostador, String username,String email, String password) {
        Apostador a = ApostadorDAO.getInstance().get(idApostador);
        a.setUsername(username);
        String password_ = String.valueOf(Encriptacao.encrypt(password));
        a.setPassword(password_);
        //this.apostadores.remove(idApostador);
        ApostadorDAO.getInstance().put(a);

    }

    @Override
    //+ara ja nao incorpora diferentes moedas levantarSaldo
    public void levantarSaldo(String idApostador, float quantia) {
        Apostador a = ApostadorDAO.getInstance().get(idApostador);
        a.levantamento(quantia);
        //this.apostadores.remove(idApostador);
        ApostadorDAO.getInstance().put(a);

    }

    /**
     * Pede um apostador com base no email. Note-se que a funcao search devolve um set mas so se dá return de 1 apostador por que snao podem haver 2 emails identicos.
     * @param email email do utilizador a ser validado
     * */
    public Apostador getApostador(String email) throws IsNullException{
        List<Apostador> apostadores = ApostadorDAO.getInstance().searchEmail(email);
        if(apostadores.isEmpty()) throw new IsNullException();
        return apostadores.get(0);
    }

    @Override
    //para ja nao incorpora diferentes moedas
    //*PDE ESTAR MAL
    public void depositarSaldo(String idApostador, float quantia) {
        Apostador a = ApostadorDAO.getInstance().get(idApostador);
        a.addSaldo(quantia);
        //this.apostadores.remove(idApostador);
        ApostadorDAO.getInstance().put(a);

    }

    @Override
    public void transferirSaldo(String idApostador, float quantia, String idMoeda) {
        ;
    }

    @Override
    //definir o que sao estatisticas da conta.
    public void consultarEstatisticas(String idApostador) {
    }
}
