package commons;

import io.atomix.core.Atomix;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class HeartbeatManager implements Runnable {

    Atomix atomix;
    String local_id;
    String pg_name;
    ScheduledExecutorService executorService;
    ScheduledFuture<?> heartbeat;

    public HeartbeatManager(Atomix atomix, String local_id, String pg_name) {
        this.atomix = atomix;
        this.local_id = local_id;
        this.pg_name = pg_name;
        executorService = Executors.newSingleThreadScheduledExecutor();
        heartbeat = executorService.scheduleAtFixedRate(new Heartbeat(atomix),
                0, 2500,
                TimeUnit.MILLISECONDS
        );
    }


    @Override
    public void run() {
        String leader = AtomixUtils.getRaftLeader(atomix, pg_name);
        // If local server is not the leader and a heartbeat is running, cancel it
        if (!local_id.equals(leader) && !heartbeat.isCancelled()) {
            heartbeat.cancel(false);
        // If local server is the leader but a heartbeat is not running, start it
        } else if (local_id.equals(leader) && heartbeat.isCancelled()) {
            heartbeat =  executorService.scheduleAtFixedRate(new Heartbeat(atomix),
                    0, 30,
                    TimeUnit.SECONDS
            );
        } else {
            // we are in an ok state, do nothing
        }
    }
}