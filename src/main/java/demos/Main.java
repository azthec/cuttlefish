package demos;

import commons.FileChunkClient;
import commons.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import static commons.FileChunkUtils.get_object;
import static commons.FileChunkUtils.get_object_primary;
import static commons.FileChunkUtils.post_object;

public class Main {
    public static void main(String[] args) {
        test_object_grpc_with_crushmap();
    }

    public static void test_object_grpc_with_crushmap() {
        Loader loader = new Loader();
        CrushMap cluster_map = loader.sample_crush_map();
        get_object("1337", cluster_map);

        post_object("monsiour", "bogas".getBytes(), cluster_map);

    }

}
