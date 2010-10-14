package info.narazaki.android.tuboroid.service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

public class TuboroidServiceTask {
    private static final String TAG = "TuboroidServiceClient";
    
    private final List<ServiceSender> sender_list_;
    private final Context context_;
    
    private ITuboroidService service_;
    private ServiceConnection service_conn_ = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service_ = ITuboroidService.Stub.asInterface(binder);
            flushServiceQueue();
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    
    public TuboroidServiceTask(Context context) {
        super();
        context_ = context;
        sender_list_ = Collections.synchronizedList(new LinkedList<ServiceSender>());
    }
    
    public static interface ServiceSender {
        public void send(ITuboroidService service) throws RemoteException;
    }
    
    public void unbind() {
        context_.unbindService(service_conn_);
        
    }
    
    public void bind() {
        Intent intent = new Intent(context_, TuboroidService.class);
        context_.bindService(intent, service_conn_, Context.BIND_AUTO_CREATE);
    }
    
    public void send(final ServiceSender sender) {
        sender_list_.add(sender);
        flushServiceQueue();
    }
    
    private synchronized void flushServiceQueue() {
        if (service_ == null) return;
        while (sender_list_.size() > 0) {
            ServiceSender sender = sender_list_.get(0);
            sender_list_.remove(0);
            try {
                if (service_ == null) return;
                sender.send(service_);
            }
            catch (RemoteException e) {
            }
        }
    }
    
}
