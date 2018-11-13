package monitor;

import commons.Map;
import commons.Node;
import commons.Crush;

import java.math.BigInteger;
import java.util.*;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

public class Rados {
    // boot up system
    public static void main(String[] args) {
        commons.Map cluster_map = new Map();
        Random random = new Random();

        // cluster_map.get_root().size = 3;

        Node b001 = new Node(110, "row", -1, false, false);
        b001.add(new Node(0, "leaf", 1000, true, true));
        b001.add(new Node(1, "leaf", 1000, true, true));
        b001.add(new Node(2, "leaf", 1000, true, true));
        b001.add(new Node(3, "leaf", 1000, true, false));
        b001.size = 1;
        Node b010 = new Node(111, "row", -1, false, false);
        b010.add(new Node(4, "leaf", -1, true, true));
        b010.add(new Node(5, "leaf", -1, true, true));
        b010.add(new Node(6, "leaf", -1, true, true));
        b010.add(new Node(7, "leaf", 1000, true, false));
        b010.size = 1;
        Node b100 = new Node(112, "row", -1, false, false);
        b100.add(new Node(8, "leaf", -1, true, true));
        b100.add(new Node(9, "leaf", -1, true, true));
        b100.add(new Node(10, "leaf", -1, true, true));
        b100.add(new Node(11, "leaf", 1000, true, false));
        b100.size = 1;
        Node b111 = new Node(112, "row", -1, false,false);
        b111.add(new Node(12, "leaf", -1, true, false));
        b111.add(new Node(13, "leaf", -1, true, false));
        b111.add(new Node(14, "leaf", -1, true, false));
        b111.add(new Node(15, "leaf", 1000, true, false));
        b111.size = 4;

        cluster_map.get_root().add(b001);
        cluster_map.get_root().add(b010);
        cluster_map.get_root().add(b100);
        cluster_map.get_root().add(b111);

        cluster_map.get_root().print(0);

        Crush crush = new Crush();
        crush.set_bucket_size(4);

        test_select_randomness(crush, cluster_map.get_root());


        // System.out.println(crush.select_OSDs(cluster_map.get_root(), "1337"));


    }

    public static void test_select_randomness(Crush crush, Node root) {
        int[] counters = new int[16];
        List<Node> git = new ArrayList<>();
        List<Node> root_list = new ArrayList<>();
        root_list.add(root);
        for (int i = 0; i<1000000; i++) {
            String oid = RandomStringUtils.random(32);
            String sha256hex = DigestUtils.sha256Hex(oid);
            BigInteger oid_bint = new BigInteger(sha256hex, 16);
            List<Node> got = crush.select(4, "row", root_list, oid_bint);
            git = crush.select(1, "leaf", got, oid_bint);
            for (Node j : git) {
                counters[j.nodeID]++;
            }
        }
        System.out.println(Arrays.toString(counters));
    }

}
