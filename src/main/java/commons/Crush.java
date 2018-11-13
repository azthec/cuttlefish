package commons;

import java.util.ArrayList;
import java.util.List;

public class Crush {
    static int p = 14981273;
    static int m = 3;

    static public List<Node> select(int n, String type, List<Node> working_vector, int oid) {

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
                        //TODO only replica rank for now
                        r_line = r + failures;
                        o = b.get_children().get(c(r_line, oid));
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

    static public int c(int r, int oid) {
        //TODO hash actual x instead of using just passed value

        return (oid + r * p) % m;
    }
}
