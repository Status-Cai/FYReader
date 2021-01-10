package xyz.fycz.myreader.ui.fragment;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import xyz.fycz.myreader.R;
import xyz.fycz.myreader.base.BaseFragment;
import xyz.fycz.myreader.base.observer.MySingleObserver;
import xyz.fycz.myreader.common.APPCONST;
import xyz.fycz.myreader.model.storage.BackupRestoreUi;
import xyz.fycz.myreader.model.storage.WebDavHelp;
import xyz.fycz.myreader.ui.dialog.MyAlertDialog;
import xyz.fycz.myreader.util.SharedPreUtils;
import xyz.fycz.myreader.util.StringHelper;
import xyz.fycz.myreader.util.ToastUtils;

/**
 * @author fengyue
 * @date 2021/1/9 12:08
 */
public class WebDavFragment extends BaseFragment {
    @BindView(R.id.webdav_setting_webdav_url)
    LinearLayout llWebdavUrl;
    @BindView(R.id.tv_webdav_url)
    TextView tvWebdavUrl;
    @BindView(R.id.webdav_setting_webdav_account)
    LinearLayout llWebdavAccount;
    @BindView(R.id.tv_webdav_account)
    TextView tvWebdavAccount;
    @BindView(R.id.webdav_setting_webdav_password)
    LinearLayout llWebdavPassword;
    @BindView(R.id.tv_webdav_password)
    TextView tvWebdavPassword;
    @BindView(R.id.webdav_setting_webdav_restore)
    LinearLayout llWebdavRestore;
    @BindView(R.id.ll_restore_num)
    LinearLayout mLlRestoreNum;
    @BindView(R.id.tv_restore_num)
    TextView mTvRestoreNum;

    private String webdavUrl;
    private String webdavAccount;
    private String webdavPassword;
    private int restoreNum;
    @Override
    protected int getContentId() {
        return R.layout.fragment_webdav_setting;
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        super.initData(savedInstanceState);
        webdavUrl = SharedPreUtils.getInstance().getString("webdavUrl", APPCONST.DEFAULT_WEB_DAV_URL);
        webdavAccount = SharedPreUtils.getInstance().getString("webdavAccount", "");
        webdavPassword = SharedPreUtils.getInstance().getString("webdavPassword", "");
        restoreNum = SharedPreUtils.getInstance().getInt("restoreNum", 30);
    }

    @Override
    protected void initWidget(Bundle savedInstanceState) {
        super.initWidget(savedInstanceState);
        tvWebdavUrl.setText(webdavUrl);
        tvWebdavAccount.setText(StringHelper.isEmpty(webdavAccount) ? "请输入WebDav账号" : webdavAccount);
        tvWebdavPassword.setText(StringHelper.isEmpty(webdavPassword) ? "请输入WebDav授权密码" : "************");
        mTvRestoreNum.setText(getString(R.string.cur_restore_list_num, restoreNum));
    }

    @Override
    protected void initClick() {
        super.initClick();
        final String[] webdavTexts = new String[3];
        llWebdavUrl.setOnClickListener(v -> {
            MyAlertDialog.createInputDia(getContext(), getString(R.string.webdav_url),
                    "", webdavUrl.equals(APPCONST.DEFAULT_WEB_DAV_URL) ?
                            "" : webdavUrl, true, 100,
                    text -> webdavTexts[0] = text,
                    (dialog, which) -> {
                        webdavUrl = webdavTexts[0];
                        tvWebdavUrl.setText(webdavUrl);
                        SharedPreUtils.getInstance().putString("webdavUrl", webdavUrl);
                        dialog.dismiss();
                    });
        });
        llWebdavAccount.setOnClickListener(v -> {
            MyAlertDialog.createInputDia(getContext(), getString(R.string.webdav_account),
                    "", webdavAccount, true, 100,
                    text -> webdavTexts[1] = text,
                    (dialog, which) -> {
                        webdavAccount = webdavTexts[1];
                        tvWebdavAccount.setText(webdavAccount);
                        SharedPreUtils.getInstance().putString("webdavAccount", webdavAccount);
                        dialog.dismiss();
                    });
        });
        llWebdavPassword.setOnClickListener(v -> {
            MyAlertDialog.createInputDia(getContext(), getString(R.string.webdav_password),
                    "", webdavPassword, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    true, 100,
                    text -> webdavTexts[2] = text,
                    (dialog, which) -> {
                        webdavPassword = webdavTexts[2];
                        tvWebdavPassword.setText("************");
                        SharedPreUtils.getInstance().putString("webdavPassword", webdavPassword);
                        dialog.dismiss();
                    });
        });
        llWebdavRestore.setOnClickListener(v -> {
            Single.create((SingleOnSubscribe<ArrayList<String>>) emitter -> {
                emitter.onSuccess(WebDavHelp.INSTANCE.getWebDavFileNames());
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new MySingleObserver<ArrayList<String>>() {
                        @Override
                        public void onSuccess(ArrayList<String> strings) {
                            if (!WebDavHelp.INSTANCE.showRestoreDialog(getContext(), strings, BackupRestoreUi.INSTANCE)) {
                                ToastUtils.showWarring("没有备份");
                            }
                        }
                    });
        });

        mLlRestoreNum.setOnClickListener(v -> {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_number_picker, null);
            NumberPicker threadPick = view.findViewById(R.id.number_picker);
            threadPick.setMaxValue(100);
            threadPick.setMinValue(10);
            threadPick.setValue(restoreNum);
            threadPick.setOnScrollListener((view1, scrollState) -> {

            });
            MyAlertDialog.build(getContext())
                    .setTitle("最大显示数")
                    .setView(view)
                    .setPositiveButton("确定", (dialog, which) -> {
                        restoreNum = threadPick.getValue();
                        SharedPreUtils.getInstance().putInt("restoreNum", restoreNum);
                        mTvRestoreNum.setText(getString(R.string.cur_restore_list_num, restoreNum));
                    }).setNegativeButton("取消", null)
                    .show();
        });
    }
}
