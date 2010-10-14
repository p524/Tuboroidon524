package info.narazaki.android.tuboroid.service;

interface ITuboroidService {
    void checkUpdateFavorites(boolean background);
    void checkUpdateRecents();
    
    void checkDownloadFavorites(boolean background);
    void checkDownloadRecents();
}
