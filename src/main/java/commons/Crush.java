package commons;

import org.apache.commons.codec.digest.DigestUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Crush {
    // TODO fix randomness, somes nodes are never selected

    private int p = 14981273;

    // numberOfReplicas depends directly on the selection rule
    // in this case rule returns one primary OSD and one replica OSD
    public static int numberOfReplicas = 1;
    public List<CrushNode> select_OSDs(CrushNode root, String oid) {
        // This is the equivalent to Crush selection rules
        // Assumes CrushMap structure follows root - row - osd scheme.
        String sha256hex = DigestUtils.sha256Hex(oid);
        BigInteger oid_bint = new BigInteger(sha256hex, 16);
        List<CrushNode> root_list = new ArrayList<>();
        root_list.add(root);
        List<CrushNode> rows = select(2, "row", root_list, oid_bint);
        List<CrushNode> osds = select(1, "osd", rows, oid_bint);
        return osds;
    }

    public List<CrushNode> select(int n, String type, List<CrushNode> working_vector, BigInteger oid_bint) {
        List<CrushNode> output = new ArrayList<>();
        int r_line;
        CrushNode o;
        for (CrushNode i : working_vector) {
            int failures = 0;
            for (int r = 1; r <= n; r++) {
                //TODO error if r > m, no uniqueness guarantee!
                int replica_failures = 0;
                boolean retry_descent = false;
                do {
                    CrushNode b = i;
                    boolean retry_bucket = false;
                    do {
                        // replica rank: replica
                        r_line = r + failures;
                        // TODO implement replica rank: parity
                        // r_line = r + replica_failures * n;
                        int selected_osd = c(r_line, oid_bint, b.alive_size);
                        o = get_nth_alive_osd(b, selected_osd);

                        if (!o.type.equals(type)) {
                            b = o;
                            retry_bucket = true;
                        } else if (output.contains(o) || o.isFailed() || o.isOverloaded()) {
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

    private CrushNode get_nth_alive_osd(CrushNode b, int target) {
        CrushNode o;
        int alives = 0;
        int alive_target = -1; // crashes program if impossible
        for (int j = 0; j < b.get_children().size(); j++) {
            o = b.get_children().get(j);
            if (o.isFailed() || o.isOverloaded()) {
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
         long rp = r * p;
         BigInteger res = oid_bint
                .add(BigInteger.valueOf(rp))
                .mod(BigInteger.valueOf(m));
         return res.intValue();
//       return ((oid_bint + r*p) % m);
    }

    public static BigInteger hash(String oid) {
        String sha256hex = DigestUtils.sha256Hex(oid);
        BigInteger oid_bint = new BigInteger(sha256hex, 16);
        return oid_bint;
    }

    public static int get_pg_id(String oid, int total_pgs) {
        BigInteger ho = Crush.hash(oid);
        BigInteger mask = ho.and(BigInteger.valueOf(total_pgs));
        return mask.intValue();
    }

}
