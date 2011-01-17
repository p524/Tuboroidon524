package info.narazaki.android.tuboroid.data;

import java.util.HashMap;

public class NewThreadData {
    public String author_name_;
    public String author_mail_;
    public String thread_title_;
    public String entry_body_;
    
    public HashMap<String, String> hidden_form_map_;
    
    public boolean is_retry_;
    
    public NewThreadData(String author_name, String author_mail, final String thread_title, String entry_body) {
        super();
        author_name_ = author_name;
        author_mail_ = author_mail;
        entry_body_ = entry_body;
        thread_title_ = thread_title;
        hidden_form_map_ = new HashMap<String, String>();
        is_retry_ = false;
    }
}
