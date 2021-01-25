package xyz.fycz.myreader.ui.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import xyz.fycz.myreader.R;
import xyz.fycz.myreader.application.MyApplication;
import xyz.fycz.myreader.application.SysManager;
import xyz.fycz.myreader.base.BaseActivity;
import xyz.fycz.myreader.base.BaseFragment;
import xyz.fycz.myreader.common.APPCONST;
import xyz.fycz.myreader.databinding.ActivityMoreSettingBinding;
import xyz.fycz.myreader.entity.Setting;
import xyz.fycz.myreader.enums.BookSource;
import xyz.fycz.myreader.greendao.entity.Book;
import xyz.fycz.myreader.greendao.service.BookService;
import xyz.fycz.myreader.ui.dialog.DialogCreator;
import xyz.fycz.myreader.ui.dialog.MultiChoiceDialog;
import xyz.fycz.myreader.ui.dialog.MyAlertDialog;
import xyz.fycz.myreader.ui.fragment.PrivateBooksFragment;
import xyz.fycz.myreader.ui.fragment.WebDavFragment;
import xyz.fycz.myreader.util.SharedPreUtils;
import xyz.fycz.myreader.util.ToastUtils;
import xyz.fycz.myreader.util.utils.FileUtils;
import xyz.fycz.myreader.webapi.crawler.ReadCrawlerUtil;

import static xyz.fycz.myreader.common.APPCONST.BOOK_CACHE_PATH;

/**
 * Created by fengyue on 17-6-6.
 * 阅读界面的更多设置
 */

public class MoreSettingActivity extends BaseActivity {

    private ActivityMoreSettingBinding binding;

    private boolean needRefresh;
    private boolean upMenu;

    private Setting mSetting;
    private boolean isVolumeTurnPage;
    private int resetScreenTime;
    private int sortStyle;
    private boolean autoRefresh;
    private boolean isMatchChapter;
    private float matchChapterSuitability;
    private int catheCap;
    private boolean isShowStatusBar;
    private boolean isLongPress;
    private boolean alwaysNext;
    private boolean noMenuTitle;
    private boolean readAloudVolumeTurnPage;

    private ArrayList<Book> mBooks;
    int booksCount;
    CharSequence[] mBooksName;
    int threadNum;

    //选择禁用更新书籍对话框
    private AlertDialog mCloseRefreshDia;
    //选择禁用更新书源对话框
    private AlertDialog mDisableSourceDia;
    //选择一键缓存书籍对话框
    private AlertDialog mDownloadAllDia;

    private WebDavFragment mWebDavFragment;
    private PrivateBooksFragment mPrivateBooksFragment;

    private BaseFragment curFragment;

    @Override
    protected void bindView() {
        binding = ActivityMoreSettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        super.initData(savedInstanceState);
        needRefresh = false;
        mSetting = SysManager.getSetting();
        isVolumeTurnPage = mSetting.isVolumeTurnPage();
        alwaysNext = mSetting.isAlwaysNext();
        resetScreenTime = mSetting.getResetScreen();
        isMatchChapter = mSetting.isMatchChapter();
        matchChapterSuitability = mSetting.getMatchChapterSuitability();
        catheCap = mSetting.getCatheGap();
        sortStyle = mSetting.getSortStyle();
        autoRefresh = mSetting.isRefreshWhenStart();
        isShowStatusBar = mSetting.isShowStatusBar();
        isLongPress = mSetting.isCanSelectText();
        noMenuTitle = mSetting.isNoMenuChTitle();
        readAloudVolumeTurnPage = mSetting.isReadAloudVolumeTurnPage();
        threadNum = SharedPreUtils.getInstance().getInt(getString(R.string.threadNum), 8);
    }

    @Override
    protected void setUpToolbar(Toolbar toolbar) {
        super.setUpToolbar(toolbar);
        setStatusBarColor(R.color.colorPrimary, true);
        setUpToolbar();
    }

