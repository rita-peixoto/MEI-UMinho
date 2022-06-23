import Entidades.Evento;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

public class Controller {

    public RasBetModel rasBetModel;
    public Scanner sc;

    //id do apostador que fez login . -1 qd nao fez.
    public String apostador_sessao;
    //id do gestor que fez login . -1 qd nao fez.
    public String gestor_sessao;
    public Controller(RasBetModel rasBetModel) {
        this.rasBetModel = rasBetModel;
        this.sc= new Scanner(System.in);
        this.apostador_sessao = "-1";
        this.gestor_sessao = "-1";
    }

    public Controller() {
        this.sc= new Scanner(System.in);
        this.apostador_sessao = "-1";
        this.gestor_sessao = "-1";

    }

    //FUNCOES AUXILIARES
    //FUNCOES AUXILIARES
    //FUNCOES AUXILIARES
    //FUNCOES AUXILIARES
    //FUNCOES AUXILIARES


    //Cada "menu" do controller estará associado a 1 inteiro.
    /*
    1  = main_menu

    */
    //Esta funcao chama esse menu.
    public void menu_mapper(int menu){
        if (menu == 0){
            menu_initializer();
        }
        if (menu == 1){
            main_menu();
        }
    }

    //PEDEM UM email e uma pass e verificam.
    //return false ate estar implementado com o model.
    //atualiza String gestor/apostador do controller
    public boolean verificarLoginApostador(){
        System.out.println("EmaiL: ");
        String email = this.sc.nextLine();
        System.out.println("Pass: ");
        String pass = this.sc.nextLine();
        this.apostador_sessao = ""; //RasBetModel.get(email)
        return true;
    }

    public boolean verificarLoginGestor(){
        System.out.println("EmaiL: ");
        String email = this.sc.nextLine();
        System.out.println("Pass: ");
        String pass = this.sc.nextLine();
        this.apostador_sessao = ""; //RasBetModel.get(email)
        return true;
    }

    //Selecionar um evento
    public boolean selecionarEvento(String evento){
        return true;
    }

    //Selecionar uma aposta
    public boolean selecionarAposta(String aposta){
        return true;
    }

    public boolean validarAposta(String idApostador, Double quantia){
        return true;
    }

    //calcular os ganhos de uma aposta
    public Double calcularGanhos(String apostaID, double quantia){
        return -10000000.0;
    }

    /*
gera uma lista de funções que devolvem um valor true or false para associar o input do utilizador a um menu ex:
y -> y.equals("1"),y -> y.equals("2"),y -> y.equals("2") etc.
o ultimo elementa da lista é sempre o de saida
* */
    List<Function<Object,Boolean>> generate_menu_checks(int n_options){
        List<Function<Object,Boolean>> checks = new ArrayList<>();
        for (int i = 0; i < n_options; i++) {
            int finalI1 = i;
            Function<Object,Boolean> f0 = (y -> ((String) y).equals("" + finalI1 + ""));
            checks.add(f0);
        }
        return checks;
    }



    //INICIALIZAR O MENU
    public void menu_initializer(){

    }
    //MENU PRINCIPAL (LOGIN)
    public void main_menu(){
        this.apostador_sessao = "-1";
        this.gestor_sessao = "-1";
        String [] opts = {"0.Login Apostador","1.Login Gestor","2.Ver Apostas","3.Registar","4.Sair"};
        /*
        gera uma lista de funções ex:
        y -> y.equals("1"),y -> y.equals("2"),y -> y.equals("2") etc.
        o ultimo elementa da lista é sempre o de saida
        * */
        List<Function<Object,Boolean>> checks = generate_menu_checks(5);
        int i = 0;
        do {
            Arrays.stream(opts).forEach(y -> System.out.println(y));
            String input = this.sc.nextLine();

           // boolean b =  checks.get(i).apply(input);
            //para quando encontra uma opççao valida ou quando nao ha nenhuuma
            for ( i = 0; i < checks.size() && !checks.get(i).apply(input); i++) {
                ;
            }
            System.out.println("i: "+i);
            switch (i){
                case (0):
                    System.out.println("APOSTADOR");
                    if (verificarLoginApostador()){
                        System.out.println("login verificado");
                        apostador_main_menu();
                    }
                    else
                        System.out.println("oops - apostador");
                    break;
                case (1):
                    System.out.println("GESTOR");
                    if (verificarLoginGestor()){
                        System.out.println("login verificado");
                        gestor_main_menu();
                    }
                    else
                        System.out.println("oops - gestor");
                    break;
                case (2):
                    System.out.println("APOSTS");
                    break;
                case (3):
                    System.out.println("Registar");
                    break;
                case (4):
                    System.out.println("SAIR");
                    i = -1;
                    break;
            }

        }
        while (i != -1);
    }



    // MENUS RELATIVOS AOS APOSTADORES
    // MENUS RELATIVOS AOS APOSTADORES
    // MENUS RELATIVOS AOS APOSTADORES
    // MENUS RELATIVOS AOS APOSTADORES

