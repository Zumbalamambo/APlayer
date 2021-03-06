package remix.myplayer.ui.dialog;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.view.SimpleDraweeView;
import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.request.LibraryUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.ImageUriUtil;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.ToastUtil;

import static remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;

/**
 * Created by Remix on 2015/12/6.
 */

/**
 * 歌曲的选项对话框
 */
public class OptionDialog extends BaseDialogActivity {
    //添加 设置铃声 分享 删除按钮
    @BindView(R.id.popup_add)
    View mAdd;
    @BindView(R.id.popup_ring)
    View mRing;
    @BindView(R.id.popup_share)
    View mShare;
    @BindView(R.id.popup_delete)
    View mDelete;

    //标题
    @BindView(R.id.popup_title)
    TextView mTitle;
    //专辑封面
    @BindView(R.id.popup_image)
    SimpleDraweeView mDraweeView;

    //当前正在播放的歌曲
    private Song mInfo = null;
    //是否是删除播放列表中歌曲
    private boolean mIsDeletePlayList = false;
    //播放列表名字
    private String mPlayListName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_option);
        ButterKnife.bind(this);

        mInfo = getIntent().getExtras().getParcelable("Song");
        if(mInfo == null)
            return;
        if(mIsDeletePlayList = getIntent().getExtras().getBoolean("IsDeletePlayList", false)){
            mPlayListName = getIntent().getExtras().getString("PlayListName");
        }

        //设置歌曲名与封面
        mTitle.setText(String.format("%s-%s", mInfo.getTitle(), mInfo.getArtist()));
        new LibraryUriRequest(mDraweeView, ImageUriUtil.getSearchRequestWithAlbumType(mInfo),new RequestConfig.Builder(SMALL_IMAGE_SIZE,SMALL_IMAGE_SIZE).build()).load();
        //置于底部
        Window w = getWindow();
//        w.setWindowAnimations(R.style.AnimBottom);
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = (metrics.widthPixels);
        w.setAttributes(lp);
        w.setGravity(Gravity.BOTTOM);

        //为按钮着色
        final int tintColor = ThemeStore.isDay() ?
                ColorUtil.getColor(R.color.day_textcolor_primary ) :
                ColorUtil.getColor(R.color.white_f4f4f5);
        ((ImageView)findViewById(R.id.popup_add_img)).setImageDrawable(Theme.TintDrawable(getResources().getDrawable(R.drawable.pop_btn_add2list),tintColor));
        ((ImageView)findViewById(R.id.popup_ring_img)).setImageDrawable(Theme.TintDrawable(getResources().getDrawable(R.drawable.pop_btn_ring),tintColor));
        ((ImageView)findViewById(R.id.popup_share_img)).setImageDrawable(Theme.TintDrawable(getResources().getDrawable(R.drawable.pop_btn_share),tintColor));
        ((ImageView)findViewById(R.id.popup_delete_img)).setImageDrawable(Theme.TintDrawable(getResources().getDrawable(R.drawable.pop_btn_delete),tintColor));

        ButterKnife.apply(new TextView[]{findView(R.id.popup_add_text),findView(R.id.popup_ring_text),
                        findView(R.id.popup_share_text),findView(R.id.popup_delete_text)},
                (ButterKnife.Action<TextView>) (textView, index) -> textView.setTextColor(tintColor));
    }

    @OnClick({R.id.popup_add,R.id.popup_share,R.id.popup_delete,R.id.popup_ring})
    public void onClick(View v){
        switch (v.getId()){
            //添加到播放列表
            case R.id.popup_add:
                MobclickAgent.onEvent(this,"AddtoPlayList");
                Intent intentAdd = new Intent(OptionDialog.this,AddtoPlayListDialog.class);
                Bundle ardAdd = new Bundle();
                ardAdd.putInt("Id",mInfo.getId());
                intentAdd.putExtras(ardAdd);
                startActivity(intentAdd);
                finish();
                break;
            //设置铃声
            case R.id.popup_ring:
                MobclickAgent.onEvent(this,"Ring");
                setRing(mInfo.getId());
                finish();
                break;
            //分享
            case R.id.popup_share:
//                Intent intent = new Intent(MusicService.ACTION_CMD);
//                intent.putExtra("Control",Constants.ADD_TO_NEXT_SONG);
//                intent.putExtra("song",mInfo);
//                sendBroadcast(intent);
//                finish();
                MobclickAgent.onEvent(this,"Share");
                Intent intentShare = new Intent(OptionDialog.this,ShareDialog.class);
                Bundle argShare = new Bundle();
                argShare.putParcelable("Song",mInfo);
                argShare.putInt("Type",Constants.SHARESONG);
                intentShare.putExtras(argShare);
                startActivity(intentShare);
                finish();
                break;
            //删除
            case R.id.popup_delete:
                MobclickAgent.onEvent(this,"Delete");
                try {
                    String title = getString(R.string.confirm_delete_from_playlist_or_library,mIsDeletePlayList ? mPlayListName : "曲库");
                    new MaterialDialog.Builder(OptionDialog.this)
                            .content(title)
                            .buttonRippleColor(ThemeStore.getRippleColor())
                            .positiveText(R.string.confirm)
                            .negativeText(R.string.cancel)
                            .onPositive((dialog, which) -> {
                                boolean deleteSuccess = !mIsDeletePlayList ?
                                        MediaStoreUtil.delete(mInfo.getId() , Constants.SONG) > 0 :
                                        PlayListUtil.deleteSong(mInfo.getId(),mPlayListName);
//                                    if(deleteSuccess){
//                                        sendBroadcast(new Intent(MusicService.ACTION_MEDIA_CHANGE));
//                                    }
                                ToastUtil.show(OptionDialog.this,deleteSuccess ? R.string.delete_success : R.string.delete_error);
                                finish();
                            })
                            .onNegative((dialog, which) -> {
                            })
                            .backgroundColorAttr(R.attr.background_color_3)
                            .positiveColorAttr(R.attr.text_color_primary)
                            .negativeColorAttr(R.attr.text_color_primary)
                            .contentColorAttr(R.attr.text_color_primary)
                            .show();
                } catch (Exception e){
                    e.printStackTrace();
                }
                break;

        }
    }


    /**
     * 设置铃声
     * @param audioId
     */
    private void setRing(int audioId) {
        try {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
            cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
            cv.put(MediaStore.Audio.Media.IS_ALARM, false);
            cv.put(MediaStore.Audio.Media.IS_MUSIC, true);
            // 把需要设为铃声的歌曲更新铃声库
            if(getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv, MediaStore.MediaColumns._ID + "=?", new String[]{audioId + ""}) > 0) {
                Uri newUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioId);
                RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, newUri);
                ToastUtil.show(this,R.string.set_ringtone_success);
            }
            else
                ToastUtil.show(this,R.string.set_ringtone_error);
        }catch (Exception e){
            //没有权限
            if(e instanceof SecurityException){
                ToastUtil.show(mContext,R.string.please_give_write_settings_permission);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.System.canWrite(mContext)) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        intent.setData(Uri.parse("package:" + mContext.getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                    }
                }
            }

        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.slide_bottom_in,0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_bottom_out);
    }


}
