package info.narazaki.android.lib.memory;

public class SimpleIntArrayPool {
    private final int[][] pool_;
    
    public SimpleIntArrayPool(int max_pool) {
        pool_ = new int[max_pool][];
    }
    
    public synchronized int[] obtain(int size) {
        int min_index = -1;
        int min_length = Integer.MAX_VALUE;
        for (int i = 0; i < pool_.length; i++) {
            if (pool_[i] != null) {
                int len = pool_[i].length;
                if (len > size && (min_length > len)) {
                    min_index = i;
                    min_length = len;
                }
            }
        }
        
        if (min_index != -1) {
            int[] result = pool_[min_index];
            pool_[min_index] = null;
            return result;
        }
        return new int[size];
    }
    
    public synchronized int[] recycle(int[] ptr) {
        int min_index = -1;
        int min_length = Integer.MAX_VALUE;
        int ptr_len = ptr.length;
        for (int i = 0; i < pool_.length; i++) {
            if (pool_[i] == null) {
                pool_[i] = ptr;
                return null;
            }
            int len = pool_[i].length;
            if (ptr_len > len && min_length > len) {
                min_index = i;
                min_length = len;
            }
        }
        
        if (min_index != -1) {
            pool_[min_index] = ptr;
        }
        return null;
    }
    
}
