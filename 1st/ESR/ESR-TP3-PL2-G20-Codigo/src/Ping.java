import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.net.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Ping {
    private Timer timer;
    private Map<InetAddress,Integer> pings;
    private ReentrantLock lock;
    private Condition cond;

    public Ping(int seconds, Map<InetAddress,Integer> pings, ReentrantLock lock, Condition cond) {
        timer = new Timer();
        timer.schedule(new RemindTask(), seconds*1000); // schedule the task
        this.pings = pings;
        this.lock = lock;
        this.cond = cond;
    }

    class RemindTask extends TimerTask {
        public void run() {

            int max = maxValue();
            int min = max;

            InetAddress client_min = null;

            try {
                lock.lock();

                for (InetAddress p : pings.keySet()) {
                    if (pings.get(p) < min) {
                        min = pings.get(p);
                        client_min = p;
                    }
                }

                if (max - min >= 3) {
                    pings.put(client_min,-1);
                    cond.signalAll(); // notificar o nodo que tem de DESATIVAR conexao
                }

            } finally {
                lock.unlock();
            }


            new Ping(3,pings, lock, cond); //Terminate the timer thread

        }
    }


    public int maxValue() { //obtém o número maximo de pings
        int max = 0;

        for (InetAddress p : pings.keySet()) {
            if(pings.get(p) > max){
                max = pings.get(p);
            }
        }
        return max;
    }


}

