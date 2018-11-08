package crush;


// input value                                         x
// replica number                                      r
// randomly deterministically chosen prime number      p
// bucket size                                         m

// c(r,x) = (hash(x)+ rp) mod m
// p > m
// r ≤ m
public class Bucket {
    // uniform buckets don't have weights, I think
    String alg = "CRUSH_BUCKET_UNIFORM";
    int hash;
    int type;
    int size;
    int[] items;

    public Bucket(Map crush_map, int hash, int type,
                  int size, int[] items) {
        this.hash = hash;
        this.type = type;
        this.size = size;
        this.items = items.clone();
    }

}