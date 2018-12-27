package commons;

import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.Atomix;
import io.atomix.core.AtomixBuilder;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;
import io.atomix.storage.StorageLevel;
import io.atomix.utils.net.Address;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AtomixUtils {

    public CompletableFuture<Atomix> getServer(String local_id, String local_ip,
                                                      int local_port) {
        // Raft requires a static membership list
        List<String> servers = Loader.loadServerNames();
        HashMap<String, String> monitors = Loader.sample_monitors();
        AtomixBuilder builder = Atomix.builder();
        builder.withMemberId(local_id)
                .withAddress(local_ip, local_port)
                .withMulticastEnabled()
                .withMulticastAddress(new Address("230.4.20.69", 8008))
//                .withClusterId("boladouro")
//                .withMembershipProvider(BootstrapDiscoveryProvider.builder()
//                        .withNodes(
//                                Node.builder()
//                                        .withId("figo")
//                                        .withAddress(monitors.get("figo"))
//                                        .build(),
//                                Node.builder()
//                                        .withId("messi")
//                                        .withAddress(monitors.get("messi"))
//                                        .build(),
//                                Node.builder()
//                                        .withId("ronaldo")
//                                        .withAddress(monitors.get("ronaldo"))
//                                        .build())
//                        .build())
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
