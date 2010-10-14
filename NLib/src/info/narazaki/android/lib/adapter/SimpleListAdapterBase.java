package info.narazaki.android.lib.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * シンプルなArrayListベースのAdapterの雛形
 * 
 * @author H.Narazaki
 * 
 * @param <T_DATA>
 *            内部ArrayListに格納されるデータの型
 */
abstract public class SimpleListAdapterBase<T_DATA extends NListAdapterDataInterface> extends BaseAdapter {
    protected ArrayList<T_DATA> data_list_;
    private final ExecutorService executor_;
    
    public SimpleListAdapterBase() {
        super();
        executor_ = createExecutorService();
    }
    
    public void postAdapterThread(final Runnable r) {
        executor_.submit(r);
    }
    
    protected ExecutorService createExecutorService() {
        return Executors.newSingleThreadExecutor();
    }
    
    /**
     * リスト1行分のViewを生成して返す
     * 
     * @return 生成されたView
     */
    abstract protected View createView(T_DATA data);
    
    /**
     * 指定のviewに対してdataに応じた内容を設定して返す
     * 
     * @param view
     * @param data
     * @return
     */
    abstract protected View setView(View view, T_DATA data, ViewGroup parent);
    
    protected ArrayList<T_DATA> getDataList() {
        if (data_list_ == null) data_list_ = new ArrayList<T_DATA>();
        return data_list_;
    }
    
    /**
     * データをクリアし、画面上に反映させる
     */
    public void clear() {
        clearData();
        notifyDataSetChanged();
    }
    
    /**
     * データをクリアする。画面上には即時反映されない (新たにデータを登録しなおすときなどに使う)
     */
    public void clearData() {
        getDataList().clear();
    }
    
    @Override
    public int getCount() {
        return getDataList().size();
    }
    
    @Override
    public Object getItem(int position) {
        if (getCount() <= position || position < 0) return null;
        return getDataList().get(position);
    }
    
    /**
     * (getItemと違い)T_DATA型で要素を取り出す
     * 
     * @param position
     * @return
     */
    public T_DATA getData(int position) {
        if (getCount() <= position || position < 0) return null;
        return getDataList().get(position);
    }
    
    @Override
    public long getItemId(int position) {
        if (getCount() <= position || position < 0) return 0;
        return getData(position).getId();
    }
    
    /**
     * データを1件追加する
     * 
     * @param data
     */
    public void addData(T_DATA data) {
        getDataList().add(data);
    }
    
    /**
     * データを設定する
     * 
     * @param data_list
     */
    public void setDataList(ArrayList<T_DATA> data_list) {
        data_list_ = data_list;
        notifyDataSetChanged();
    }
    
    public void addDataList(final List<T_DATA> data_list) {
        data_list_.addAll(data_list);
        notifyDataSetChanged();
    }
    
    @Override
    public View getView(int position, View convert_view, ViewGroup parent) {
        if (getCount() <= position || position < 0) return convert_view;
        T_DATA data = getData(position);
        if (convert_view == null) convert_view = createView(data);
        
        return setView(convert_view, data, parent);
    }
}
