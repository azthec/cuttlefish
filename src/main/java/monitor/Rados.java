package monitor;

import commons.Map;
import commons.Node;
import commons.Crush;

import java.util.*;

public class Rados {
    // boot up system
    public static void main(String[] args) {
        commons.Map cluster_map = new Map();

        Node b001 = new Node(11, "row", -1, false);
        b001.add(new Node(20, "leaf", 1000, true));
        b001.add(new Node(21, "leaf", 1000, true));
        b001.add(new Node(22, "leaf", 1000, true));
        b001.add(new Node(23, "leaf", 1000, true));
        Node b010 = new Node(12, "row", -1, false);
        b010.add(new Node(24, "leaf", -1, true));
        b010.add(new Node(25, "leaf", -1, true));
        b010.add(new Node(26, "leaf", -1, true));
        b010.add(new Node(27, "leaf", 1000, true));
        Node b100 = new Node(13, "row", -1, false);
        b100.add(new Node(28, "leaf", -1, true));
        b100.add(new Node(29, "leaf", -1, true));
        b100.add(new Node(30, "leaf", -1, true));
        b100.add(new Node(31, "leaf", 1000, true));

        cluster_map.get_root().add(b001);
        cluster_map.get_root().add(b010);
        cluster_map.get_root().add(b100);

        cluster_map.get_root().print(0);

        List<Node> root_list = new ArrayList<>();
        root_list.add(cluster_map.get_root());

        List<Node> got = Crush.select(2, "row",root_list, 1337);
        System.out.println(got.get(0).hash + " " + got.get(1).hash);
        List<Node> git = Crush.select(1, "leaf", got, 1337);
        System.out.println(git.get(0).hash + " " + git.get(1).hash);
    }


}
