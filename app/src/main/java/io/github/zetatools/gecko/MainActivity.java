package io.github.zetatools.gecko;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.mozilla.geckoview.*;
import java.util.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.*;

public class MainActivity extends Activity {
    private static final String HOME="https://zeta-ai.io/ko";
    private static GeckoRuntime runtime;
    private boolean checkingUpdate=false;
    private static final String RELEASE_API="https://api.github.com/repos/e4493089-cmyk/zeta-android-app/releases/latest";
    private static final String RELEASE_BASE="https://github.com/e4493089-cmyk/zeta-android-app/releases/download/";
    private GeckoView view;
    private GeckoSession main,current;
    private final ArrayList<GeckoSession> tabs=new ArrayList<>();
    private final IdentityHashMap<GeckoSession,String> locations=new IdentityHashMap<>();
    private final IdentityHashMap<GeckoSession,Boolean> backs=new IdentityHashMap<>();
    private final IdentityHashMap<GeckoSession,GeckoSession> parents=new IdentityHashMap<>();
    private TextView origin,menu,close;
    private LinearLayout bar;
    private FrameLayout frame;
    private SharedPreferences prefs;
    private int mode;
    private WebExtension theme;
    private boolean themeBusy=false,themeEnabled=false;
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("settings",MODE_PRIVATE);
        mode=prefs.getInt("screen",0);
        if(runtime==null)runtime=GeckoRuntime.create(getApplicationContext(),new GeckoRuntimeSettings.Builder().remoteDebuggingEnabled(false).build());
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);
        bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setBackgroundColor(0xff252525);
        close=new TextView(this);close.setText("‹");close.setTextColor(Color.WHITE);close.setTextSize(28);close.setGravity(Gravity.CENTER);close.setContentDescription("로그인 창 닫기");bar.addView(close,new LinearLayout.LayoutParams(dp(42),dp(46)));close.setOnClickListener(v->back());
        origin=new TextView(this);origin.setTextColor(Color.WHITE);origin.setTextSize(14);origin.setPadding(dp(5),0,dp(5),0);bar.addView(origin,new LinearLayout.LayoutParams(0,dp(46),1));origin.setGravity(Gravity.CENTER_VERTICAL);
        layout.addView(bar,new LinearLayout.LayoutParams(-1,dp(46)));
        frame=new FrameLayout(this);view=new GeckoView(this);frame.addView(view,new FrameLayout.LayoutParams(-1,-1));
        menu=new TextView(this);menu.setText("⋮");menu.setTextColor(Color.WHITE);menu.setTextSize(25);menu.setGravity(Gravity.CENTER);menu.setBackgroundColor(0xb330302f);menu.setContentDescription("앱 설정");
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(36),dp(44),Gravity.TOP|Gravity.END);p.topMargin=dp(8);p.rightMargin=dp(4);frame.addView(menu,p);menu.setOnClickListener(v->settings());
        layout.addView(frame,new LinearLayout.LayoutParams(-1,0,1));setContentView(layout);
        getWindow().setStatusBarColor(0xff252525);getWindow().setNavigationBarColor(0xff252525);
        main=createSession();main.open(runtime);show(main);locations.put(main,HOME);main.loadUri(HOME);updateChrome();
        installTheme(true);
        installMenu();
        if(prefs.getBoolean("autoUpdate",true)){checkUpdate(false);}
    }
    private GeckoSession createSession(){
        GeckoSession s=new GeckoSession();tabs.add(s);
        s.setNavigationDelegate(new GeckoSession.NavigationDelegate(){
            @Override public GeckoResult<AllowOrDeny> onLoadRequest(GeckoSession session,LoadRequest request){
                Uri u=Uri.parse(request.uri);
                String scheme=u.getScheme();
                if (session==main && "https://zeta-ai.io/__zeta_tools_app_settings__".equals(request.uri)) {
                    runOnUiThread(()->{if(!isDestroyed()&&current==main&&ownPage())settings();});
                    return GeckoResult.fromValue(AllowOrDeny.DENY);
                }
                if("https".equals(scheme)||"about".equals(scheme)||"blob".equals(scheme)||"data".equals(scheme))return GeckoResult.fromValue(AllowOrDeny.ALLOW);
                return GeckoResult.fromValue(AllowOrDeny.DENY);
            }
            @Override public GeckoResult<GeckoSession> onNewSession(GeckoSession parent,String uri){
                if(tabs.size()>=8)return null;
                GeckoSession child=createSession();parents.put(child,parent);locations.put(child,uri);
                // Gecko opens the child itself and preserves the opener relationship.
                new Handler(Looper.getMainLooper()).post(()->{if(!isDestroyed()&&tabs.contains(child))show(child);});
                return GeckoResult.fromValue(child);
            }
            @Override public void onCanGoBack(GeckoSession s,boolean can){backs.put(s,can);}
            @Override public void onLocationChange(GeckoSession s,String url,List<GeckoSession.PermissionDelegate.ContentPermission> permissions,Boolean hasUserGesture){
                locations.put(s,url);if(s==current)updateChrome();
            }
        });
        s.setContentDelegate(new GeckoSession.ContentDelegate(){
            @Override public void onCloseRequest(GeckoSession s){if(s!=main)closeTab(s);}
        });
        s.setPromptDelegate(new GeckoSession.PromptDelegate(){
            @Override public GeckoResult<PromptResponse> onPopupPrompt(GeckoSession s,PopupPrompt prompt){
                GeckoResult<PromptResponse> result=new GeckoResult<>();
                new AlertDialog.Builder(MainActivity.this).setTitle("새 창 열기").setMessage("웹사이트가 새 창을 열려고 합니다.").setPositiveButton("허용",(d,w)->result.complete(prompt.confirm(AllowOrDeny.ALLOW))).setNegativeButton("차단",(d,w)->result.complete(prompt.confirm(AllowOrDeny.DENY))).setOnCancelListener(d->result.complete(prompt.confirm(AllowOrDeny.DENY))).show();
                return result;
            }
        });
        return s;
    }
    private void show(GeckoSession s){
        if(current!=s){if(view.getSession()!=null)view.releaseSession();current=s;view.setSession(s);}
        updateChrome();
    }
    private void closeTab(GeckoSession s){
        GeckoSession parent=parents.get(s);if(parent==null||!tabs.contains(parent))parent=main;
        if(current==s)show(parent);
        tabs.remove(s);locations.remove(s);backs.remove(s);parents.remove(s);
        if(s.isOpen())s.close();
        updateChrome();
    }
    private boolean ownPage(){
        try{Uri u=Uri.parse(locations.getOrDefault(current,""));return "https".equals(u.getScheme())&&"zeta-ai.io".equals(u.getHost());}catch(Exception e){return false;}
    }
    private void updateChrome(){
        if(current==null)return;
        boolean showBar=current!=main||!ownPage();
        bar.setVisibility(showBar?View.VISIBLE:View.GONE);
        menu.setVisibility(View.GONE);
        Uri u=Uri.parse(locations.getOrDefault(current,"about:blank"));
        // Show the actual origin, never the OAuth query or fragment.
        origin.setText(u.getHost()!=null?u.getScheme()+"://"+u.getHost()+(u.getPort()!=-1?":"+u.getPort():""):"새 창");
        applyScreen();
        showPendingUpdate();
    }
    private void installMenu(){
        runtime.getWebExtensionController().ensureBuiltIn("resource://android/assets/appmenu/","appmenu@zetatools.local").accept(ext->{},err->runOnUiThread(()->{if(!isDestroyed())toast("프로필 설정 메뉴를 불러오지 못했습니다. 뒤로 가기 종료 창에서 설정을 열 수 있습니다.");}));
    }
    private void installTheme(boolean reload){
        if(themeBusy)return;themeBusy=true;
        runtime.getWebExtensionController().ensureBuiltIn("resource://android/assets/theme/","theme@zetatools.local").accept(ext->{
            runOnUiThread(()->{
                if(isDestroyed())return;
                theme=ext;
                boolean on=prefs.getBoolean("theme",true);
                setTheme(on,reload);
            });
        },err->runOnUiThread(()->{themeBusy=false;if(!isDestroyed())toast("테마 설치 실패 · 로그인 테스트는 가능");}));
    }
    private void setTheme(boolean on,boolean reload){
        if(theme==null){themeBusy=false;return;}
        WebExtensionController c=runtime.getWebExtensionController();
        GeckoResult<WebExtension> action=on?c.enable(theme,WebExtensionController.EnableSource.USER):c.disable(theme,WebExtensionController.EnableSource.USER);
        action.accept(ext->runOnUiThread(()->{if(isDestroyed())return;theme=ext;themeBusy=false;themeEnabled=on;prefs.edit().putBoolean("theme",on).apply();if(reload && current==main && ownPage()){main.reload();}}),err->runOnUiThread(()->{themeBusy=false;if(!isDestroyed())toast("테마 설정 실패");}));
    }
    private void applyScreen(){
        int flags=0;
        // Keep the native address/origin and system bars visible on authentication pages.
        if(current==main&&ownPage()){
            if(mode==1)flags=View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            if(mode==2)flags=View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
        if(prefs.getBoolean("awake",false))getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private long installedCode(){
        try{android.content.pm.PackageInfo p=getPackageManager().getPackageInfo(getPackageName(),0);return Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;}
        catch(Exception e){return Long.MAX_VALUE;}
    }
    private String installedName(){
        try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception e){return "0.2.2";}
    }
    private void checkUpdate(boolean manual){
        if(checkingUpdate){if(manual)toast("업데이트 확인 중입니다.");return;}
        checkingUpdate=true;
        if(manual)toast("최신 버전 확인 중");
        new Thread(()->{
            HttpURLConnection c=null;
            String tag=null,version=null,error=null;
            try{
                c=(HttpURLConnection)new URL(RELEASE_API).openConnection();
                c.setConnectTimeout(12000);c.setReadTimeout(12000);c.setInstanceFollowRedirects(false);
                c.setRequestProperty("Accept","application/vnd.github+json");
                c.setRequestProperty("User-Agent","ZETA-Tools-Android");
                int status=c.getResponseCode();
                if(status!=404){
                    if(status!=200)throw new java.io.IOException("HTTP "+status);
                    String body;
                    try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                        byte[] b=new byte[8192];int n,total=0;
                        while((n=in.read(b))!=-1){total+=n;if(total>1024*1024)throw new java.io.IOException("response too large");out.write(b,0,n);}
                        body=new String(out.toByteArray(),StandardCharsets.UTF_8);
                    }
                    JSONObject release=new JSONObject(body);
                    String candidate=release.optString("tag_name");
                    if(!release.optBoolean("draft")&&!release.optBoolean("prerelease")&&candidate.matches("android-[0-9]{1,9}")&&Long.parseLong(candidate.substring(8))>installedCode()){
                        JSONArray assets=release.optJSONArray("assets");
                        if(assets!=null)for(int i=0;i<assets.length();i++){
                            JSONObject item=assets.getJSONObject(i);
                            if("zeta-tools.apk".equals(item.optString("name"))&&"uploaded".equals(item.optString("state"))&&item.optLong("size")>0){
                                tag=candidate;version=release.optString("name",candidate);break;
                            }
                        }
                    }
                }
            }catch(Exception e){error="업데이트 확인 실패 · 기존 앱을 계속 사용할 수 있습니다.";}
            finally{if(c!=null)c.disconnect();}
            final String found=tag,label=version,problem=error;
            runOnUiThread(()->{
                checkingUpdate=false;
                if(isDestroyed()||isFinishing())return;
                if(problem!=null){if(manual)toast(problem);return;}
                if(found==null){if(manual)toast("설치 가능한 새 정식 버전이 없습니다.");return;}
                if(current!=main||!ownPage()||!hasWindowFocus()){
                    prefs.edit().putString("pendingUpdate",found).putString("pendingUpdateLabel",label).apply();
                    return;
                }
                if(!manual&&!prefs.getBoolean("autoUpdate",true))return;
                showUpdate(found,label);
            });
        },"zeta-update-check").start();
    }
    private void showUpdate(String tag,String label){
        if(!tag.matches("android-[0-9]{1,9}")||Long.parseLong(tag.substring(8))<=installedCode())return;
        prefs.edit().remove("pendingUpdate").remove("pendingUpdateLabel").apply();
        new AlertDialog.Builder(this).setTitle("새 업데이트가 있습니다")
            .setMessage("현재 버전: "+installedName()+"\n새 버전: "+label+"\n\n업데이트 파일을 다운로드한 뒤 안드로이드 설치 화면에서 승인하세요.")
            .setPositiveButton("업데이트",(d,w)->{
                try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(RELEASE_BASE+tag+"/zeta-tools.apk")));}
                catch(ActivityNotFoundException e){toast("다운로드할 브라우저를 찾을 수 없습니다.");}
            }).setNegativeButton("나중에",null).show();
    }
    private void showPendingUpdate(){
        if(prefs==null||!prefs.getBoolean("autoUpdate",true)||current!=main||!ownPage()||!hasWindowFocus())return;
        String tag=prefs.getString("pendingUpdate","");
        if(!tag.isEmpty())showUpdate(tag,prefs.getString("pendingUpdateLabel",tag));
    }
    private void settings(){
        String[] options={"화면 모드","라이트 모드: "+(themeEnabled?"켜짐":"꺼짐"),"새로고침","제타 홈","화면 꺼짐 방지: "+(prefs.getBoolean("awake",false)?"켜짐":"꺼짐"),"앱 정보","업데이트 확인","자동 업데이트 확인: "+(prefs.getBoolean("autoUpdate",true)?"켜짐":"꺼짐")};
        new AlertDialog.Builder(this).setTitle("ZETA Tools Gecko").setItems(options,(d,w)->{
            switch(w){
                case 0:new AlertDialog.Builder(this).setTitle("화면 모드").setSingleChoiceItems(new String[]{"기본","확장","전체화면"},mode,(a,b)->{mode=b;prefs.edit().putInt("screen",b).apply();a.dismiss();applyScreen();}).setNegativeButton("닫기",null).show();break;
                case 1:if(themeBusy||theme==null)toast("테마 준비 중");else{themeBusy=true;setTheme(!themeEnabled,true);}break;
                case 2:current.reload();break;
                case 3:main.loadUri(HOME);show(main);break;
                case 4:prefs.edit().putBoolean("awake",!prefs.getBoolean("awake",false)).apply();applyScreen();break;
                case 6:checkUpdate(true);break;
                case 7:boolean auto=!prefs.getBoolean("autoUpdate",true);prefs.edit().putBoolean("autoUpdate",auto).apply();if(auto)checkUpdate(false);else{prefs.edit().remove("pendingUpdate").remove("pendingUpdateLabel").apply();toast("자동 업데이트 확인 꺼짐");}break;
                case 5:new AlertDialog.Builder(this).setTitle("ZETA Tools "+installedName()).setMessage("파이어폭스 엔진 기반 로그인 테스트 앱. 제타 공식 앱이 아닙니다. 계정 인증은 실제 웹사이트에서 진행하며, 쿠키를 외부로 복사하지 않습니다. 라이트 모드는 새 설치에서 기본 적용됩니다.").setPositiveButton("확인",null).show();break;
            }
        }).setOnDismissListener(d->applyScreen()).show();
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void back(){if(current!=main){closeTab(current);return;}if(Boolean.TRUE.equals(backs.get(current)))current.goBack();else new AlertDialog.Builder(this).setMessage("앱을 종료할까요?").setPositiveButton("종료",(d,w)->finish()).setNegativeButton("취소",null).setNeutralButton("앱 설정",(d,w)->settings()).show();}
    @Override public void onBackPressed(){back();}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus&&prefs!=null){applyScreen();showPendingUpdate();}}
    @Override protected void onDestroy(){if(view!=null&&view.getSession()!=null)view.releaseSession();for(GeckoSession s:new ArrayList<>(tabs))if(s.isOpen())s.close();tabs.clear();super.onDestroy();}
}
