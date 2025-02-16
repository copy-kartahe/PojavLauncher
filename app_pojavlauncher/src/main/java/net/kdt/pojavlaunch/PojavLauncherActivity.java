package net.kdt.pojavlaunch;

import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.VerticalTabLayout.ViewPagerAdapter;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.viewpager.widget.ViewPager;

import net.kdt.pojavlaunch.fragments.ConsoleFragment;
import net.kdt.pojavlaunch.fragments.CrashFragment;
import net.kdt.pojavlaunch.fragments.LauncherFragment;
import net.kdt.pojavlaunch.prefs.LauncherPreferenceFragment;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_HIDE_SIDEBAR;

public class PojavLauncherActivity extends BaseLauncherActivity
{

    private ViewPager viewPager;

    private TextView tvUsernameView, tvConnectStatus;
    private Spinner accountSelector;
    private ViewPagerAdapter viewPageAdapter;
    private final Button[] Tabs = new Button[4];
    private View selected;

    private Button switchUsrBtn, logoutBtn; // MineButtons

    public PojavLauncherActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launcher_main_v4);


        if (BuildConfig.DEBUG) {
            Toast.makeText(this, "Launcher process id: " + android.os.Process.myPid(), Toast.LENGTH_LONG).show();
        }


        viewPager = findViewById(R.id.launchermainTabPager);
        selected = findViewById(R.id.viewTabSelected);

        mConsoleView = new ConsoleFragment();
        mCrashView = new CrashFragment();

        viewPageAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPageAdapter.addFragment(new LauncherFragment(), 0, getString(R.string.mcl_tab_news));
        viewPageAdapter.addFragment(mConsoleView, 0, getString(R.string.mcl_tab_console));
        viewPageAdapter.addFragment(mCrashView, 0, getString(R.string.mcl_tab_crash));
        viewPageAdapter.addFragment(new LauncherPreferenceFragment(), 0, getString(R.string.mcl_option_settings));

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                setTabActive(position);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        viewPager.setAdapter(viewPageAdapter);

        tvConnectStatus = (TextView) findViewById(R.id.launchermain_text_accountstatus);
        tvUsernameView = (TextView) findViewById(R.id.launchermain_text_welcome);
        mTextVersion = (TextView) findViewById(R.id.launcherMainVersionView);

        //The following line is used to make this TextView horizontally scroll if the version name is larger than the view
        mTextVersion.setSelected(true);

        Tabs[0] = findViewById(R.id.btnTab1);
        Tabs[1] = findViewById(R.id.btnTab2);
        Tabs[2] = findViewById(R.id.btnTab3);
        Tabs[3] = findViewById(R.id.btnTab4);


        pickAccount();
        
