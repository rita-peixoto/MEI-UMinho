package GUI;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class Scenes {
    private ListView<String> listView;
    private TextField usertxt, txt;
    private PasswordField passwordtxt;

    public Scene menu() {
        VBox layout = javafx.makebox();

        listView = new ListView<String>();
        listView.getItems().addAll(
                "Registar Utilizador", "Login de Utilizador", "Visualizar Apostas", "Login de Gestor"
        );

        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Button b1 = new Button("Escolher.");
        b1.setOnAction(e -> escolherMenu());

        Button b2 = javafx.shutdown();

        layout.getChildren().addAll(listView,b1,b2);
        return new Scene(layout, 400, 400);
    }

    private void escolherMenu(){
        String s = String.valueOf(listView.getSelectionModel().getSelectedItems());

        if(s.equals("[Registar Utilizador]")){
            javafx.makeWindow("Registo - RASBET", registarUtilizador());
        }

        if(s.equals("[Login de Gestor]")){
            javafx.makeWindow("Log In - Entidades.Gestor", login(true));
        }

        if(s.equals("[Login de Utilizador]")){
            javafx.makeWindow("Log In - Utilizador", login(false));
        }

        if(s.equals("[Visualizar Apostas]")){
            javafx.makeWindow("Apostas - RASBET", vizualizarApostas());
        }
    }

    private void escolherMenuUtilizador(String codID){
        String s = String.valueOf(listView.getSelectionModel().getSelectedItems());

        if(s.equals("[Historico de Apostas]")){
            javafx.makeWindow("Historico de Apostas", login(true));
        }

        if(s.equals("[Lista de Eventos]")){
            javafx.makeWindow("Lista de Eventos", login(false));
        }

        if(s.equals("[Desportos]")){
            javafx.makeWindow("Desportos - RASBET", vizualizarApostas());
        }
    }

    private void escolherMenuGestor(String codID){
        String s = String.valueOf(listView.getSelectionModel().getSelectedItems());

        if(s.equals("[Adicionar Entidades.Evento]")){
            javafx.makeWindow("Adicionar Entidades.Evento - Entidades.Gestor", login(true));
        }

        if(s.equals("[Lista de Eventos Ativos]")){
            javafx.makeWindow("Lista de Eventos Ativos", login(false));
        }

        if(s.equals("[Lista de Eventos ><]")){
            javafx.makeWindow("Lista de Eventos ><", vizualizarApostas());
        }

        if(s.equals("[Adicionar uma nova Moeda]")){
            javafx.makeWindow("Painel da Moeda", painelMoeda());
        }
    }

    private Scene painelMoeda() {
        VBox layout = javafx.makebox();

        TextField nometxt = new TextField();
        Label lblnome = new Label("Nome da Moeda");

        TextField tokentxt = new TextField();
        Label lbltoken = new Label("Token da Moeda");

        TextField ratiotxt = new TextField();
        Label lblratio = new Label("Ratio da Moeda em relação ao Euro");

        TextField taxtxt = new TextField();
        Label lbltax = new Label("Imposto sobre o cambio da Moeda");

        Button b = new Button("Adicionar esta moeda ao cambio.");
        b.setOnAction(e -> {
            String nome = nometxt.getText();
            String token = tokentxt.getText();
            try {
                Float ratio = Float.valueOf(ratiotxt.getText());
            } catch (Exception ex){
                javafx.alert("ERRO", "Precisa de inserir um valor para o ratio.");
            }
            try {
                Float imposto = Float.valueOf(taxtxt.getText());
            } catch (Exception ex){
                javafx.alert("ERRO", "Precisa de inserir um valor para o imposto.");
            }

            if(nome.equals("")) javafx.alert("ERRO", "Precisa de inserir um nome para registar uma moeda.");
            if(token.equals("")) javafx.alert("ERRO", "Precisa de inserir um token para registar uma moeda.");
            else {
                // new Moeda(nome, token, ratio, imposto);
                javafx.endScene(e);
            }
        });

        layout.getChildren().addAll(lblnome, nometxt, lbltoken, tokentxt, lblratio, ratiotxt, lbltax, taxtxt, b);
        return new Scene(layout, 500, 400);
    }

    private Scene registarUtilizador() {
        VBox layout = javafx.makebox();

        txt = new TextField();
        Label lblNome = new Label("Nome Completo");

        usertxt = new TextField();
        Label lblUser = new Label("Username");

        passwordtxt = new PasswordField();
        Label lblPassword = new Label("Password");

        Button b = new Button("Registar.");
        b.setOnAction(e -> {
            String nome = txt.getText();
            String user = usertxt.getText();
            String pwd = passwordtxt.getText();

            if(user.equals("")) javafx.alert("ERRO", "Precisa de inserir um email para se registar.");
            if(pwd.equals("")) javafx.alert("ERRO", "Precisa de inserir uma palavra-passe para se registar.");
            if(nome.equals("")) javafx.alert("ERRO", "Precisa de inserir um nome para se registar.");
            else {
                // c.validaRegisto(nome, user, pwd);
                javafx.endScene(e);
            }
        });

        layout.getChildren().addAll(lblNome, txt, lblUser, usertxt, lblPassword, passwordtxt, b);
        return new Scene(layout, 500, 400);
    }

    private Scene login(boolean isGestor){
        VBox layout = javafx.makebox();

        usertxt = new TextField();
        Label lblUser = new Label("Username");

        passwordtxt = new PasswordField();
        Label lblPassword = new Label("Password");

        Button b = new Button("LogIn.");
        b.setOnAction(e -> {
            String user = usertxt.getText();
            String pwd = passwordtxt.getText();

            if(user.equals("")) javafx.alert("ERRO", "Precisa de inserir um Código ID para se conectar.");
            if(pwd.equals("")) javafx.alert("ERRO", "Precisa de inserir uma palavra-passe para se conectar.");
            else {
                login(user, pwd, isGestor);
                javafx.endScene(e);
            }
        });

        layout.getChildren().addAll(lblUser, usertxt, lblPassword, passwordtxt, b);
        return new Scene(layout, 500, 400);
    }

    private Scene vizualizarApostas(){
        VBox layout = javafx.makebox();

        Button b1 = new Button("Log in.");
        // b1.setOnAction(e -> fun());

        Button b2 = javafx.sair();

        layout.getChildren().addAll(b1,b2);
        return new Scene(layout, 400, 400);
    }

    private void login(String codID, String pwd, boolean isGestor){
        if(isGestor){
            if(true /* iniciar sessao do gestor */ ){
                javafx.makeWindow("Menu do Entidades.Gestor", painelGestor(codID));
            } else javafx.alert("ERRO", "Falha ao iniciar a sessão, verifique os seus dados.");
        } else {
            if(true /* iniciar sessao do utilizador */ ){
                javafx.makeWindow("Menu de Utilizador", painelUtilizador(codID));
            } else javafx.alert("ERRO", "Falha ao iniciar a sessão, verifique os seus dados.");
        }
    }

    private Scene painelUtilizador(String codID) {
        VBox layout = javafx.makebox();

        listView = new ListView<>();
        listView.getItems().addAll(
                "Historico de Apostas", "Lista de Eventos", "Desportos"
        );

        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Button b1 = new Button("Escolher.");
        b1.setOnAction(e -> escolherMenuUtilizador(codID));

        Button b2 = new Button("Terminar Sessão.");
        b2.setOnAction(e -> {
            /* logoutUtilizador(codID); */
            javafx.endScene(e);
        });

        Button b3 = new Button("Atualizar.");
        b3.setOnAction(e -> {
            javafx.endScene(e);
            javafx.makeWindow("Menu do Utilizador", painelUtilizador(codID));
        });

        layout.getChildren().addAll(listView,b1,b2,b3);
        return new Scene(layout, 400, 400);
    }

    private Scene painelGestor(String codID) {
        VBox layout = javafx.makebox();

        listView = new ListView<>();
        listView.getItems().addAll(
                "Adicionar Entidades.Evento", "Lista de Eventos Ativos", "Lista de Eventos ><", "Adicionar uma nova Moeda"
        );

        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Button b1 = new Button("Escolher.");
        b1.setOnAction(e -> escolherMenuGestor(codID));

        Button b2 = new Button("Terminar Sessão.");
        b2.setOnAction(e -> {
            /* logoutGestor(codID); */
            javafx.endScene(e);
        });

        Button b3 = new Button("Atualizar.");
        b3.setOnAction(e -> {
            javafx.endScene(e);
            javafx.makeWindow("Menu do Entidades.Gestor", painelGestor(codID));
        });

        layout.getChildren().addAll(listView,b1,b2,b3);
        return new Scene(layout, 400, 400);
    }
}
