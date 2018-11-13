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

        Node b001 = new Node(110, "row",false);
        b001.add(new Node(0, "leaf", true));
        b001.add(new Node(1, "leaf", true));
        b001.add(new Node(2, "leaf", true));
        b001.add(new Node(3, "leaf", true));
        Node b010 = new Node(111, "row", false);
        b010.add(new Node(4, "leaf", true));
        b010.add(new Node(5, "leaf", true));
        b010.add(new Node(6, "leaf", true));
        b010.add(new Node(7, "leaf", true));
        Node b100 = new Node(112, "row", false);
        b100.add(new Node(8, "leaf", true));
        b100.add(new Node(9, "leaf", true));
        b100.add(new Node(10, "leaf", true));
        b100.add(new Node(11, "leaf", true));
        Node b111 = new Node(112, "row", false);
        b111.add(new Node(12, "leaf", true));
        b111.add(new Node(13, "leaf", true));
        b111.add(new Node(14, "leaf", true));
        b111.add(new Node(15, "leaf", true));

        // overload some dudes
        b001.overloadChildren(b001.get_children().get(0));
        b001.overloadChildren(b001.get_children().get(2));
        b001.overloadChildren(b001.get_children().get(3));
        b111.failChildren(b111.get_children().get(2));

        cluster_map.get_root().add(b001);
        cluster_map.get_root().add(b010);
        cluster_map.get_root().add(b100);
        cluster_map.get_root().add(b111);

        cluster_map.get_root().print(0);

        Crush crush = new Crush();

        // test_select_randomness(crush, cluster_map.get_root());


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
