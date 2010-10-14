package info.narazaki.android.lib.agent.db;

import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

abstract public class SQLiteAgentBase {
    static public interface DbResultReceiver {
        public void onQuery(final Cursor cursor);
        
        public void onError();
    }
    
    static public interface DbTransaction {
        public void run();
        
        public void onError();
    }
    
    static public abstract class DbTransactionBase implements DbTransaction {
        @Override
        public void onError() {}
    }
    
    static public class DbTransactionDelegate implements DbTransaction {
        final Runnable runnable_;
        
        public DbTransactionDelegate(Runnable runnable) {
            runnable_ = runnable;
        }
        
        @Override
        public void run() {
            if (runnable_ != null) runnable_.run();
        }
        
        @Override
        public void onError() {
            if (runnable_ != null) runnable_.run();
        }
    }
    
    private Context context_;
    private final ExecutorService executor_;
    private SQLiteDatabase db_;
    private SQLiteOpenHelper open_helper_;
    
    public SQLiteAgentBase(Context context) {
        super();
        context_ = context;
        executor_ = Executors.newSingleThreadExecutor();
        open_helper_ = createSQLiteOpenHelper(context);
        transaction_queue_ = new LinkedList<Runnable>();
    }
    
    abstract protected SQLiteOpenHelper createSQLiteOpenHelper(Context context);
    
    public synchronized SQLiteAgentBase open() {
        try {
            db_ = executor_.submit(new Callable<SQLiteDatabase>() {
                @Override
                public SQLiteDatabase call() throws Exception {
                    return open_helper_.getWritableDatabase();
                }
            }).get();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        catch (ExecutionException e) {
            e.printStackTrace();
        }
        return this;
    }
    
    public synchronized SQLiteAgentBase close() {
        if (db_ == null) return this;
        try {
            executor_.submit(new Runnable() {
                @Override
                public void run() {
                    open_helper_.close();
                }
            }).get();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        catch (ExecutionException e) {
            e.printStackTrace();
        }
        db_ = null;
        return this;
    }
    
    public SQLiteAgentBase reopen() {
        return close().open();
    }
    
    public Context getContext() {
        return context_;
    }
    
    public SQLiteDatabase getDB() {
        return db_;
    }
    
    private final LinkedList<Runnable> transaction_queue_;
    
    private synchronized void pushTransactionTask(final Runnable runnable) {
        transaction_queue_.addLast(runnable);
        if (transaction_queue_.size() == 1) {
            executor_.submit(runnable);
        }
    }
    
    private synchronized void popTransactionTask() {
        transaction_queue_.removeFirst();
        if (transaction_queue_.size() == 0) return;
        Runnable runnable = transaction_queue_.getFirst();
        executor_.submit(runnable);
    }
    
    public void submitTransaction(final DbTransaction runnable) {
        pushTransactionTask(new Runnable() {
            @Override
            public void run() {
                boolean ok = false;
                try {
                    getDB().beginTransaction();
                    runnable.run();
                    ok = true;
                    getDB().setTransactionSuccessful();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    if (!ok) runnable.onError();
                }
                finally {
                    getDB().endTransaction();
                    popTransactionTask();
                }
            }
        });
    }
    
    /**
     * LIKE検索のためorig文字列を$文字でエスケープする
     * 
     * LIKE ? ESCAPE '$'のようにして使う
     * 
     * @param orig
     * @return
     */
    static public String escapeLike(String orig) {
        StringBuilder result = new StringBuilder();
        int length = orig.length();
        for (int i = 0; i < length; i++) {
            char c = orig.charAt(i);
            switch (c) {
            case '%':
            case '_':
            case '$':
                result.append('$');
            default:
                result.append(c);
            }
        }
        return result.toString();
    }
}
