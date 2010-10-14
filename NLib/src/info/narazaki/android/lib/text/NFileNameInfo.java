package info.narazaki.android.lib.text;

import java.io.File;

public class NFileNameInfo {
    private String dirname_;
    private String filename_;
    private String ext_;
    
    public NFileNameInfo(final File file) {
        File parent = file.getParentFile();
        dirname_ = parent != null ? parent.getAbsolutePath() : "";
        
        String full_filename = file.getName();
        if (full_filename == null) {
            filename_ = "";
            ext_ = "";
            return;
        }
        
        int index = full_filename.lastIndexOf('.');
        if (index == -1) {
            filename_ = full_filename;
            ext_ = "";
            return;
        }
        
        filename_ = index > 0 ? full_filename.substring(0, index) : "";
        ext_ = full_filename.length() > index ? full_filename.substring(index + 1, full_filename.length()) : "";
    }
    
    /**
     * @return dirname_
     */
    public String getDirName() {
        return dirname_;
    }
    
    /**
     * @return filename_
     */
    public String getFileName() {
        return filename_;
    }
    
    /**
     * @return ext_
     */
    public String getExtention() {
        return ext_;
    }
    
}
