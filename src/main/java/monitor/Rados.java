package monitor;

import java.util.*;

public class Rados {
    static int p = 14981273;
    static int m = 3;

    // boot up system
    public static void main(String[] args) {
        Map cluster_map = new Map();

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

        List<Node> got = select(2, "row",root_list, 1337);
        System.out.println(got.get(0).hash + " " + got.get(1).hash);
        List<Node> git = select(1, "leaf", got, 1337);
        System.out.println(git.get(0).hash + " " + git.get(1).hash);

    }

    static public List<Node> select(int n, String type, List<Node> working_vector, int x) {

        List<Node> output = new ArrayList<>();
        int r_line;
        Node i, o;
        for (Iterator<Node> iterator = working_vector.iterator(); iterator.hasNext();) {
            i = iterator.next();

            int failures = 0;
            for (int r = 1; r <= n; r++) {
                //TODO error if r > m, no uniqueness guarantee!
                int replica_failures = 0;
                boolean retry_descent = false;
                do {
                    Node b = i;
                    boolean retry_bucket = false;
                    do {
                        //TODO only replica rank for now
                        r_line = r + failures;
                        // System.out.println(b.hash);
                        // System.out.println(c(r_line, x));
                        o = b.get_children().get(c(r_line, x));
                        // System.out.println(type);
                        // System.out.println(o.type);
                        // System.out.println(o.type.equals(type));
                        if (!o.type.equals(type)) {
                            b = o;
                            retry_bucket = true;
                        } else if (output.contains(o) || o.failed || o.overloaded) {
                            replica_failures++;
                            failures++;
                            if (output.contains(o) && replica_failures < 3) {
                                retry_bucket = true;
                            } else {
                                retry_descent = true;
                            }
                        }
                    } while ( retry_bucket);
                } while ( retry_descent);
                output.add(o);
            }
        }

        return output;
    }

    // x is the file input vale / object ID
    static public int c(int r, int x) {
        //int[] output = new int[r];
        //TODO hash actual x instead of using just passed value

        return (x + r * p) % m;
    }
}