    public void apostador_main_menu(){
        String [] opts = {"0.Escolher evento","1.Sair"};
        System.out.println("Aqui é preciso haver uma maneira de navegar eventos");
        //this.rasBetModel.getEventList();
        List<Function<Object,Boolean>> checks = generate_menu_checks(2);
        int i = 0;
        do {
            Arrays.stream(opts).forEach(y -> System.out.println(y));
            String input = this.sc.nextLine();

            // boolean b =  checks.get(i).apply(input);
            //para quando encontra uma opççao valida ou quando nao ha nenhuuma
            for ( i = 0; i < checks.size() && !checks.get(i).apply(input); i++) {
                ;
            }
            switch (i){
                case (0):
                    System.out.println("Selecionar evento");
                    String evento = sc.nextLine();
                    if (selecionarEvento(evento)) {
                        System.out.println("evento selecionada");
                        apostador_escolher_aposta();
                    }
                    else
                        System.out.println("evento invalido");
                    break;
                case (1):
                    //System.out.println("GESTOR");
                    i = -1;
                    break;
            }

        }
        while (i != -1);
    }

    public void apostador_escolher_aposta(){
        String [] opts = {"0.Escolher aposta","1.Sair"};
        System.out.println("Aqui é preciso haver uma maneira de ver apostas");
        //this.rasBetModel.getEventList();
        List<Function<Object,Boolean>> checks = generate_menu_checks(2);
        int i = 0;
        do {
            Arrays.stream(opts).forEach(y -> System.out.println(y));
            String input = this.sc.nextLine();

            // boolean b =  checks.get(i).apply(input);
            //para quando encontra uma opççao valida ou quando nao ha nenhuuma
            for ( i = 0; i < checks.size() && !checks.get(i).apply(input); i++) {
                ;
            }
            switch (i){
                //menu dentru de um menu
                case 0: {
                    int j;
                    int k;
                    do {
                        List<Function<Object, Boolean>> checks2 = generate_menu_checks(2);
                        System.out.println("Prima 0 para sair ou 1. para indicar a quantia q quer apostar: ");
                        String aposta = sc.nextLine();

                        for (k = 0; k < checks2.size() && !checks.get(k).apply(aposta); k++)
                            ;

                        switch (k) {
                            case 0:
                                k = -1;
                                break;
                            case 1:
                                //escolher id da APOSTA
                                System.out.println("Indique a aposta:");
                                String id = sc.nextLine();
                                boolean aposta_existe = true;//this.rasBetModel.getAposta(id);
                                //validar aposta:
                                if (aposta_existe) {
                                    System.out.println("Indique a quantia:");
                                    input = sc.nextLine();
                                    Double valor = Double.valueOf(aposta);
                                    if (validarAposta(this.apostador_sessao, valor)) {
                                        System.out.println("Aposta submetida.");
                                        Double ganhos = calcularGanhos(input, valor);
                                        System.out.println("Ganhos: " + ganhos);
                                    } else
                                        System.out.println("quantia invalida");
                                    k = -1;
                                }
                        }
                    } while (k != -1);
                    break;
                }
                case 1:
                    i = -1;
                    break;
            }
        }
        while (i != -1);
    }

    // MENUS RELATIVOS AOS GESTORES
    // MENUS RELATIVOS AOS GESTORES
    // MENUS RELATIVOS AOS GESTORES
    // MENUS RELATIVOS AOS GESTORES

