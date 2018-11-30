package demos;

import commons.FileChunkClient;
import commons.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        test_object_grpc_with_crushmap();
    }

    public static void test_object_grpc_with_crushmap() {
        Loader loader = new Loader();
        CrushMap cluster_map = loader.sample_crush_map();
        Crush crush = new Crush();
        String file_name = "passwd";
        String data = "topsecret_passwordNSAnosee";
        byte[] data_bytes = data.getBytes(StandardCharsets.UTF_8);

        int pg = Crush.get_pg_id(file_name, cluster_map.total_pgs);
        System.out.println("Selected PG: " + pg);

        // this maps a placement group to OSD's
        List<CrushNode> selected_osds = crush.select_OSDs(cluster_map.get_root(), "" + pg);
        System.out.println("Selected OSD's: " + selected_osds);

        HashMap<String, ObjectStorageNode> hashMap = loader.get_osd_map();
        ObjectStorageNode primary = hashMap.get(selected_osds.get(0).nodeID + "");

        test_object_grpc(primary.ip, primary.port, file_name, data);
    }

    public static void test_object_grpc(String ip, int port, String file_name, String data) {
//        String file_name = "1337";
//        String data = "1234567890";
        byte[] data_bytes = data.getBytes(StandardCharsets.UTF_8);
        FileChunkClient client = new FileChunkClient(ip, port);

        System.out.println("Getting file with ID: " + file_name);
        byte[] get_result = client.getChunk(file_name);
        if (get_result.length > 0)
            System.out.println("File getting returned: " + new String(get_result, StandardCharsets.UTF_8));
        else
            System.out.println("File getting failed");


        System.out.println("Posting file: " + file_name + " | with data: " + data);
        boolean post_result = client.postChunk(data_bytes, file_name);
        System.out.println("File posting returned: " + post_result);

        System.out.println("Getting file with ID: " + file_name);
        get_result = client.getChunk(file_name);
        if (get_result.length > 0)
            System.out.println("File getting returned: " + new String(get_result, StandardCharsets.UTF_8));
        else
            System.out.println("File getting failed");
    }
}
