package GUI;

import PL.Controller;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;

public class Utilizador implements IUtilizador{
    private ListView<String> listView;
    private Controller controller;
    private TextField usertxt, txt;
    private PasswordField passwordtxt;
    String email;
    String nome;

    public Utilizador(Controller controller){
        this.controller = controller;
    }

    @Override
    public Scene painelUtilizador(String nome, String email) {
        this.email = email;
        this.nome = nome;

        VBox layout = javafx.makebox();

        listView = new ListView<>();
        listView.getItems().addAll(
                "Historico de Apostas", "Lista de Eventos", "Lista de Desportos", "Consultar Carteira", "Adicionar ou Levantar Saldo" , "Configuraçoes da conta", "Ver Estatisticas"
        );

        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Button b1 = new Button("Escolher.");
        b1.setOnAction(e -> escolherMenuUtilizador());

        Button b2 = new Button("Terminar Sessão.");
        b2.setOnAction(e -> {
            controller.logoutUtilizador(email);
            javafx.endScene(e);
        });

        Button b3 = new Button("Atualizar.");
        b3.setOnAction(e -> {
            javafx.endScene(e);
            javafx.makeWindow("Menu do Utilizador", painelUtilizador(nome,email));
        });

        layout.getChildren().addAll(listView,b1,b2,b3);
        return new Scene(layout, 400, 400);
    }

    private void escolherMenuUtilizador(){
        String s = String.valueOf(listView.getSelectionModel().getSelectedItems());

        if(s.equals("[Historico de Apostas]")){
            javafx.makeWindow("Historico de Apostas de " + nome, painelHistoricoUtilizador());
        }

        if(s.equals("[Lista de Eventos]")){
            javafx.makeWindow("Lista de Eventos", painelEventosUtilizador());
        }

        if(s.equals("[Lista de Desportos]")){
            javafx.makeWindow("Desportos - RASBET", painelDesportoUtilizador());
        }

        if(s.equals("[Consultar Carteira]")){
            javafx.makeWindow("Carteira de " + nome + " - RASBET", painelCarteiraUtilizador());
        }

        if(s.equals("[Adicionar ou Levantar Saldo]")){
            javafx.makeWindow("Adicionar Saldo - RASBET", painelSaldoUtilizador());
        }

        if(s.equals("[Configuraçoes da conta]")){
            javafx.makeWindow("Configurações da conta de " + nome + " - RASBET", painelConfigUtilizador());
        }

        if(s.equals("[Ver Estatisticas]")){
            javafx.makeWindow("Painel de Estatisticas - RASBET", painelEstatisticasUtilizador());
        }
    }

    @Override
    public Scene registarUtilizador() {
        VBox layout = javafx.makebox();

        txt = new TextField();
        Label lblNome = new Label("Nome Completo");

        TextField email = new TextField();
        Label lblEmail = new Label("Endereço Email");

        usertxt = new TextField();
        Label lblUser = new Label("Username");

        passwordtxt = new PasswordField();
        Label lblPassword = new Label("Password");

        TextField d = new TextField();
        Label lblData = new Label("Data de Nascimento no formato dd/MM/yyyy");

        Button b = new Button("Registar.");
        b.setOnAction(e -> {
            String nome = txt.getText();
            String user = usertxt.getText();
            String pwd = passwordtxt.getText();
            String em = email.getText();
            String data = d.getText();

            if(user.equals("")) javafx.alert("ERRO", "Precisa de inserir um username para se registar.");
            else if(em.equals("")) javafx.alert("ERRO", "Precisa de inserir um email para se registar.");
            else if(pwd.equals("")) javafx.alert("ERRO", "Precisa de inserir uma palavra-passe para se registar.");
            else if(nome.equals("")) javafx.alert("ERRO", "Precisa de inserir um nome para se registar.");
            else if(data.equals("")) javafx.alert("ERRO", "Precisa de inserir uma data de nascimento para se registar.");
            else {
                if(controller.verificaData(data)){
                    if (!controller.validaRegisto(em, nome, user, pwd, data))  javafx.alert("ERRO", "O email inserido já está a ser usado por outro Apostador.");
                } else javafx.alert("ERRO", "Precisa de ter mais de 18 anos para usar este serviço.");
                javafx.endScene(e);
            }
        });

        layout.getChildren().addAll(lblNome, txt, lblEmail, email, lblUser, usertxt, lblPassword, passwordtxt, lblData, d, b);
        return new Scene(layout, 500, 400);
    }

    public Scene painelEstatisticasUtilizador() {
        VBox layout = javafx.makebox();
        return new Scene(layout, 500, 400);
    }

    public Scene painelConfigUtilizador() {
        VBox layout = javafx.makebox();

        listView = new ListView<>();
        listView.getItems().addAll(
                "Mudar palavra-passe", "Mudar email", "Mudar username"
        );

        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Button b1 = new Button("Escolher.");
        b1.setOnAction(e -> escolherMenuConfig());

        layout.getChildren().addAll(listView,b1);
        return new Scene(layout, 500, 400);
    }

