package info.narazaki.android.tuboroid.agent.http;

import org.apache.http.HttpHost;

import info.narazaki.android.lib.agent.http.HttpSingleTaskAgent;
import android.content.Context;

public class HttpMaruTaskAgent extends HttpSingleTaskAgent {
    
    public HttpMaruTaskAgent(Context context, String userAgent, HttpHost proxy) {
        super(context, userAgent, proxy);
    }
}
