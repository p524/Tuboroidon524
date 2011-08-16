package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.lib.agent.http.task.HttpGetFileTask;
import info.narazaki.android.lib.system.MigrationSDK4;
import info.narazaki.android.lib.text.NFileNameInfo;
import info.narazaki.android.tuboroid.data.ThreadData;
import info.narazaki.android.tuboroid.data.ThreadEntryData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Log;

public class ImageFetchAgent {
    private static final String TAG = "ImageFetchAgent";
    private static final int MAX_BITMAP_HARD_CACHE = 16;
    private static final int MAX_FETCHING_PROGRESS = 10;
    
    private TuboroidAgentManager agent_manager_;
    
    private final ExecutorService executor_;
    
    private final LinkedList<Runnable> task_queue_ = new LinkedList<Runnable>();
    private final HashMap<ImageLocation, LinkedList<BitmapFetchedCallback>> callbacks_map_ = new HashMap<ImageLocation, LinkedList<BitmapFetchedCallback>>();
    
    public interface BitmapFetchedCallback {
        
    	public void onBeginOnlineFetch();
         
    	public void onProgress(final int current_length, final int content_length);
        
        public void onBegeinNoCache();
        
        public void onFailed();
        
        public void onFetched(final Bitmap bitmap);
        
        public void onCacheFetched(final Bitmap bitmap);
    }
    
    private class ImageLocation {
        final String image_uri_;
        final int max_width_;
        final int max_height_;
        
        public ImageLocation(String imageUri, int maxWidth, int maxHeight) {
            super();
            image_uri_ = imageUri;
            max_width_ = maxWidth;
            max_height_ = maxHeight;
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof ImageLocation) {
                ImageLocation target = (ImageLocation) o;
                if (!image_uri_.equals(target.image_uri_)) return false;
                if (max_width_ != target.max_width_) return false;
                if (max_height_ != target.max_height_) return false;
                return true;
            }
            return super.equals(o);
        }
        