    private void escolherMenuConfig() {

        String s = String.valueOf(listView.getSelectionModel().getSelectedItems());

        if(s.equals("[Mudar palavra-passe]")){
            javafx.makeWindow("Mudar palavra-passe", mudarPassword());
        }

        if(s.equals("[Mudar email]")){
            javafx.makeWindow("Mudar email", mudarEmail());
        }

        if(s.equals("[Mudar username]")){
            javafx.makeWindow("Mudar username", mudarUsername());
        }
    }

    public Scene mudarUsername() {
        VBox layout = javafx.makebox();

        usertxt = new TextField();
        Label lblUser = new Label("Novo Username");

        passwordtxt = new PasswordField();
        Label lblPassword = new Label("Password para confirmação");

        Button b = new Button("Confirmar.");
        b.setOnAction(e -> {
            String user = usertxt.getText();
            String pwd = passwordtxt.getText();

            if(user.equals("")) javafx.alert("ERRO", "Precisa de inserir um Username novo.");
            else if(pwd.equals("")) javafx.alert("ERRO", "Precisa de inserir a palavra-passe para confirmar a mudança.");
            else {
                if(!controller.mudarUsername(email, user, pwd)) javafx.alert("ERRO", "Infelizmente a mudança não foi efetuada.");
                javafx.endScene(e);
            }
        });

        layout.getChildren().addAll(lblUser, usertxt, lblPassword, passwordtxt,b);
        return new Scene(layout, 500, 400);
    }

    public Scene mudarEmail() {
        VBox layout = javafx.makebox();

        usertxt = new TextField();
        Label lblEmail = new Label("Novo Email");

        passwordtxt = new PasswordField();
        Label lblPassword = new Label("Password para confirmação");

        Button b = new Button("Confirmar.");
        b.setOnAction(e -> {
            String email = usertxt.getText();
            String pwd = passwordtxt.getText();

            if(email.equals("")) javafx.alert("ERRO", "Precisa de inserir um Email novo.");
            else if(pwd.equals("")) javafx.alert("ERRO", "Precisa de inserir a palavra-passe para confirmar a mudança.");
            else {
                if(!controller.mudarEmail(email, email, pwd)) javafx.alert("ERRO", "Infelizmente a mudança não foi efetuada.");

                javafx.endScene(e);
            }
        });

        layout.getChildren().addAll(lblEmail, usertxt, lblPassword, passwordtxt,b);
        return new Scene(layout, 500, 400);
    }

    public Scene mudarPassword() {
        VBox layout = javafx.makebox();

        PasswordField passwordtxt1 = new PasswordField();
        Label lblPassword1 = new Label("Nova palavra-passe");

        passwordtxt = new PasswordField();
        Label lblPassword = new Label("Palavra-passe antiga para confirmação");

        Button b = new Button("Confirmar.");
        b.setOnAction(e -> {
            String pwd1 = passwordtxt1.getText();
            String pwd = passwordtxt.getText();

            if(pwd1.equals("")) javafx.alert("ERRO", "Precisa de inserir uma palavra-passe nova.");
            else if(pwd.equals("")) javafx.alert("ERRO", "Precisa de inserir a palavra-passe antiga para confirmar a mudança.");
            else {
                if(!controller.mudarPassword(email, pwd1, pwd)) javafx.alert("ERRO", "Infelizmente a mudança não foi efetuada.");

                javafx.endScene(e);
            }
        });

        layout.getChildren().addAll(lblPassword1, passwordtxt1, lblPassword, passwordtxt,b);
        return new Scene(layout, 500, 400);
    }

    public Scene painelSaldoUtilizador() {
        VBox layout = javafx.makebox();

        List<String> moedas = controller.getMoedasDisponiveis();

        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(moedas);
        cb.setPromptText("Moedas aceites pela RasBet : ");
        cb.setOnAction(e -> {
            if(!controller.setMoeda(cb.getValue())) javafx.alert("Erro", "Moeda indisponivel.");
        });

        TextField valor = new TextField();
        Label lblValor = new Label("Quantia a depositar");

        String v = valor.getText();
        if(v.equals("")) javafx.alert("Erro", "Por favor insira a quantidade a depositar.");
        try {
            float quantia = Float.parseFloat(v);
            controller.depositarSaldo(email, quantia, controller.getMoeda());
        } catch (Exception e){
            javafx.alert("Erro", "A quantia precisa de ser um valor decimal.");
        }

        layout.getChildren().addAll(cb, lblValor, valor);
        return new Scene(layout, 500, 400);
    }

    public Scene painelCarteiraUtilizador() {
        VBox layout = javafx.makebox();
        return new Scene(layout, 500, 400);
    }

    public Scene painelDesportoUtilizador() {
        VBox layout = javafx.makebox();
        return new Scene(layout, 500, 400);
    }

    public Scene painelEventosUtilizador() {
        VBox layout = javafx.makebox();
        return new Scene(layout, 500, 400);
    }

    public Scene painelHistoricoUtilizador() {
        VBox layout = javafx.makebox();
        return new Scene(layout, 500, 400);
    }
}
