package commons;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.math3.primes.Primes;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Crush {
    // TODO fix randomness, somes nodes are never selected

    private int p = 14981273;
    private int m = 4;

    public List<Node> select_OSDs(Node root, String oid) {
        String sha256hex = DigestUtils.sha256Hex(oid);
        BigInteger oid_bint = new BigInteger(sha256hex, 16);
        List<Node> root_list = new ArrayList<>();
        root_list.add(root);
        List<Node> rows = select(2, "row", root_list, oid_bint);
        List<Node> osds = select(1, "leaf", rows, oid_bint);
        return osds;
    }

    public List<Node> select(int n, String type, List<Node> working_vector, BigInteger oid_bint) {
        List<Node> output = new ArrayList<>();
        int r_line;
        Node o;
        for (Node i : working_vector) {
            int failures = 0;
            for (int r = 1; r <= n; r++) {
                //TODO error if r > m, no uniqueness guarantee!
                int replica_failures = 0;
                boolean retry_descent = false;
                do {
                    Node b = i;
                    boolean retry_bucket = false;
                    do {
                        // replica rank: replica
                        r_line = r + failures;
                        // replica rank: parity
                        // r_line = r + replica_failures * n;
                        int selected_osd = c(r_line, oid_bint, b.size);
                        o = get_nth_alive_osd(b, selected_osd);

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

    private Node get_nth_alive_osd(Node b, int target) {
        Node o;
        int alives = 0;
        int alive_target = -1; // crashes program if impossible
        for (int j = 0; j < b.get_children().size(); j++) {
            o = b.get_children().get(j);
            if (o.failed || o.overloaded) {
                continue;
            } else {
                if (target == alives) {
                    alive_target = j;
                    break;
                } else {
                    alives++;
                }
            }
        }
        o = b.get_children().get(alive_target);
        return o;
    }

    private int c(int r, BigInteger oid_bint, int m) {
         return oid_bint
                .add(BigInteger.valueOf(r * p))
                .mod(BigInteger.valueOf(m))
                .intValue();
//        return ((oid_bint + r*p) % m);
    }

    public void set_bucket_size(int m) {
        this.m = m;
    }
}
