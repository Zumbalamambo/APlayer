package remix.myplayer.fragment;

import android.support.v4.app.Fragment;

import butterknife.Unbinder;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/7/27 15:18
 */
public class BaseFragment extends Fragment {
    protected Unbinder mUnBinder;
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnBinder.unbind();
    }

}
