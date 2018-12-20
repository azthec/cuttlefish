package commons;

import io.atomix.core.Atomix;
import io.atomix.core.AtomixBuilder;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;
import io.atomix.storage.StorageLevel;
import io.atomix.utils.net.Address;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AtomixUtils {

    // todo load List<String> servers from config file
    // local_id not in server list -> client
    public CompletableFuture<Atomix> getServer(String local_id, String local_ip,
                                                      int local_port,
                                                      List<String> servers) {
        AtomixBuilder builder = Atomix.builder();
        builder.withMemberId(local_id)
                .withAddress(local_ip, local_port)
                .withMulticastEnabled()
                .withMulticastAddress(new Address("230.4.20.69", 8008))
                .withManagementGroup(RaftPartitionGroup.builder("system")
                        .withNumPartitions(1)
                        .withDataDirectory(new File("mngdir", local_id))
                        .withStorageLevel(StorageLevel.DISK)
                        .withMembers(servers)
                        .build())
                .addPartitionGroup(RaftPartitionGroup.builder("data")
                        .withNumPartitions(1)
                        .withDataDirectory(new File("datadir", local_id))
                        .withStorageLevel(StorageLevel.DISK)
                        .withMembers(servers)
                        .build());
        Atomix atomix = builder.build();

        // atomix.getMembershipService().addListener(event -> System.out.println(event.toString()));

        System.out.println("Starting node: " + local_id + " @ Port: " + local_port + ".");
        return CompletableFuture.supplyAsync(() -> {
            atomix.start().join();
            return atomix;
        });
    }

    public static String getRaftLeader(Atomix atomix, String partition_group_name ) {
        return atomix
                .getPartitionService()
                .getPartitionGroup(partition_group_name)
                .getPartition("1")
                .primary()
                .toString();
    }
}
