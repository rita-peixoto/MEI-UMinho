package GUI;

import PL.Controller;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class Gestor implements IGestor{
    private ListView<String> listView;
    private Controller controller;

    public Gestor(Controller c){
        this.controller = c;
    }

    public Scene painelGestor() {
        VBox layout = javafx.makebox();

        listView = new ListView<>();
        listView.getItems().addAll(
                "Adicionar Evento", "Lista de Eventos Ativos", "Lista de Eventos ><", "Adicionar uma nova Moeda"
        );

        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Button b1 = new Button("Escolher.");
        b1.setOnAction(e -> escolherMenuGestor());

        Button b2 = new Button("Terminar Sessão.");
        b2.setOnAction(javafx::endScene);

        Button b3 = new Button("Atualizar.");
        b3.setOnAction(e -> {
            javafx.endScene(e);
            javafx.makeWindow("Menu do Gestor", painelGestor());
        });

        layout.getChildren().addAll(listView,b1,b2,b3);
        return new Scene(layout, 400, 400);
    }

    private void escolherMenuGestor(){
        String s = String.valueOf(listView.getSelectionModel().getSelectedItems());

        if(s.equals("[Adicionar Evento]")){
            javafx.makeWindow("Adicionar Evento - Gestor", painelGestor());
        }

        if(s.equals("[Lista de Eventos Ativos]")){
            javafx.makeWindow("Lista de Eventos Ativos", painelGestor());
        }

        if(s.equals("[Lista de Eventos ><]")){
            javafx.makeWindow("Lista de Eventos ><", painelGestor());
        }

        if(s.equals("[Adicionar uma nova Moeda]")){
            javafx.makeWindow("Painel da Moeda", painelMoeda());
        }
    }

    public Scene painelMoeda() {
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
            float ratio = (float) 0.0;
            float imposto = (float) 0.0;
            try {
                ratio = Float.parseFloat(ratiotxt.getText());
            } catch (Exception ex){
                javafx.alert("ERRO", "Precisa de inserir um valor para o ratio.");
            }
            try {
                imposto = Float.parseFloat(taxtxt.getText());
            } catch (Exception ex){
                javafx.alert("ERRO", "Precisa de inserir um valor para o imposto.");
            }

            if(nome.equals("")) javafx.alert("ERRO", "Precisa de inserir um nome para registar uma moeda.");
            if(token.equals("")) javafx.alert("ERRO", "Precisa de inserir um token para registar uma moeda.");
            else {
                if(!controller.validarMoeda(nome, token, ratio, imposto)) javafx.alert("ERRO", "Esse token já está registado.");
                javafx.endScene(e);
            }
        });

        layout.getChildren().addAll(lblnome, nometxt, lbltoken, tokentxt, lblratio, ratiotxt, lbltax, taxtxt, b);
        return new Scene(layout, 500, 400);
    }

    @Override
    public Scene painelAdicionarEvento() {
        VBox layout = javafx.makebox();

        TextField txtnome = new TextField();
        Label lblnome = new Label("Nome do Evento");

        TextField txtdata = new TextField();
        Label lbldata = new Label("Data do Evento");

        TextField txtequipa1 = new TextField();
        Label lblequipa1 = new Label("Equipa 1");

        TextField txtequipa2 = new TextField();
        Label lblequipa2 = new Label("Equipa 2");

        String nome = txtnome.getText();
        String data = txtdata.getText();
        String equipa1 = txtequipa1.getText();
        String equipa2 = txtequipa2.getText();

        Button b = new Button("Adicionar Evento.");
        b.setOnAction(e -> {
            if(nome.equals("")) javafx.alert("ERRO", "Precisa de introduzir um nome para criar um Evento.");
            else if(data.equals("")) javafx.alert("ERRO", "Precisa de introduzir uma data para criar um Evento.");
            else if(equipa1.equals("")) javafx.alert("ERRO", "Precisa de introduzir o nome da equipa 1 para criar um Evento.");
            else if(equipa2.equals("")) javafx.alert("ERRO", "Precisa de introduzir o nome da equipa 2 para criar um Evento.");
            else if(!controller.validaData(data)) javafx.alert("ERRO", "A data inserida é inválida.");
            else {
                controller.adicionarEvento(nome,data,equipa1,equipa2);
                javafx.endScene(e);
            }
        });

        layout.getChildren().addAll(lblnome, txtnome, lbldata, txtdata, lblequipa1, txtequipa1, lblequipa2, txtequipa2, b);
        return new Scene(layout, 500, 400);
    }

    @Override
    public Scene painelEventosAtivos() {
        VBox layout = javafx.makebox();

        listView = new ListView<>();
        listView.getItems().addAll(controller.getListaEventosAtivos());

        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        String s = String.valueOf(listView.getSelectionModel().getSelectedItems());

        Button b = new Button("Cancelar Evento.");
        b.setOnAction(e -> {
            controller.cancelarEvento(s);
        });

        Button b1 = new Button("Suspender Evento.");
        b1.setOnAction(e -> {
            controller.supenderEvento(s);
        });

        return evento_butoes(layout, s, b, b1);
    }

    @Override
    public Scene painelAdicionarAposta(String s) {
        VBox layout = javafx.makebox();

        TextField txtaposta = new TextField();
        Label lblaposta = new Label("Descrição da Aposta");

        TextField txtodd = new TextField();
        Label lblodd = new Label("Odd da Aposta");

        String aposta = txtaposta.getText();
        String sodd = txtodd.getText();

        Button b = new Button("Adicionar aposta.");
        b.setOnAction(e -> {
            try{
                if(aposta.equals("")) javafx.alert("ERRO", "Precisa de inserir uma Descrição na aposta");
                else if(sodd.equals("")) javafx.alert("ERRO", "Precisa de inserir uma Odd na aposta");
                else {
                    float odd = Float.parseFloat(sodd);
                    controller.adicionarApostaIn(s,aposta,odd);
                    javafx.endScene(e);
                }
            } catch (Exception ex){
                javafx.alert("Erro", "A odd precisa de ser um valor decimal.");
            }
        });

        layout.getChildren().addAll(lblaposta, txtaposta, lblodd, txtodd, b);
        return new Scene(layout, 500, 400);
    }

    @Override
    public Scene painelEventos() {
        VBox layout = javafx.makebox();

        listView = new ListView<>();
        listView.getItems().addAll(controller.getListaEventos());

        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        String s = String.valueOf(listView.getSelectionModel().getSelectedItems());

        Button b = new Button("Cancelar Evento.");
        b.setOnAction(e -> {
            controller.cancelarEvento(s);
        });

        Button b1 = new Button("Ativar Evento.");
        b1.setOnAction(e -> {
            controller.ativarEvento(s);
        });

        return evento_butoes(layout, s, b, b1);
    }

    private Scene evento_butoes(VBox layout, String s, Button b, Button b1) {
        Button b2 = new Button("Adicionar uma aposta ao Evento.");
        b2.setOnAction(e -> {
            javafx.makeWindow("Evento - " + s, painelAdicionarAposta(s));
        });

        Button b3 = new Button("Finalizar Evento.");
        b3.setOnAction(e -> {
            javafx.makeWindow("Evento - " + s, painelFinalizarEvento(s));
        });

        Button b4 = new Button("Editar Evento.");
        b4.setOnAction(e -> {
            javafx.makeWindow("Evento - " + s, painelEditarEvento(s));
        });

        layout.getChildren().addAll(listView, b, b1, b2, b3, b4);
        return new Scene(layout, 500, 400);
    }

    private Scene painelFinalizarEvento(String s) {
        VBox layout = javafx.makebox();

        return new Scene(layout, 500, 400);
    }

    @Override
    public Scene painelEditarEvento(String s) {
        VBox layout = javafx.makebox();

        return new Scene(layout, 500, 400);
    }
}
