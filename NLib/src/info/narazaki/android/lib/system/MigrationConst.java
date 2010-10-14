package info.narazaki.android.lib.system;

public class MigrationConst {
    static int default_buf_size_ = 0;
    
    public static synchronized int getDefaultBufSize() {
        if (default_buf_size_ != 0) return default_buf_size_;
        
        if (MigrationSDK5.supported()) {
            default_buf_size_ = 1024 * 8;
        }
        else {
            default_buf_size_ = 256;
        }
        
        return default_buf_size_;
    }
    
}
