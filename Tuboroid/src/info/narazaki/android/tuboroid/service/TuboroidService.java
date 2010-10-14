package info.narazaki.android.tuboroid.service;

import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.activity.NotificationReceiverActivity;
import info.narazaki.android.tuboroid.agent.FavoriteCacheListAgent;
import info.narazaki.android.tuboroid.agent.FavoriteCacheListAgent.FavoriteThreadListFetchedCallback;
import info.narazaki.android.tuboroid.agent.ThreadEntryListAgent;
import info.narazaki.android.tuboroid.agent.ThreadListAgent;
import info.narazaki.android.tuboroid.agent.ThreadListAgent.RecentListFetchedCallback;
import info.narazaki.android.tuboroid.agent.TuboroidAgent;
import info.narazaki.android.tuboroid.data.BoardData;
import info.narazaki.android.tuboroid.data.ThreadData;
import info.narazaki.android.tuboroid.data.ThreadEntryData;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;

public class TuboroidService extends Service {
    private static final String TAG = "TuboroidService";
    private static ExecutorService executor_;
    private static boolean check_update_in_progress_ = false;
    
    private WakeLock wake_lock_;
    
    public static class CHECK_UPDATE {
        public static final String ACTION_NEW = "info.narazaki.android.tuboroid.service.TuboroidService.CHECK_UPDATE";
        public static final String MAX = "MAX";
        public static final String PROGRESS1 = "PROGRESS1";
        public static final String PROGRESS2 = "PROGRESS2";
        
        public static final String ACTION_FINISHED = "info.narazaki.android.tuboroid.service.TuboroidService.CHECK_UPDATE_FINISHED";
        public static final String NUM_UNREAD_THREADS = "NUM_UNREAD_THREADS";
    }
    
    private synchronized boolean lockCheckUpdateInProgress() {
        if (check_update_in_progress_) return false;
        check_update_in_progress_ = true;
        getWakeLock().acquire();
        return true;
    }
    
    private synchronized boolean unlockCheckUpdateInProgress() {
        if (!check_update_in_progress_) return false;
        check_update_in_progress_ = false;
        getWakeLock().release();
        return true;
    }
    