        @Override
        public int hashCode() {
            return image_uri_.hashCode();
        }
    }
    
    private synchronized void pushTask(final Runnable runnable) {
        task_queue_.addLast(runnable);
        if (task_queue_.size() == 1) {
            executor_.submit(runnable);
        }
    }
    
    private synchronized boolean registerCallback(final ImageLocation location, final BitmapFetchedCallback callback) {
        LinkedList<BitmapFetchedCallback> list = callbacks_map_.get(location);
        if (list != null) {
            list.add(callback);
            return false;
        }
        
        list = new LinkedList<BitmapFetchedCallback>();
        list.add(callback);
        callbacks_map_.put(location, list);
        return true;
    }
    
    private synchronized void popTask() {
        task_queue_.removeFirst();
        
        if (task_queue_.size() == 0) return;
        Runnable next = task_queue_.getFirst();
        executor_.submit(next);
    }
    
    private synchronized void sendCallback(final ImageLocation location, final Bitmap bitmap) {
        if (location != null) {
            LinkedList<BitmapFetchedCallback> list = callbacks_map_.get(location);
            callbacks_map_.remove(location);
            if (list != null) {
                for (BitmapFetchedCallback callback : list) {
                    if (callback != null) {
                        if (bitmap != null) {
                            callback.onFetched(bitmap);
                        }
                        else {
                            callback.onFailed();
                        }
                    }
                }
            }
        }
    }

    private synchronized void sendBeginOnlineFetchCallback(final ImageLocation location) {
        if (location != null) {
            LinkedList<BitmapFetchedCallback> list = callbacks_map_.get(location);
            if (list != null) {
                for (BitmapFetchedCallback callback : list) {
                    if (callback != null) {
                        callback.onBeginOnlineFetch();
                    }
                }
            }
        }
    }
    
    private synchronized void sendProgressCallback(final ImageLocation location, final int current_length,
            final int content_length) {
        if (location != null) {
            LinkedList<BitmapFetchedCallback> list = callbacks_map_.get(location);
            if (list != null) {
                for (BitmapFetchedCallback callback : list) {
                    if (callback != null) {
                        callback.onProgress(current_length, content_length);
                    }
                }
            }
        }
    }
    
    private final HashMap<File, Bitmap> bitmap_hard_cache_ = new LinkedHashMap<File, Bitmap>(MAX_BITMAP_HARD_CACHE / 2,
            0.75f, true) {
        private static final long serialVersionUID = 1L;
        
        @Override
        protected boolean removeEldestEntry(LinkedHashMap.Entry<File, Bitmap> eldest) {
            if (size() <= MAX_BITMAP_HARD_CACHE) return false;
            return true;
        }
    };
    
    private final HashMap<File, SoftReference<Bitmap>> bitmap_soft_cache_ = new HashMap<File, SoftReference<Bitmap>>();
    
    private void storeBitmapCache(File file, Bitmap bitmap) {
        if (bitmap == null) return;
        synchronized (bitmap_soft_cache_) {
            bitmap_hard_cache_.put(file, bitmap);
            bitmap_soft_cache_.put(file, new SoftReference<Bitmap>(bitmap));
        }
    }
    
    private void deleteBitmapCache(File file) {
        synchronized (bitmap_soft_cache_) {
            bitmap_hard_cache_.remove(file);
            bitmap_soft_cache_.remove(file);
        }
    }
    
    private Bitmap getBitmapCache(File file) {
        synchronized (bitmap_soft_cache_) {
            SoftReference<Bitmap> ref = bitmap_soft_cache_.get(file);
            if (ref == null) return null;
            bitmap_hard_cache_.remove(file);
            Bitmap bitmap = ref.get();
            if (bitmap == null) return null;
            
            bitmap_hard_cache_.put(file, bitmap);
            
            return bitmap;
        }
    }
    
    public ImageFetchAgent(TuboroidAgentManager agent_manager) {
        super();
        agent_manager_ = agent_manager;
        executor_ = Executors.newSingleThreadExecutor();
    }
    
    public void deleteImage(final File image_local_file) {
        pushTask(new Runnable() {
            @Override
            public void run() {
                File parent_dir = image_local_file.getParentFile();
                if (!parent_dir.exists() || !parent_dir.canRead() || !parent_dir.isDirectory()) return;
                
                NFileNameInfo info = new NFileNameInfo(image_local_file);
                final String file_prefix = info.getFileName();
                File[] file_list = parent_dir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        return filename.startsWith(file_prefix);
                    }
                });
                for (File file : file_list) {
                    try {
                        deleteBitmapCache(file.getCanonicalFile());
                        if (file.canWrite()) file.delete();
                    }
                    catch (SecurityException e) {
                        e.printStackTrace();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                sendCallback(null, null);
                popTask();
            }
        });
    }
    
    public boolean hasImageCacheFile(final ThreadData thread_data, final ThreadEntryData entry_data,
            final int image_index) {
        final File file = entry_data.getImageLocalFile(agent_manager_.getContext(), thread_data, image_index);
        if (file == null) return false;
        
        if (file.exists() && file.canRead()) return true;
        return false;
    }
    
    public synchronized boolean fetchImage(final BitmapFetchedCallback callback, final File image_local_file,
            final String image_uri, final int max_width, final int max_height, final boolean can_cache) {
        File cache_file = getScaledCacheFile(max_width, max_height, image_local_file);
        Bitmap bitmap = getBitmapCache(cache_file);
        if (bitmap != null) {
            callback.onCacheFetched(bitmap);
            return true;
        }
        
        callback.onBegeinNoCache();
        
        final ImageLocation location = new ImageLocation(image_uri, max_width, max_height);
        if (registerCallback(location, callback)) {
            pushTask(new Runnable() {
                @Override
                public void run() {
                    execFetchImage(location, callback, image_local_file, image_uri, max_width, max_height, can_cache,
                            true);
                }
            });
        }
        return false;
    }
    
    private void execFetchImage(final ImageLocation location, final BitmapFetchedCallback callback,
            final File image_local_file, final String image_uri, final int max_width, final int max_height,
            final boolean can_cache, final boolean can_http_get) {
        if (image_local_file.exists() && image_local_file.canRead()) {
            if (!assignImage(location, max_width, max_height, image_local_file, can_cache)) {
                sendCallback(location, null);
                popTask();
            }
            return;
        }
        
        if (!can_http_get) {
            sendCallback(location, null);
            popTask();
            return;
        }
        
        // 通信が発生する時はタスクを登録して一旦pop
        HttpGetFileTask task = new HttpGetFileTask(image_uri, image_local_file, new HttpGetFileTask.Callback() {
            @Override
            public void onFailed() {
                sendCallback(location, null);
            }
            
            @Override
            public void onCompleted() {
                pushTask(new Runnable() {
                    @Override
                    public void run() {
                        execFetchImage(location, callback, image_local_file, image_uri, max_width, max_height,
                                can_cache, false);
                    }
                });
            }
            
            @Override
            public void onStart() {
                Log.i(TAG, "Fetch online image : " + image_uri + " : started");
                sendBeginOnlineFetchCallback(location);
            }
            
            @Override
            public void onProgress(int current_length, int content_length) {
                sendProgressCallback(location, current_length, content_length);
            }
        });
        
        agent_manager_.getMultiHttpAgent().send(task);
        popTask();
    }
    
    private boolean assignImage(final ImageLocation location, final int max_width, final int max_height,
            final File file, final boolean can_cache) {
        final Bitmap bitmap_scaled = getBitmap(max_width, max_height, file, can_cache);
        if (bitmap_scaled == null) return false;
        
        sendCallback(location, bitmap_scaled);
        popTask();
        return true;
    }
    
    private Bitmap getBitmap(final int max_width, final int max_height, final File file, final boolean can_cache) {
        if (max_width <= 0 || max_height <= 0) return getBulkBitmap(file);
        
        File cache_file = getScaledCacheFile(max_width, max_height, file);
        Bitmap bitmap = getBitmapCache(cache_file);
        if (bitmap != null) return bitmap;
        if (cache_file.exists() && cache_file.canRead()) {
            bitmap = getBulkBitmap(cache_file);
            if (can_cache) storeBitmapCache(cache_file, bitmap);
            return bitmap;
        }
        
        return createScaledBitmap(max_width, max_height, file, cache_file, can_cache);
    }
    
    private File getScaledCacheFile(final int max_width, final int max_height, final File file) {
        NFileNameInfo info = new NFileNameInfo(file);
        File cache_file = new File(info.getDirName() + File.separator + info.getFileName() + "_s_" + max_width + "x"
                + max_height + ".png");
        try {
            cache_file = cache_file.getCanonicalFile();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return cache_file;
    }
    
    private Bitmap getBulkBitmap(final File file) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        MigrationSDK4.BitmapFactory_SetOptions(opt, "inPurgeable", true);
        opt.inJustDecodeBounds = false;
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), opt);
        
        if (bitmap == null) {
            Log.i(TAG, "Bitmap : " + file.getAbsolutePath() + " : failed");
        }
        return bitmap;
    }
    
    private Bitmap createScaledBitmap(final int max_width, final int max_height, final File file,
            final File cache_file, final boolean can_cache) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        MigrationSDK4.BitmapFactory_SetOptions(opt, "inPurgeable", true);
        opt.inJustDecodeBounds = true;
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        
        BitmapFactory.decodeFile(file.getAbsolutePath(), opt);
        if (opt.outHeight == -1 || opt.outWidth == -1) return null;
        
        int width = opt.outWidth;
        int height = opt.outHeight;
        
        if (width <= max_width && height <= max_height) {
            return getBulkBitmap(file);
        }
        
        int width_ratio = 1;
        int height_ratio = 1;
        if (width > max_width) width_ratio = width / max_width;
        if (height > max_height) height_ratio = height / max_height;
        
        int ratio = width_ratio > height_ratio ? width_ratio : height_ratio;
        
        opt.inSampleSize = ratio;
        
        opt.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), opt);
        if (bitmap == null) return null;
        
        final float scale = agent_manager_.getContext().getResources().getDisplayMetrics().density;
        final int real_max_width = (int) (max_width * scale + 0.5f);
        final int real_max_height = (int) (max_height * scale + 0.5f);
        
        if (width > height) {
            height = (int) (((float) real_max_width * (float) height / width) + 0.5f);
            width = real_max_width;
        }
        else {
            width = (int) (((float) real_max_height * (float) width / height) + 0.5f);
            height = real_max_height;
        }
        
        if (width <= 0) width++;
        if (height <= 0) height++;
        
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
        if (bitmap == null) return null;
        
        // セーブ
        
        if (can_cache) {
            try {
                File temp_file = File.createTempFile(cache_file.getName(), ".tmp", cache_file.getParentFile());
                FileOutputStream os = new FileOutputStream(temp_file);
                bitmap.compress(CompressFormat.PNG, 100, os);
                os.flush();
                os.close();
                temp_file.renameTo(cache_file);
            }
            catch (SecurityException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }
}