    public void gestor_main_menu(){
        String [] opts = {"0.Adicionar evento","1.Atualizar apostas","2.Finalizar evento","3.Atualizar Odds","4. Sair"};
        //this.rasBetModel.getEventList();
        List<Function<Object,Boolean>> checks = generate_menu_checks(5);
        int i = 0;
        do {
            Arrays.stream(opts).forEach(y -> System.out.println(y));
            String input = this.sc.nextLine();
            // boolean b =  checks.get(i).apply(input);
            //para quando encontra uma opççao valida ou quando nao ha nenhuuma
            for ( i = 0; i < checks.size() && !checks.get(i).apply(input); i++) {
                ;
            }
            switch (i){
                case(0):
                    System.out.println("Escolher parametros do evento:");
                    //aqui é preciso escolher os parametros do evento
                    Evento e = new Evento();
                    //this.rasBetModel.adicionarEvento(e);
                    break;
                case (1):
                    gestor_ver_eventos(1);
                    break;
                case(2):
                    gestor_ver_eventos(2);
                    break;
                case(3):
                    gestor_ver_eventos(3);
                    break;
                case (4):
                    //System.out.println("GESTOR");
                    i = -1;
                    break;
            }

        }
        while (i != -1);
    }
/* funcao que mostra eventos ao gestor , e dependendo do inteiro permite-lhe as seguintes funcionalidades:
int tipo: 1.Atualizar apostas
          2.Finalizar evento
          3.Atualizar Odds
*
* */
    public void gestor_ver_eventos(int tipo){
        String [] opts = {"0.Escolher evento","1.Sair"};
        System.out.println("Aqui é preciso haver uma maneira de navegar eventos");
        //this.rasBetModel.getEventList();
        List<Function<Object,Boolean>> checks = generate_menu_checks(2);
        int i = 0;
        do {
            Arrays.stream(opts).forEach(y -> System.out.println(y));
            String input = this.sc.nextLine();

            // boolean b =  checks.get(i).apply(input);
            //para quando encontra uma opççao valida ou quando nao ha nenhuuma
            for ( i = 0; i < checks.size() && !checks.get(i).apply(input); i++) {
                ;
            }
            switch (i){
                case (0):
                    System.out.println("Selecionar evento");
                    String evento = sc.nextLine();
                    if (selecionarEvento(evento)) {
                        System.out.println("evento selecionada");
                        switch (tipo){
                            case 1:
                                //1.Atualizar apostas
                                String [] opts2 = {"0.mudar para suspenso","1.mudar para aberto", "2.cancelar"};
                                List<Function<Object,Boolean>> checks1 = generate_menu_checks(3);
                                int j;
                                do{
                                    Arrays.stream(opts2).forEach(y -> System.out.println(y));
                                    input = this.sc.nextLine();
                                    for ( j = 0; j < checks.size() && !checks.get(j).apply(input); j++) {
                                        ;
                                    }
                                    switch (j){
                                        case 0:
                                           // this.rasBetModel.atualizarEvento(evento,"Suspenso");
                                            System.out.println("Evento suspenso.");
                                            j = -1;
                                            break;
                                        case 1:
                                            // this.rasBetModel.atualizarEvento(evento,"Aberto");
                                            System.out.println("Evento aberto.");
                                            j = -1;
                                            break;
                                        case 2:
                                            j = -1;
                                            break;
                                    }
                                }while(j != -1);
                                break;
                            case 2:
                                //          2.Finalizar evento
                                String [] opts3 = {"0.finalisar","1.cancelamento inesperado", "2.cancelar"};
                                List<Function<Object,Boolean>> checks2 = generate_menu_checks(3);
                                int k;
                                do{
                                    Arrays.stream(opts3).forEach(y -> System.out.println(y));
                                    input = this.sc.nextLine();
                                    for ( k = 0; k < checks2.size() && !checks2.get(k).apply(input); k++) {
                                        ;
                                    }
                                    switch (k){
                                        case 0:
                                            //estas partes estao inacabadas
                                            //indicar resultado final?
                                            System.out.println("Evento finalisado.");
                                            break;
                                        case 1:
                                            // evento cancelado -> indicar razao?
                                            System.out.println("Evento cancelado.");
                                            break;
                                        case 2:
                                            k = -1;
                                            break;
                                    }
                                }while(k != -1);
                                break;
                            case 3:
                                //          3.Atualizar Odds
                                gestor_escolher_resultado(evento);

                        }
                    }
                    else
                        System.out.println("evento invalido");
                    break;
                case (1):
                    //System.out.println("GESTOR");
                    i = -1;
                    break;
            }
        }
        while (i != -1);
    }

    public void gestor_escolher_resultado(String evento){
        String [] opts = {"0.Escolher rsultado","1.Sair"};
        System.out.println("Aqui é preciso haver uma maneira de ver resultados");
        //this.rasBetModel.getEventList();
        List<Function<Object,Boolean>> checks = generate_menu_checks(2);
        int i = 0;
        do {
            Arrays.stream(opts).forEach(y -> System.out.println(y));
            String input = this.sc.nextLine();

            // boolean b =  checks.get(i).apply(input);
            //para quando encontra uma opççao valida ou quando nao ha nenhuuma
            for ( i = 0; i < checks.size() && !checks.get(i).apply(input); i++) {
                ;
            }
            switch (i){
                //menu dentru de um menu
                case 0: {
                    int j;
                    int k;
                    do {
                        List<Function<Object, Boolean>> checks2 = generate_menu_checks(2);
                        System.out.println("Prima 0 para sair ou 1. para indicar o resultado a que quer alterar a odd: ");
                        String aposta = sc.nextLine();

                        for (k = 0; k < checks2.size() && !checks.get(k).apply(aposta); k++)
                            ;

                        switch (k) {
                            case 0:
                                k = -1;
                                break;
                            case 1:
                                //escolher id da APOSTA
                                System.out.println("Indique o id do resultado:");
                                String id = sc.nextLine();
                                boolean aposta_existe = true;//this.rasBetModel.getEvento(evento).getResultado(id);
                                //validar aposta:
                                if (aposta_existe) {
                                    System.out.println("Indique a nova odd:");
                                    input = sc.nextLine();
                                    Double valor = Double.valueOf(aposta);
                                    if (validarAposta(this.apostador_sessao, valor)) {
                                        //this.rasBetModel.atualizarOdd(id,valor);
                                        System.out.println("odd submetida.");
                                    } else
                                        System.out.println("quantia invalida");
                                    k = -1;
                                }
                        }
                    } while (k != -1);
                    break;
                }
                case 1:
                    i = -1;
                    break;
            }
        }
        while (i != -1);
    }





//MAIN TESTES
    public static void main(String []args){
        Controller c = new Controller();
        c.main_menu();
    }


}
