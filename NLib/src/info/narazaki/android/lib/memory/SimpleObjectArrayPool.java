package info.narazaki.android.lib.memory;

abstract public class SimpleObjectArrayPool<T> {
    private final Object[] pool_;
    
    public SimpleObjectArrayPool(int max_pool) {
        pool_ = new Object[max_pool];
    }
    
    @SuppressWarnings("unchecked")
    public synchronized T obtain() {
        for (int i = 0; i < pool_.length; i++) {
            if (pool_[i] != null) {
                T result = (T) pool_[i];
                pool_[i] = null;
                return result;
            }
        }
        return construct();
    }
    
    abstract protected T construct();
    
    public synchronized T recycle(T ptr) {
        for (int i = 0; i < pool_.length; i++) {
            if (pool_[i] == null) {
                pool_[i] = ptr;
                return null;
            }
        }
        return null;
    }
}
