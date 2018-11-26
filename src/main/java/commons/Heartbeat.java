package commons;

import io.atomix.core.Atomix;

public class Heartbeat implements Runnable {

    Atomix atomix;
    boolean booting;

    public Heartbeat(Atomix atomix) {
        this.atomix = atomix;
        this.booting = true;
    }


    @Override
    public void run() {
        // TODO may be unecessary, remove later date
        if (booting) {
            booting = false;
            return;
        }
        String leader = atomix
                .getPartitionService()
                .getPartitionGroup("system")
                .getPartition("1")
                .primary()
                .toString();
        System.out.println(leader);
    }
}
