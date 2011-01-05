package info.narazaki.android.lib.agent.http;

import info.narazaki.android.lib.agent.http.task.HttpTaskBase;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpHost;

import android.content.Context;

public class HttpSingleTaskAgent extends HttpTaskAgent {
    Future<?> pending_;
    HttpTaskBase prev_task_;
    
    public HttpSingleTaskAgent(Context context, String user_agent, HttpHost proxy) {
        super(context, user_agent, proxy);
        pending_ = null;
        prev_task_ = null;
    }
    
    @Override
    public Future<?> send(HttpTaskBase task) {
        abort();
        final Future<?> pending = super.send(task);
        pending_ = pending;
        prev_task_ = task;
        return new Future() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return abort(pending, mayInterruptIfRunning);
            }
            
            @Override
            public Object get() throws InterruptedException, ExecutionException {
                return pending.get();
            }
            
            @Override
            public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                    TimeoutException {
                return pending.get(timeout, unit);
            }
            
            @Override
            public boolean isCancelled() {
                return pending.isCancelled();
            }
            
            @Override
            public boolean isDone() {
                return pending.isDone();
            }
        };
    }
    
    public void abort() {
        abort(pending_, true);
        pending_ = null;
        prev_task_ = null;
    }
    
    public boolean abort(Future<?> pending, boolean mayInterruptIfRunning) {
        boolean result = false;
        if (pending != null) {
            if (!pending.isDone() && !pending.isCancelled()) {
                prev_task_.abort();
                result = pending.cancel(mayInterruptIfRunning);
                // try {
                // pending.get();
                // }
                // catch (Exception e) {
                // e.printStackTrace();
                // }
            }
        }
        return result;
    }
    
}
