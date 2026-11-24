package io.github.zetatools.gecko;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.json.JSONObject;

/** Downloads CSS only from the independent theme. Never executes downloaded JavaScript. */
final class ThemeStore {
    private static final int LIMIT = 512 * 1024;
    private static final String SOURCE = "https://raw.githubusercontent.com/e4493089-cmyk/zeta-android-app/main/scripts/zeta-app-light.user.js";
    private static final String START = "const CSS = String.raw" + (char)96;
    private static final String END = "\n" + (char)96 + ";";
    private static final Pattern VERSION = Pattern.compile("(?m)^//\\s*@version\\s+([0-9]+(?:\\.[0-9]+){1,3})\\s*$");
    private static final Pattern EXTERNAL = Pattern.compile("(?i)@\\s*import\\b|@\\s*font-face\\b|\\b(?:url|image-set|src)\\s*\\(");
    static final class Data {
        final String version, css;
        Data(String version,String css){this.version=version;this.css=css;}
        boolean same(Data other){return other!=null && version.equals(other.version) && css.equals(other.css);}
        JSONObject json() {
            JSONObject obj=new JSONObject();
            try{obj.put("version",version);obj.put("css",css);}catch(Exception ignored){}
            return obj;
        }
    }
    static String read(InputStream in) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] buf=new byte[8192]; int n,total=0;
        while((n=in.read(buf))!=-1){total+=n;if(total>LIMIT)throw new IOException("theme too large");out.write(buf,0,n);}
        return new String(out.toByteArray(),StandardCharsets.UTF_8);
    }
    static Data checked(String version,String css) throws IOException {
        if(version==null || !version.matches("[0-9]+(?:\\.[0-9]+){1,3}") || css==null ||
            css.length()>LIMIT || css.trim().isEmpty() || !css.contains("--zl-bg:") ||
            css.indexOf(0)>=0 || css.contains("$"+"{") || EXTERNAL.matcher(css).find())
            throw new IOException("invalid theme data");
        return new Data(version,css);
    }
    static Data parse(String source) throws IOException {
        Matcher m=VERSION.matcher(source);
        int from=source.indexOf(START);
        int to=from<0?-1:source.indexOf(END,from+START.length());
        if(!source.startsWith("// ==UserScript==") || !m.find() || from<0 || to<0)
            throw new IOException("unknown theme format");
        return checked(m.group(1),source.substring(from+START.length(),to));
    }
    static Data initial(Context context,SharedPreferences prefs) {
        try{return checked(prefs.getString("remoteThemeVersion",""),prefs.getString("remoteThemeCss",""));}
        catch(Exception ignored){}
        try(InputStream in=context.getAssets().open("theme/theme-default.json")){
            JSONObject obj=new JSONObject(read(in));
            return checked(obj.getString("version"),obj.getString("css"));
        }catch(Exception ignored){return null;}
    }
    static Data fetch() throws IOException {
        HttpURLConnection c=(HttpURLConnection)new URL(SOURCE+"?t="+System.currentTimeMillis()).openConnection();
        try{
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(10000);c.setReadTimeout(10000);
            c.setUseCaches(false);
            c.setRequestProperty("Accept","text/plain");
            c.setRequestProperty("Cache-Control","no-cache");
            c.setRequestProperty("User-Agent","ZETA-Tools-Android");
            if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());
            try(InputStream in=c.getInputStream()){return parse(read(in));}
        }finally{c.disconnect();}
    }
}