    private ITuboroidService.Stub service_impl_ = new ITuboroidService.Stub() {
        private int check_update_max_ = 0;
        private int check_update_remain_ = 0;
        private boolean check_update_in_background_ = false;
        private int check_updated_new_threads_ = 0;
        private int check_updated_unread_threads_ = 0;
        
        private void onEndChecking() {
            if (check_update_in_background_ && check_updated_new_threads_ > 0) {
                String message = String.valueOf(check_updated_unread_threads_) + " "
                        + getString(R.string.notif_check_upadted);
                
                NotificationManager notif_manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                Notification notif = new Notification(R.drawable.stat_notify_updated, message,
                        System.currentTimeMillis());
                Intent intent = new Intent(TuboroidService.this, NotificationReceiverActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                PendingIntent pending = PendingIntent.getActivity(TuboroidService.this, 0, intent, 0);
                notif.setLatestEventInfo(TuboroidService.this, getString(R.string.app_name), message, pending);
                notif_manager.notify(TuboroidApplication.NOTIF_ID_BACKGROUND_UPDATED, notif);
            }
            Intent intent = new Intent(CHECK_UPDATE.ACTION_FINISHED);
            intent.putExtra(CHECK_UPDATE.NUM_UNREAD_THREADS, check_updated_unread_threads_);
            sendBroadcast(intent);
            
            stopSelf();
            unlockCheckUpdateInProgress();
        }
        
        private void onBeginChecking(final boolean background) {
            check_update_in_background_ = background;
            check_updated_new_threads_ = 0;
            check_updated_unread_threads_ = 0;
        }
        
        // お気に入り更新チェック
        @Override
        public void checkUpdateFavorites(final boolean background) throws RemoteException {
            if (!lockCheckUpdateInProgress()) return;
            onBeginChecking(background);
            executor_.submit(new Runnable() {
                @Override
                public void run() {
                    getAgent().fetchBoardOfFavoriteThreadList(
                            new FavoriteCacheListAgent.UpdateCheckListFetchedCallback() {
                                @Override
                                public void onUpdateCheckListFetched(final List<BoardData> data_list) {
                                    checkUpdateBoardList(data_list, null);
                                }
                            });
                }
            });
        }
        
        // 履歴更新チェック
        @Override
        public void checkUpdateRecents() throws RemoteException {
            if (!lockCheckUpdateInProgress()) return;
            onBeginChecking(false);
            executor_.submit(new Runnable() {
                @Override
                public void run() {
                    getAgent().fetchBoardOfRecentThreadList(
                            new FavoriteCacheListAgent.UpdateCheckListFetchedCallback() {
                                @Override
                                public void onUpdateCheckListFetched(final List<BoardData> data_list) {
                                    checkUpdateBoardList(data_list, null);
                                }
                            });
                }
            });
        }
        
        // お気に入りダウンロード
        @Override
        public void checkDownloadFavorites(final boolean background) throws RemoteException {
            if (!lockCheckUpdateInProgress()) return;
            onBeginChecking(background);
            executor_.submit(new Runnable() {
                @Override
                public void run() {
                    getAgent().fetchBoardOfFavoriteThreadList(
                            new FavoriteCacheListAgent.UpdateCheckListFetchedCallback() {
                                @Override
                                public void onUpdateCheckListFetched(final List<BoardData> data_list) {
                                    checkUpdateBoardList(data_list, new Runnable() {
                                        @Override
                                        public void run() {
                                            downloadFavorites();
                                        }
                                    });
                                }
                            });
                }
            });
        }
        
        // 履歴ダウンロード
        @Override
        public void checkDownloadRecents() throws RemoteException {
            if (!lockCheckUpdateInProgress()) return;
            onBeginChecking(false);
            executor_.submit(new Runnable() {
                @Override
                public void run() {
                    getAgent().fetchBoardOfRecentThreadList(
                            new FavoriteCacheListAgent.UpdateCheckListFetchedCallback() {
                                @Override
                                public void onUpdateCheckListFetched(final List<BoardData> data_list) {
                                    checkUpdateBoardList(data_list, new Runnable() {
                                        @Override
                                        public void run() {
                                            downloadRecents();
                                        }
                                    });
                                }
                            });
                }
            });
        }
        
        private void checkUpdateBoardList(final List<BoardData> data_list, final Runnable on_checked_callback) {
            check_update_remain_ = data_list.size();
            check_update_max_ = data_list.size();
            
            sendCheckedUpdateIntent(on_checked_callback);
            
            for (BoardData data : data_list) {
                checkUpdateBoard(data, on_checked_callback);
            }
        }
        
        private void checkUpdateBoard(final BoardData board_data, final Runnable on_checked_callback) {
            executor_.submit(new Runnable() {
                @Override
                public void run() {
                    final LinkedList<ThreadData> new_data_list = new LinkedList<ThreadData>();
                    getAgent().fetchThreadList(board_data, true, new ThreadListAgent.ThreadListFetchedCallback() {
                        
                        @Override
                        public void onThreadListFetchCompleted() {
                            onCheckedUpdateBoard(new_data_list, on_checked_callback);
                        }
                        
                        @Override
                        public void onThreadListFetchFailed(final boolean maybe_moved) {
                            onCheckedUpdateBoard(new_data_list, on_checked_callback);
                        }
                        
                        @Override
                        public void onThreadListFetchedCache(List<ThreadData> dataList) {
                            new_data_list.addAll(dataList);
                        }
                        
                        @Override
                        public void onThreadListFetched(List<ThreadData> dataList) {}
                        
                        @Override
                        public void onInterrupted() {
                            onCheckedUpdateBoard(new_data_list, on_checked_callback);
                        }
                        
                        @Override
                        public void onConnectionOffline() {
                            onCheckedUpdateBoard(new_data_list, on_checked_callback);
                        }
                        
                    });
                }
            });
        }
        
        private void onCheckedUpdateBoard(LinkedList<ThreadData> new_data_list, final Runnable on_checked_callback) {
            for (ThreadData data : new_data_list) {
                if (data.is_favorite_) {
                    if (data.online_count_ > data.read_count_) {
                        if (data.new_online_count_ > 0) {
                            check_updated_new_threads_++;
                        }
                        check_updated_unread_threads_++;
                    }
                }
            }
            check_update_remain_--;
            sendCheckedUpdateIntent(on_checked_callback);
        }
        
        private void sendCheckedUpdateIntent(final Runnable on_checked_callback) {
            Intent intent = new Intent(CHECK_UPDATE.ACTION_NEW);
            intent.putExtra(CHECK_UPDATE.MAX, check_update_max_);
            if (on_checked_callback == null) {
                intent.putExtra(CHECK_UPDATE.PROGRESS1, check_update_max_ - check_update_remain_);
                intent.putExtra(CHECK_UPDATE.PROGRESS2, 0);
            }
            else {
                intent.putExtra(CHECK_UPDATE.PROGRESS1, 0);
                intent.putExtra(CHECK_UPDATE.PROGRESS2, check_update_max_ - check_update_remain_);
            }
            sendBroadcast(intent);
            
            if (check_update_remain_ == 0) {
                if (on_checked_callback == null) {
                    onEndChecking();
                }
                else {
                    on_checked_callback.run();
                }
                return;
            }
        }
        
        private void downloadFavorites() {
            executor_.submit(new Runnable() {
                @Override
                public void run() {
                    getAgent().fetchFavoriteThreadList(new FavoriteThreadListFetchedCallback() {
                        @Override
                        public void onThreadListFetched(ArrayList<ThreadData> dataList) {
                            beginDownloadThreadList(dataList);
                        }
                    });
                }
            });
        }
        
        private void downloadRecents() {
            executor_.submit(new Runnable() {
                @Override
                public void run() {
                    getAgent().fetchRecentList(ThreadData.KEY.RECENT_ORDER_READ, new RecentListFetchedCallback() {
                        @Override
                        public void onRecentListFetched(ArrayList<ThreadData> dataList) {
                            beginDownloadThreadList(dataList);
                        }
                    });
                }
            });
        }
        
        private void beginDownloadThreadList(final List<ThreadData> data_list) {
            ArrayList<ThreadData> unread_data_list = new ArrayList<ThreadData>();
            for (ThreadData thread_data : data_list) {
                if (thread_data.online_count_ > thread_data.cache_count_) {
                    unread_data_list.add(thread_data);
                }
            }
            
            check_update_max_ = unread_data_list.size();
            if (unread_data_list.size() > 0) {
                downloadThreadList(unread_data_list);
            }
            else {
                onDownloadThreadProgress(unread_data_list);
            }
        }
        
        private void downloadThreadList(final List<ThreadData> data_list) {
            ThreadData thread_data = data_list.get(0);
            data_list.remove(0);
            
            getAgent().reloadThreadEntryList(thread_data, true,
                    new ThreadEntryListAgent.ThreadEntryListAgentCallback() {
                        
                        @Override
                        public void onThreadEntryListFetchedCompleted(ThreadData threadData, final boolean is_analyzed) {
                            onDownloadThreadProgress(data_list);
                        }
                        
                        @Override
                        public void onThreadEntryListFetchedByCache(List<ThreadEntryData> dataList) {}
                        
                        @Override
                        public void onThreadEntryListFetched(List<ThreadEntryData> dataList) {}
                        
                        @Override
                        public void onThreadEntryListFetchStarted(ThreadData threadData) {}
                        
                        @Override
                        public void onThreadEntryListClear() {}
                        
                        @Override
                        public void onInterrupted() {
                            onDownloadThreadProgress(data_list);
                        }
                        
                        @Override
                        public void onDatDropped(boolean isPermanently) {
                            onDownloadThreadProgress(data_list);
                        }
                        
                        @Override
                        public void onConnectionFailed(boolean connectionFailed) {
                            onDownloadThreadProgress(data_list);
                        }
                        
                        @Override
                        public void onConnectionOffline(ThreadData threadData) {
                            onDownloadThreadProgress(data_list);
                        }
                    });
        }
        
        private void onDownloadThreadProgress(final List<ThreadData> data_list) {
            Intent intent = new Intent(CHECK_UPDATE.ACTION_NEW);
            intent.putExtra(CHECK_UPDATE.MAX, check_update_max_);
            intent.putExtra(CHECK_UPDATE.PROGRESS1, check_update_max_ - data_list.size());
            intent.putExtra(CHECK_UPDATE.PROGRESS2, check_update_max_);
            sendBroadcast(intent);
            
            if (data_list.size() == 0) {
                onEndChecking();
                return;
            }
            downloadThreadList(data_list);
        }
    };
    
    public TuboroidService() {
        super();
        executor_ = Executors.newSingleThreadExecutor();
        wake_lock_ = null;
    }
    
    private WakeLock getWakeLock() {
        if (wake_lock_ == null) {
            PowerManager pm = (PowerManager) getApplication().getSystemService(Context.POWER_SERVICE);
            wake_lock_ = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Tuboroid Update Service");
        }
        return wake_lock_;
    }
    
    protected TuboroidApplication getTuboroidApplication() {
        return ((TuboroidApplication) getApplication());
    }
    
    public TuboroidAgent getAgent() {
        return getTuboroidApplication().getAgent();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return service_impl_;
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
        
    }
    
}
