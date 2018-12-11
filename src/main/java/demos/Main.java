package demos;

import commons.FileChunkClient;
import commons.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static commons.FileChunkUtils.*;

public class Main {
    public static void main(String[] args) {
        test_file_posting();
    }

    public static void test_object_grpc_with_crushmap() {
        Loader loader = new Loader();
        CrushMap cluster_map = loader.sample_crush_map();
        get_object("1337", cluster_map);

        post_object("monsiour", "bogas".getBytes(), cluster_map);

    }

    public static void test_file_splitting() {
        File input = new File("/home/azthec/IdeaProjects/cuttlefish/storage/toogood.mp4");
        try {
            byte[][] result = fileToByteArrays(input);

            byteArraysToFile(result, new File("/home/azthec/IdeaProjects/cuttlefish/storage/toobyted.mp4"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void test_file_posting() {
        Loader loader = new Loader();
        CrushMap cluster_map = loader.sample_crush_map();
        MetadataTree meta_tree = loader.sample_metadata_tree();
        try {
            post_file(
                    "/home/azthec/IdeaProjects/cuttlefish/storage/toogood.mp4",
                    "/test.mp4",
                    cluster_map,
                    meta_tree
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
