package info.narazaki.android.tuboroid.agent.thread;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataFileAgent {
    private static final String TAG = "DataFileAgent";
    private static final String SWAP_SUFFIX = ".swap";
    
    private final ExecutorService executor_;
    
    public HashMap<File, LinkedList<Runnable>> task_list_map_;
    
    public DataFileAgent(int threads_num) {
        super();
        executor_ = Executors.newFixedThreadPool(threads_num);
        task_list_map_ = new HashMap<File, LinkedList<Runnable>>();
    }
    
    private synchronized void pushTask(final File key, final Runnable runnable) {
        LinkedList<Runnable> task_list = task_list_map_.get(key);
        if (task_list == null) {
            task_list = new LinkedList<Runnable>();
            task_list_map_.put(key, task_list);
        }
        task_list.addLast(runnable);
        if (task_list.size() == 1) {
            executor_.submit(runnable);
        }
    }
    
    private synchronized void popTask(final File key) {
        LinkedList<Runnable> task_list = task_list_map_.get(key);
        if (task_list == null) return;
        task_list.removeFirst();
        if (task_list.size() == 0) {
            task_list_map_.remove(key);
            return;
        }
        Runnable runnable = task_list.getFirst();
        executor_.submit(runnable);
    }
    
    public static interface FileReadCallback {
        public void read(final InputStream stream);
    }
    
    public static interface FileReadUTF8Callback {
        public void read(final BufferedReader reader);
    }
    
    public static interface FileWroteCallback {
        public void onFileWrote(final boolean succeeded);
    }
    
    public static interface FileWriteStreamCallback {
        public void write(final OutputStream stream);
    }
    
    public static interface FileWriteUTF8StreamCallback {
        public void write(final Writer writer) throws IOException;
    }
    
    public void readFile(final String filename, final FileReadUTF8Callback callback) {
        readFile(filename, new FileReadCallback() {
            
            @Override
            public void read(InputStream stream) {
                if (stream == null) {
                    callback.read(null);
                    return;
                }
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"), 8 * 1024);
                    callback.read(reader);
                    reader.close();
                }
                catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    public void readFile(final String filename, final FileReadCallback callback) {
        final File file = new File(filename);
        pushTask(file, new Runnable() {
            @Override
            public void run() {
                execReadFile(file, callback);
            }
        });
    }
    
    protected void execReadFile(final File file, final FileReadCallback callback) {
        try {
            final FileInputStream is = new FileInputStream(file);
            callback.read(is);
            popTask(file);
            return;
        }
        catch (FileNotFoundException e) {
            // e.printStackTrace();
        }
        catch (SecurityException e) {
            // e.printStackTrace();
        }
        callback.read(null);
        popTask(file);
    }
    
    public void writeFile(final String filename, final FileWriteUTF8StreamCallback writer, final boolean append,
            final boolean mkdir, final boolean atomic, final FileWroteCallback callback) {
        writeFile(filename, new FileWriteStreamCallback() {
            @Override
            public void write(final OutputStream stream) {
                OutputStreamWriter osw = null;
                try {
                    osw = new OutputStreamWriter(stream, "UTF-8");
                    writer.write(osw);
                }
                catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                finally {
                    if (osw != null) {
                        try {
                            osw.close();
                        }
                        catch (IOException e1) {
                        }
                    }
                }
                
            }
        }, append, mkdir, atomic, callback);
    }
    
    public void writeFile(final String filename, final FileWriteStreamCallback stream_writer, final boolean append,
            final boolean mkdir, final boolean atomic, final FileWroteCallback callback) {
        final File file = new File(filename);
        pushTask(file, new Runnable() {
            @Override
            public void run() {
                execWriteFile(file, stream_writer, append, mkdir, atomic, callback);
            }
        });
    }
    
    protected void execWriteFile(final File file, final FileWriteStreamCallback stream_writer, final boolean append,
            final boolean mkdir, boolean atomic, final FileWroteCallback callback) {
        if (append) atomic = false;
        try {
            final File target = atomic ? File.createTempFile(file.getName(), SWAP_SUFFIX, file.getParentFile()) : file;
            if (!file.canWrite() || !target.canWrite()) {
                // mkdir
                if (!mkdir) throw new IOException();
                File dir = target.getParentFile();
                if (dir == null) throw new IOException();
                dir.mkdirs();
            }
            final FileOutputStream os = new FileOutputStream(target, append);
            final BufferedOutputStream os_buf = new BufferedOutputStream(os);
            stream_writer.write(os_buf);
            os_buf.close();
            os.close();
            
            if (atomic) target.renameTo(file);
            if (callback != null) callback.onFileWrote(true);
            popTask(file);
            return;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (callback != null) callback.onFileWrote(false);
        popTask(file);
    }
    
    public void deleteFile(final String filename, final FileWroteCallback callback) {
        final File file = new File(filename);
        pushTask(file, new Runnable() {
            @Override
            public void run() {
                execDeleteFile(file, callback);
            }
        });
    }
    
    protected void execDeleteFile(final File file, final FileWroteCallback callback) {
        boolean result = false;
        try {
            if (file.exists()) {
                execDeleteRecursive(file);
            }
        }
        catch (SecurityException e) {
            // e.printStackTrace();
        }
        if (callback != null) callback.onFileWrote(result);
        popTask(file);
    }
    
    public void deleteFiles(final List<String> filenames, final FileWroteCallback callback) {
        executor_.submit(new Runnable() {
            @Override
            public void run() {
                execDeleteFiles(filenames, callback);
            }
        });
    }
    
    public void deleteFiles(final String[] filenames, final FileWroteCallback callback) {
        executor_.submit(new Runnable() {
            @Override
            public void run() {
                execDeleteFiles(filenames, callback);
            }
        });
    }
    
    protected void execDeleteFiles(final List<String> filenames, final FileWroteCallback callback) {
        boolean result = false;
        try {
            for (String filename : filenames) {
                File file = new File(filename);
                if (file.exists()) {
                    execDeleteRecursive(file);
                }
            }
        }
        catch (SecurityException e) {
            // e.printStackTrace();
        }
        if (callback != null) callback.onFileWrote(result);
    }
    
    protected void execDeleteFiles(final String[] filenames, final FileWroteCallback callback) {
        boolean result = false;
        try {
            for (String filename : filenames) {
                File file = new File(filename);
                if (file.exists()) {
                    execDeleteRecursive(file);
                }
            }
        }
        catch (SecurityException e) {
            // e.printStackTrace();
        }
        if (callback != null) callback.onFileWrote(result);
    }
    
    protected void execDeleteRecursive(final File dir) {
        if (!dir.exists()) return;
        
        if (dir.isFile()) dir.delete();
        
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                execDeleteRecursive(files[i]);
            }
            dir.delete();
        }
    }
    
    public void moveFilesInDirectory(final String from_dir, final String to_dir, final Runnable callback) {
        final File from_dir_file = new File(from_dir);
        final File to_dir_file = new File(to_dir);
        executor_.submit(new Runnable() {
            @Override
            public void run() {
                execMoveFilesInDirectory(from_dir_file, to_dir_file, callback);
            }
        });
    }
    
    protected void execMoveFilesInDirectory(final File from_dir_file, final File to_dir_file, final Runnable callback) {
        try {
            if (!from_dir_file.exists()) throw new SecurityException(from_dir_file + " not found");
            if (!from_dir_file.isDirectory()) throw new SecurityException(from_dir_file + " is not directory");
            if (!from_dir_file.canRead()) throw new SecurityException(from_dir_file + " is not readable");
            
            if (to_dir_file.exists() && !to_dir_file.isDirectory()) throw new SecurityException(to_dir_file
                    + " is not directory");
            if (!to_dir_file.exists()) to_dir_file.mkdirs();
            if (!to_dir_file.canWrite()) throw new SecurityException(from_dir_file + " is not writable");
            
            File[] files = from_dir_file.listFiles();
            for (File file : files) {
                if (file.isFile()) {
                    file.renameTo(new File(to_dir_file, file.getName()));
                }
            }
        }
        catch (SecurityException e) {
            // e.printStackTrace();
        }
        if (callback != null) callback.run();
    }
    
    public void copyFile(final String orig, final String dist, final boolean mkdir, final FileWroteCallback callback) {
        final File file_orig = new File(orig);
        final File file_dist = new File(dist);
        
        pushTask(file_dist, new Runnable() {
            @Override
            public void run() {
                try {
                    final File file_swap = File.createTempFile(file_dist.getName(), SWAP_SUFFIX,
                            file_dist.getParentFile());
                    execCopyFile(file_orig, file_swap, mkdir, new FileWroteCallback() {
                        @Override
                        public void onFileWrote(boolean succeeded) {
                            if (!succeeded) {
                                if (callback != null) callback.onFileWrote(false);
                                return;
                            }
                            if (callback != null) callback.onFileWrote(file_swap.renameTo(file_dist));
                        }
                    });
                }
                catch (IOException e) {
                    if (callback != null) callback.onFileWrote(false);
                }
            }
        });
    }
    
    protected void execCopyFile(final File orig, final File dist, final boolean mkdir, final FileWroteCallback callback) {
        try {
            if (!orig.canRead()) throw new IOException();
            if (!dist.canWrite()) {
                // mkdir
                if (!mkdir) throw new IOException();
                File dir = dist.getParentFile();
                if (dir == null) throw new IOException();
                dir.mkdirs();
            }
            
            FileChannel src_channel = new FileInputStream(orig).getChannel();
            FileChannel dest_channel = new FileOutputStream(dist).getChannel();
            try {
                src_channel.transferTo(0, src_channel.size(), dest_channel);
            }
            finally {
                src_channel.close();
                dest_channel.close();
            }
            
            if (callback != null) callback.onFileWrote(true);
            popTask(dist);
            return;
        }
        catch (FileNotFoundException e) {
            // e.printStackTrace();
        }
        catch (SecurityException e) {
            // e.printStackTrace();
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (callback != null) callback.onFileWrote(false);
        popTask(dist);
    }
    
}