    private void setUpToolbar() {
        if (curFragment == null) {
            getSupportActionBar().setTitle("设置");
        } else if (curFragment == mWebDavFragment) {
            getSupportActionBar().setTitle(getString(R.string.webdav_setting));
            invalidateOptionsMenu();
        }else if (curFragment == mPrivateBooksFragment){
            getSupportActionBar().setTitle(getString(R.string.private_bookcase));
            invalidateOptionsMenu();
        }
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        initSwitchStatus();
        if (sortStyle == 1) {
            binding.tvBookSort.setText(getString(R.string.time_sort));
        } else if (sortStyle == 2) {
            binding.tvBookSort.setText(getString(R.string.book_name_sort));
        }
        if (isMatchChapter) {
            binding.rlMatchChapterSuitability.setVisibility(View.VISIBLE);
        } else {
            binding.rlMatchChapterSuitability.setVisibility(View.GONE);
        }
        binding.tvThreadNum.setText(getString(R.string.cur_thread_num, threadNum));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.webdav_help, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (curFragment == null) {
            menu.findItem(R.id.action_tip).setVisible(false);
        } else {
            menu.findItem(R.id.action_tip).setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_tip) {
            if (curFragment == mWebDavFragment) {
                DialogCreator.createAssetTipDialog(this, "如何使用WebDav进行云备份？", "webdavhelp.fy");
            }else if(curFragment == mPrivateBooksFragment){
                DialogCreator.createTipDialog(this, "关于私密书架", getString(R.string.private_bookcase_tip));
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void initSwitchStatus() {
        binding.scVolume.setChecked(isVolumeTurnPage);
        binding.scAlwaysNext.setChecked(alwaysNext);
        binding.scMatchChapter.setChecked(isMatchChapter);
        binding.scAutoRefresh.setChecked(autoRefresh);
        binding.scShowStatus.setChecked(isShowStatusBar);
        binding.scLongPress.setChecked(isLongPress);
        binding.scNoMenuTitle.setChecked(noMenuTitle);
        binding.scReadAloudVolumeTurnPage.setChecked(readAloudVolumeTurnPage);
    }

    @Override
    protected void initClick() {
        super.initClick();
        binding.llWebdav.setOnClickListener(v -> {
            binding.svContent.setVisibility(View.GONE);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (mWebDavFragment == null) {
                mWebDavFragment = new WebDavFragment();
                ft.add(R.id.ll_content, mWebDavFragment);
            } else {
                ft.show(mWebDavFragment);
            }
            ft.commit();
            curFragment = mWebDavFragment;
            setUpToolbar();
        });

        binding.rlVolume.setOnClickListener(
                (v) -> {
                    if (isVolumeTurnPage) {
                        isVolumeTurnPage = false;
                    } else {
                        isVolumeTurnPage = true;
                    }
                    binding.scVolume.setChecked(isVolumeTurnPage);
                    mSetting.setVolumeTurnPage(isVolumeTurnPage);
                    SysManager.saveSetting(mSetting);
                }
        );
        binding.rlAlwaysNext.setOnClickListener(
                (v) -> {
                    if (alwaysNext) {
                        alwaysNext = false;
                    } else {
                        alwaysNext = true;
                    }
                    binding.scAlwaysNext.setChecked(alwaysNext);
                    mSetting.setAlwaysNext(alwaysNext);
                    SysManager.saveSetting(mSetting);
                }
        );
        binding.rlShowStatus.setOnClickListener(
                (v) -> {
                    needRefresh = true;
                    if (isShowStatusBar) {
                        isShowStatusBar = false;
                    } else {
                        isShowStatusBar = true;
                    }
                    binding.scShowStatus.setChecked(isShowStatusBar);
                    mSetting.setShowStatusBar(isShowStatusBar);
                    SysManager.saveSetting(mSetting);
                }
        );
        binding.rlLongPress.setOnClickListener(
                (v) -> {
                    needRefresh = false;
                    if (isLongPress) {
                        isLongPress = false;
                    } else {
                        isLongPress = true;
                    }
                    binding.scLongPress.setChecked(isLongPress);
                    mSetting.setCanSelectText(isLongPress);
                    SysManager.saveSetting(mSetting);
                }
        );
        binding.rlContentReplace.setOnClickListener(v -> startActivity(new Intent(this, RuleActivity.class)));
        binding.rlReadAloudVolumeTurnPage.setOnClickListener(
                (v) -> {
                    if (readAloudVolumeTurnPage) {
                        readAloudVolumeTurnPage = false;
                    } else {
                        readAloudVolumeTurnPage = true;
                    }
                    binding.scReadAloudVolumeTurnPage.setChecked(readAloudVolumeTurnPage);
                    mSetting.setReadAloudVolumeTurnPage(readAloudVolumeTurnPage);
                    SysManager.saveSetting(mSetting);
                }
        );
        binding.rlNoMenuTitle.setOnClickListener(
                (v) -> {
                    upMenu = true;
                    if (noMenuTitle) {
                        noMenuTitle = false;
                    } else {
                        noMenuTitle = true;
                    }
                    binding.scNoMenuTitle.setChecked(noMenuTitle);
                    mSetting.setNoMenuChTitle(noMenuTitle);
                    SysManager.saveSetting(mSetting);
                }
        );

        binding.llBookSort.setOnClickListener(v -> {
            MyAlertDialog.build(this)
                    .setTitle(getString(R.string.book_sort))
                    .setSingleChoiceItems(R.array.book_sort, sortStyle, (dialog, which) -> {
                        sortStyle = which;
                        mSetting.setSortStyle(sortStyle);
                        SysManager.saveSetting(mSetting);
                        if (sortStyle == 0) {
                            binding.tvBookSort.setText(getString(R.string.manual_sort));
                            if (!SharedPreUtils.getInstance().getBoolean("manualSortTip")) {
                                DialogCreator.createTipDialog(this, "可在书架编辑状态下长按移动书籍进行排序！");
                                SharedPreUtils.getInstance().putBoolean("manualSortTip", true);
                            }
                        } else if (sortStyle == 1) {
                            binding.tvBookSort.setText(getString(R.string.time_sort));
                        } else if (sortStyle == 2) {
                            binding.tvBookSort.setText(getString(R.string.book_name_sort));
                        }
                        dialog.dismiss();
                    }).setNegativeButton("取消", null).show();
        });

        binding.rlPrivateBookcase.setOnClickListener(v -> {
            MyAlertDialog.showPrivateVerifyDia(this, needGoTo -> {
                showPrivateBooksFragment();
            });
        });

        binding.rlAutoRefresh.setOnClickListener(
                (v) -> {
                    if (autoRefresh) {
                        autoRefresh = false;
                    } else {
                        autoRefresh = true;
                    }
                    binding.scAutoRefresh.setChecked(autoRefresh);
                    mSetting.setRefreshWhenStart(autoRefresh);
                    SysManager.saveSetting(mSetting);
                }
        );

        binding.llCloseRefresh.setOnClickListener(v -> {
            MyApplication.runOnUiThread(() -> {
                if (mCloseRefreshDia != null) {
                    mCloseRefreshDia.show();
                    return;
                }

                initmBooks();

                if (mBooks.size() == 0) {
                    ToastUtils.showWarring("当前书架没有支持禁用更新的书籍！");
                    return;
                }

                boolean[] isCloseRefresh = new boolean[booksCount];
                int crBookCount = 0;

                for (int i = 0; i < booksCount; i++) {
                    Book book = mBooks.get(i);
                    isCloseRefresh[i] = book.getIsCloseUpdate();
                    if (isCloseRefresh[i]) {
                        crBookCount++;
                    }
                }

                mCloseRefreshDia = new MultiChoiceDialog().create(this, "禁用更新的书籍",
                        mBooksName, isCloseRefresh, crBookCount, (dialog, which) -> {
                            BookService.getInstance().updateBooks(mBooks);
                        }, null, new DialogCreator.OnMultiDialogListener() {
                            @Override
                            public void onItemClick(DialogInterface dialog, int which, boolean isChecked) {
                                mBooks.get(which).setIsCloseUpdate(isChecked);
                            }

                            @Override
                            public void onSelectAll(boolean isSelectAll) {
                                for (Book book : mBooks) {
                                    book.setIsCloseUpdate(isSelectAll);
                                }
                            }
                        });

            });
        });

        binding.llDisableSource.setOnClickListener(v -> {
            if (mDisableSourceDia != null) {
                mDisableSourceDia.show();
                return;
            }

            HashMap<CharSequence, Boolean> mSources = ReadCrawlerUtil.getDisableSources();
            CharSequence[] mSourcesName = new CharSequence[mSources.keySet().size()];
            boolean[] isDisables = new boolean[mSources.keySet().size()];
            int dSourceCount = 0;
            int i = 0;
            for (CharSequence sourceName : mSources.keySet()) {
                mSourcesName[i] = sourceName;
                Boolean isDisable = mSources.get(sourceName);
                if (isDisable == null) isDisable = false;
                if (isDisable) dSourceCount++;
                isDisables[i++] = isDisable;
            }

            mDisableSourceDia = new MultiChoiceDialog().create(this, "选择禁用的书源",
                    mSourcesName, isDisables, dSourceCount, (dialog, which) -> {
                        SharedPreUtils spu = SharedPreUtils.getInstance();
                        StringBuilder sb = new StringBuilder();
                        for (CharSequence sourceName : mSources.keySet()) {
                            if (!mSources.get(sourceName)) {
                                sb.append(BookSource.getFromName(String.valueOf(sourceName)));
                                sb.append(",");
                            }
                        }
                        if (sb.lastIndexOf(",") >= 0) sb.deleteCharAt(sb.lastIndexOf(","));
                        spu.putString(getString(R.string.searchSource), sb.toString());
                    }, null, new DialogCreator.OnMultiDialogListener() {
                        @Override
                        public void onItemClick(DialogInterface dialog, int which, boolean isChecked) {
                            mSources.put(mSourcesName[which], isChecked);
                        }

                        @Override
                        public void onSelectAll(boolean isSelectAll) {
                            for (CharSequence sourceName : mSources.keySet()) {
                                mSources.put(sourceName, isSelectAll);
                            }
                        }
                    });
        });

        binding.llThreadNum.setOnClickListener(v -> {
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_number_picker, null);
            NumberPicker threadPick = view.findViewById(R.id.number_picker);
            threadPick.setMaxValue(1024);
            threadPick.setMinValue(1);
            threadPick.setValue(threadNum);
            threadPick.setOnScrollListener((view1, scrollState) -> {

            });
            MyAlertDialog.build(this)
                    .setTitle("搜索线程数")
                    .setView(view)
                    .setPositiveButton("确定", (dialog, which) -> {
                        threadNum = threadPick.getValue();
                        SharedPreUtils.getInstance().putInt(getString(R.string.threadNum), threadNum);
                        binding.tvThreadNum.setText(getString(R.string.cur_thread_num, threadNum));
                    }).setNegativeButton("取消", null)
                    .show();
        });

        binding.rlMatchChapter.setOnClickListener(
                (v) -> {
                    if (isMatchChapter) {
                        isMatchChapter = false;
                        binding.rlMatchChapterSuitability.setVisibility(View.GONE);
                    } else {
                        isMatchChapter = true;
                        binding.rlMatchChapterSuitability.setVisibility(View.VISIBLE);
                    }
                    binding.scMatchChapter.setChecked(isMatchChapter);
                    mSetting.setMatchChapter(isMatchChapter);
                    SysManager.saveSetting(mSetting);
                }
        );


        binding.llDownloadAll.setOnClickListener(v -> {
            MyApplication.runOnUiThread(() -> {
                if (mDownloadAllDia != null) {
                    mDownloadAllDia.show();
                    return;
                }

                initmBooks();

                if (mBooks.size() == 0) {
                    ToastUtils.showWarring("当前书架没有支持缓存的书籍！");
                    return;
                }

                int booksCount = mBooks.size();
                CharSequence[] mBooksName = new CharSequence[booksCount];
                boolean[] isDownloadAll = new boolean[booksCount];
                int daBookCount = 0;
                for (int i = 0; i < booksCount; i++) {
                    Book book = mBooks.get(i);
                    mBooksName[i] = book.getName();
                    isDownloadAll[i] = book.getIsDownLoadAll();
                    if (isDownloadAll[i]) {
                        daBookCount++;
                    }
                }

                mDownloadAllDia = new MultiChoiceDialog().create(this, "一键缓存的书籍",
                        mBooksName, isDownloadAll, daBookCount, (dialog, which) -> {
                            BookService.getInstance().updateBooks(mBooks);
                        }, null, new DialogCreator.OnMultiDialogListener() {
                            @Override
                            public void onItemClick(DialogInterface dialog, int which, boolean isChecked) {
                                mBooks.get(which).setIsDownLoadAll(isChecked);
                            }

                            @Override
                            public void onSelectAll(boolean isSelectAll) {
                                for (Book book : mBooks) {
                                    book.setIsDownLoadAll(isSelectAll);
                                }
                            }
                        });

            });
        });

        binding.ivMatchChapterTip.setOnClickListener(v -> DialogCreator.createTipDialog(this, "智能匹配", getString(R.string.match_chapter_tip)));

        binding.rlMatchChapterSuitability.setOnClickListener(v -> binding.scMatchChapterSuitability.performClick());
        binding.rlResetScreen.setOnClickListener(v -> binding.scResetScreen.performClick());
        binding.rlCatheGap.setOnClickListener(v -> binding.scCatheGap.performClick());

        binding.rlDeleteCathe.setOnClickListener(v -> {
            MyApplication.runOnUiThread(() -> {
                File catheFile = getCacheDir();
                String catheFileSize = FileUtils.getFileSize(FileUtils.getDirSize(catheFile));

                File eCatheFile = new File(BOOK_CACHE_PATH);
                String eCatheFileSize;
                if (eCatheFile.exists() && eCatheFile.isDirectory()) {
                    eCatheFileSize = FileUtils.getFileSize(FileUtils.getDirSize(eCatheFile));
                } else {
                    eCatheFileSize = "0";
                }
                CharSequence[] cathes = {"章节缓存：" + eCatheFileSize, "图片缓存：" + catheFileSize};
                boolean[] catheCheck = {true, true};
                new MultiChoiceDialog().create(this, "清除缓存", cathes, catheCheck, 2,
                        (dialog, which) -> {
                            String tip = "";
                            if (catheCheck[0]) {
                                BookService.getInstance().deleteAllBookCathe();
                                tip += "章节缓存 ";
                            }
                            if (catheCheck[1]) {
                                FileUtils.deleteFile(catheFile.getAbsolutePath());
                                tip += "图片缓存 ";
                            }
                            if (tip.length() > 0) {
                                tip += "清除成功";
                                ToastUtils.showSuccess(tip);
                            }
                        }, null, null);
            });
        });

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initSpinner();
    }

    @Override
    public void finish() {
        if (curFragment == null) {
            Intent result = new Intent();
            result.putExtra(APPCONST.RESULT_NEED_REFRESH, needRefresh);
            result.putExtra(APPCONST.RESULT_UP_MENU, upMenu);
            setResult(AppCompatActivity.RESULT_OK, result);
            super.finish();
        } else {
            binding.svContent.setVisibility(View.VISIBLE);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.hide(curFragment);
            ft.commit();
            curFragment = null;
            setUpToolbar();
            invalidateOptionsMenu();
        }
    }

    private void initSpinner() {
        // initSwitchStatus() be called earlier than onCreate(), so setSelection() won't work
        ArrayAdapter<CharSequence> resetScreenAdapter = ArrayAdapter.createFromResource(this,
                R.array.reset_screen_time, android.R.layout.simple_spinner_item);
        resetScreenAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.scResetScreen.setAdapter(resetScreenAdapter);

        int resetScreenSelection = 0;
        switch (resetScreenTime) {
            case 0:
                resetScreenSelection = 0;
                break;
            case 1:
                resetScreenSelection = 1;
                break;
            case 3:
                resetScreenSelection = 2;
                break;
            case 5:
                resetScreenSelection = 3;
                break;
        }
        binding.scResetScreen.setSelection(resetScreenSelection);
        binding.scResetScreen.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        resetScreenTime = 0;
                        break;
                    case 1:
                        resetScreenTime = 1;
                        break;
                    case 2:
                        resetScreenTime = 3;
                        break;
                    case 3:
                        resetScreenTime = 5;
                        break;
                }
                mSetting.setResetScreen(resetScreenTime);
                SysManager.saveSetting(mSetting);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        ArrayAdapter<CharSequence> matchSuiAdapter = ArrayAdapter.createFromResource(this,
                R.array.match_chapter_suitability, android.R.layout.simple_spinner_item);
        matchSuiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.scMatchChapterSuitability.setAdapter(matchSuiAdapter);

        if (matchChapterSuitability == 0.0) {
            matchChapterSuitability = 0.7f;
            mSetting.setMatchChapterSuitability(matchChapterSuitability);
            SysManager.saveSetting(mSetting);
        }
        int matchSuiSelection = (int) (matchChapterSuitability * 10 - 5);

        binding.scMatchChapterSuitability.setSelection(matchSuiSelection);

        binding.scMatchChapterSuitability.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                matchChapterSuitability = (position + 5) * 1f / 10f;
                mSetting.setMatchChapterSuitability(matchChapterSuitability);
                SysManager.saveSetting(mSetting);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        ArrayAdapter<CharSequence> catheGapAdapter = ArrayAdapter.createFromResource(this,
                R.array.cathe_chapter_gap, android.R.layout.simple_spinner_item);
        catheGapAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.scCatheGap.setAdapter(catheGapAdapter);

        if (catheCap == 0) {
            catheCap = 150;
            mSetting.setCatheGap(catheCap);
            SysManager.saveSetting(mSetting);
        }
        int catheGapSelection = catheCap / 50 - 1;

        binding.scCatheGap.setSelection(catheGapSelection);

        binding.scCatheGap.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                catheCap = (position + 1) * 50;
                mSetting.setCatheGap(catheCap);
                SysManager.saveSetting(mSetting);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void initmBooks() {
        if (mBooks != null) {
            return;
        }
        mBooks = (ArrayList<Book>) BookService.getInstance().getAllBooksNoHide();

        Iterator<Book> mBooksIter = mBooks.iterator();
        while (mBooksIter.hasNext()) {
            Book book = mBooksIter.next();
            if ("本地书籍".equals(book.getType())) {
                mBooksIter.remove();
            }
        }
        booksCount = mBooks.size();
        mBooksName = new CharSequence[booksCount];

        for (int i = 0; i < booksCount; i++) {
            Book book = mBooks.get(i);
            mBooksName[i] = book.getName();
        }
    }

    private void showPrivateBooksFragment(){
        binding.svContent.setVisibility(View.GONE);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (mPrivateBooksFragment == null) {
            mPrivateBooksFragment = new PrivateBooksFragment();
            ft.add(R.id.ll_content, mPrivateBooksFragment);
        } else {
            ft.show(mPrivateBooksFragment);
            mPrivateBooksFragment.init();
        }
        ft.commit();
        curFragment = mPrivateBooksFragment;
        setUpToolbar();
    }

}