/*
        File logFile = new File(Tools.MAIN_PATH, "latestlog.txt");
        if (logFile.exists() && logFile.length() < 20480) {
            String errMsg = "Error occurred during initialization of ";
            try {
                String logContent = Tools.read(logFile.getAbsolutePath());
                if (logContent.contains(errMsg + "VM") && 
                    logContent.contains("Could not reserve enough space for")) {
                    OutOfMemoryError ex = new OutOfMemoryError("Java error: " + logContent);
                    ex.setStackTrace(null);
                    Tools.showError(PojavLauncherActivity.this, ex);

                    // Do it so dialog will not shown for second time
                    Tools.write(logFile.getAbsolutePath(), logContent.replace(errMsg + "VM", errMsg + "JVM"));
                }
            } catch (Throwable th) {
                Log.w(Tools.APP_NAME, "Could not detect java crash", th);
            }
        }
*/
        //showProfileInfo();

        final List<String> accountList = new ArrayList<String>();
        final MinecraftAccount tempProfile = PojavProfile.getTempProfileContent(this);
        if (tempProfile != null) {
            accountList.add(tempProfile.username);
        }
        for (String s : new File(Tools.DIR_ACCOUNT_NEW).list()) {
            accountList.add(s.substring(0, s.length() - 5));
        }
        
        ArrayAdapter<String> adapterAcc = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, accountList);
        adapterAcc.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        accountSelector = (Spinner) findViewById(R.id.launchermain_spinner_account);
        accountSelector.setAdapter(adapterAcc);
        if (tempProfile != null) {
            accountSelector.setSelection(0);
        } else {
            for (int i = 0; i < accountList.size(); i++) {
                String account = accountList.get(i);
                if (account.equals(mProfile.username)) {
                    accountSelector.setSelection(i);
                }
            }
        }

        accountSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> p1, View p2, int position, long p4) {
                if (tempProfile != null && position == 0) {
                    PojavProfile.setCurrentProfile(PojavLauncherActivity.this, tempProfile);
                } else {
                    PojavProfile.setCurrentProfile(PojavLauncherActivity.this,
                        accountList.get(position + (tempProfile != null ? 1 : 0)));
                }
                pickAccount();
            }

            @Override
            public void onNothingSelected(AdapterView<?> p1) {
                // TODO: Implement this method
            }
        });
        
        List<String> versions = new ArrayList<String>();
        final File fVers = new File(Tools.DIR_HOME_VERSION);

        try {
            if (fVers.listFiles().length < 1) {
                throw new Exception(getString(R.string.error_no_version));
            }

            for (File fVer : fVers.listFiles()) {
                if (fVer.isDirectory())
                    versions.add(fVer.getName());
            }
        } catch (Exception e) {
            versions.add(getString(R.string.global_error) + ":");
            versions.add(e.getMessage());

        } finally {
            mAvailableVersions = versions.toArray(new String[0]);
        }

        //mAvailableVersions;
        ArrayAdapter<String> adapterVer = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mAvailableVersions);
        adapterVer.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        mVersionSelector = (Spinner) findViewById(R.id.launchermain_spinner_version);
        mVersionSelector.setAdapter(adapterVer);

        mLaunchProgress = (ProgressBar) findViewById(R.id.progressDownloadBar);
        mLaunchTextStatus = (TextView) findViewById(R.id.progressDownloadText);
        switchUsrBtn = (Button) findViewById(R.id.infoDevBtn);
        logoutBtn = (Button) findViewById(R.id.switchUserBtn);

        mPlayButton = (Button) findViewById(R.id.launchermainPlayButton);

        statusIsLaunching(false);

        initTabs(0);
        LauncherPreferences.DEFAULT_PREF.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key.equals("hideSidebar")) {
                    restoreOldLook(sharedPreferences.getBoolean("hideSidebar",false));
                }
            }
        });
        restoreOldLook(PREF_HIDE_SIDEBAR);
    }



    private void selectTabPage(int pageIndex){
        viewPager.setCurrentItem(pageIndex);
        setTabActive(pageIndex);
    }

    private void pickAccount() {
        try {
            mProfile = PojavProfile.getCurrentProfileContent(this);

            tvUsernameView.setText(getString(R.string.main_welcome, mProfile.username));
            tvConnectStatus.setText(mProfile.accessToken.equals("0") ? R.string.mcl_account_offline : R.string.mcl_account_connected);
        } catch(Exception e) {
            mProfile = new MinecraftAccount();
            Tools.showError(this, e, true);
        }
    }

    public void statusIsLaunching(boolean isLaunching) {
        int launchVisibility = isLaunching ? View.VISIBLE : View.GONE;
        mLaunchProgress.setVisibility(launchVisibility);
        mLaunchTextStatus.setVisibility(launchVisibility);

        switchUsrBtn.setEnabled(!isLaunching);
        logoutBtn.setEnabled(!isLaunching);
        mVersionSelector.setEnabled(!isLaunching);
        canBack = !isLaunching;
    }

    public void onTabClicked(View view) {
        for(int i=0; i<Tabs.length;i++){
            if(view.getId() == Tabs[i].getId()) {
                selectTabPage(i);
                return;
            }
        }
    }

    private void setTabActive(int index){
        for (Button tab : Tabs) {
            tab.setTypeface(null, Typeface.NORMAL);
            tab.setTextColor(Color.rgb(220,220,220)); //Slightly less bright white.
        }
        Tabs[index].setTypeface(Tabs[index].getTypeface(), Typeface.BOLD);
        Tabs[index].setTextColor(Color.WHITE);

        //Animating the white bar on the left
        ValueAnimator animation = ValueAnimator.ofFloat(selected.getY(), Tabs[index].getY()+(Tabs[index].getHeight()-selected.getHeight())/2f);
        animation.setDuration(250);
        animation.addUpdateListener(animation1 -> selected.setY((float) animation1.getAnimatedValue()));
        animation.start();
    }

    protected void initTabs(int activeTab){
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                selectTabPage(activeTab);
            }
        }, 500);
    }

    private void restoreOldLook(boolean oldLookState){
        Guideline guideLine = findViewById(R.id.guidelineLeft);
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideLine.getLayoutParams();
        if(oldLookState){
            //UI v1 Style
            //Hide the sidebar
            params.guidePercent = 0; // 0%, range: 0 <-> 1
            guideLine.setLayoutParams(params);

            //Remove the selected Tab
            selected.setVisibility(View.GONE);

            //Enlarge the button, but just a bit.
            params = (ConstraintLayout.LayoutParams) mPlayButton.getLayoutParams();
            params.matchConstraintPercentWidth = 0.35f;
        }else{
            //UI v2 Style
            //Show the sidebar back
            params.guidePercent = 0.23f; // 23%, range: 0 <-> 1
            guideLine.setLayoutParams(params);

            //Show the selected Tab
            selected.setVisibility(View.VISIBLE);

            //Set the default button size
            params = (ConstraintLayout.LayoutParams) mPlayButton.getLayoutParams();
            params.matchConstraintPercentWidth = 0.25f;
        }
        mPlayButton.setLayoutParams(params);
    }
}

