package Entidades;

import GUI.ApostadorObserver;

import java.util.ArrayList;
import java.util.List;

public class Subject {
    private List<ApostadorObserver> apostadores;

    private static Subject singleton = new Subject();

    public Subject() {
        this.apostadores = new ArrayList<>();
    }

    public static Subject getInstance(){
        return Subject.singleton;
    }

    public void addApostador(ApostadorObserver a){
        this.apostadores.add(a);
    }

    public void removeApostador(ApostadorObserver a){
        this.apostadores.remove(a);
    }

    public void notifyApostadores(){
        apostadores.forEach(y -> y.update());
    }

}
