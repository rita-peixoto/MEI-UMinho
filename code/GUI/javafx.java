package GUI;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class javafx {

    public static VBox makebox(){
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20, 20, 20, 20));

        return layout;
    }

    public static void makeWindow(String title, Scene s){
        Stage w = new Stage();
        w.setTitle(title);
        w.setScene(s);
        w.show();
    }

    public static void endScene(ActionEvent e) {
        final Node source = (Node) e.getSource();
        final Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    public static Button sair(){
        Button b = new Button("Sair.");
        b.setOnAction(e -> {  javafx.endScene(e); });

        return b;
    }

    public static Button shutdown(){
        Button b = new Button("Sair.");
        b.setOnAction(e -> {  Platform.exit(); });

        return b;
    }

    public static void alert(String titulo, String mensagem){
        Stage w = new Stage();
        w.initModality(Modality.APPLICATION_MODAL);
        w.setTitle(titulo);
        w.setMinWidth(300);

        Label label = new Label();
        label.setText(mensagem);
        Button closeButton = new Button("Fechar.");
        closeButton.setOnAction(e -> w.close());

        VBox layout = new VBox(15);
        layout.getChildren().addAll(label, closeButton);
        layout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(layout);
        w.setScene(scene);
        w.showAndWait();
    }
}
