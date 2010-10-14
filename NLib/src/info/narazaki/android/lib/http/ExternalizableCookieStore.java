package info.narazaki.android.lib.http;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

public class ExternalizableCookieStore extends BasicCookieStore implements Externalizable {
    
    @Override
    public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
        clear();
        
        BasicClientCookie cookie;
        String name;
        String value;
        
        int len = input.readInt();
        for (int i = 0; i < len; i++) {
            name = input.readUTF();
            value = input.readUTF();
            cookie = new BasicClientCookie(name, value);
            if (input.readBoolean()) cookie.setComment(input.readUTF());
            if (input.readBoolean()) cookie.setExpiryDate(new Date(input.readLong()));
            
            if (input.readBoolean()) cookie.setDomain(input.readUTF());
            if (input.readBoolean()) cookie.setPath(input.readUTF());
            
            cookie.setSecure(input.readBoolean());
            cookie.setVersion(input.readInt());
            addCookie(cookie);
        }
    }
    
    @Override
    public void writeExternal(ObjectOutput output) throws IOException {
        String data;
        Date date;
        List<Cookie> cookie_list = getCookies();
        output.writeInt(cookie_list.size());
        for (Cookie cookie : cookie_list) {
            output.writeUTF(cookie.getName());
            output.writeUTF(cookie.getValue());
            
            data = cookie.getComment();
            output.writeBoolean(data != null);
            if (data != null) output.writeUTF(data);
            
            date = cookie.getExpiryDate();
            output.writeBoolean(date != null);
            if (date != null) output.writeLong(date.getTime());
            
            data = cookie.getDomain();
            output.writeBoolean(data != null);
            if (data != null) output.writeUTF(data);
            
            data = cookie.getPath();
            output.writeBoolean(data != null);
            if (data != null) output.writeUTF(data);
            
            output.writeBoolean(cookie.isSecure());
            output.writeInt(cookie.getVersion());
        }
    }
    
}
