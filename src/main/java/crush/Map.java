package crush;

public class Map {
    int choose_local_tries = 0;
    int choose_local_fallback_tries = 0;
    int choose_total_tries = 50;
    int chooseleaf_descend_once = 1;
    int chooseleaf_vary_r = 1;
    int chooseleaf_stable = 1;

    // https://github.com/ceph/ceph/blob/master/src/crush/builder.c#L138
    int add_bucket() {
        return 0;
    }
}