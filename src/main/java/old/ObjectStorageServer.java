package old;

import commons.AtomixUtils;
import commons.CrushMap;
import commons.CrushNode;
import io.atomix.core.Atomix;
import io.atomix.core.list.DistributedList;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;


public class ObjectStorageServer {
    public static void main(String[] args) {
        String local_id = args[0];
        String local_ip = args[1];
        int local_port = Integer.parseInt(args[2]);

        AtomixUtils atomixUtils = new AtomixUtils();
        Atomix atomix = atomixUtils.getServer(local_id, local_ip, local_port).join();

        // as this OSD server is not in the servers list it acts as a clients
        DistributedList<CrushMap> distributed_crush_maps = get_maps(atomix);

        // wait for figo to register initial map
        while(distributed_crush_maps.size() == 0) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public static DistributedList<CrushMap> get_maps(Atomix atomix) {
        DistributedList<CrushMap> distributed_crush_maps = atomix.getList("maps");
        distributed_crush_maps.addListener(event -> {
            switch (event.type()) {
                case ADD:
                    System.out.println("Entry added: (" + event.element() + ")");
                    break;
                case REMOVE:
                    System.out.println("Entry removed: (" + event.element() +")");
                    break;
            }
        });

        return distributed_crush_maps;
    }
}
