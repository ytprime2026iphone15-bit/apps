package com.mohamed.store;
import android.app.*;
import android.app.job.*;
import android.content.*;
import android.content.pm.*;
import android.os.Build;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;

public class Core {
  // Catalog address. Replaced at build time (see build.sh).
  static final String CATALOG = "@@CATALOG_URL@@";
  // Signing certificates that are pinned inside the store: a changed catalog can never make the store install an app signed by a different key.
  static final String[][] PINNED = {
    {"com.garrebha.salah", "fa809742b02315076baa4f87d53e6eb3dfa1dbc57f7c3062e70830292a509e33"},
    {"com.garrebha.tracker", "30535dfca4244f9602c03ffa3744c615bf740c58141f98007eeb3abdd0a6c1eb"},
    {"com.mohamed.ricohprint", "c3726f9d28fa53289e97c772ae444f9493d711c9f64d872810c960dc1a11ca8f"},
    {"com.garrebha.music", "730490a691c1645f1d70aa32588533ace89c0d34b84278064cc18769c54011f6"},
    {"com.mohamed.store", "@@STORE_CERT@@"}
  };
  static final String CH = "store";

  static String http(String u) throws Exception {
    HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
    c.setConnectTimeout(15000); c.setReadTimeout(30000);
    c.setRequestProperty("Cache-Control", "no-cache");
    if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
    StringBuilder sb = new StringBuilder(); char[] b = new char[8192]; int n;
    while ((n = r.read(b)) > 0) sb.append(b, 0, n);
    return sb.toString();
  }
  static JSONArray apps() throws Exception {
    String sep = CATALOG.contains("?") ? "&" : "?";
    return new JSONObject(http(CATALOG + sep + "t=" + System.currentTimeMillis())).getJSONArray("apps");
  }
  static int installedVC(Context c, String pkg) {
    try { return (int) c.getPackageManager().getPackageInfo(pkg, 0).getLongVersionCode(); } catch (Throwable e) { return -1; }
  }
  static String installedVN(Context c, String pkg) {
    try { return c.getPackageManager().getPackageInfo(pkg, 0).versionName; } catch (Throwable e) { return ""; }
  }
  static String installer(Context c, String pkg) {
    try { if (Build.VERSION.SDK_INT >= 30) return c.getPackageManager().getInstallSourceInfo(pkg).getInstallingPackageName(); return c.getPackageManager().getInstallerPackageName(pkg); } catch (Throwable e) { return null; }
  }
  // true when Android 12+ may update this app without a confirmation screen: we are its installer.
  static boolean canSilent(Context c, String pkg) {
    return Build.VERSION.SDK_INT >= 31 && c.getPackageName().equals(installer(c, pkg));
  }
  static String hex(byte[] b) { StringBuilder s = new StringBuilder(); for (byte x : b) s.append(String.format("%02x", x)); return s.toString(); }
  static String pinned(String pkg) { for (String[] p : PINNED) if (p[0].equals(pkg)) return p[1]; return null; }

  interface Prog { void on(int pct); }

  static File download(Context c, JSONObject a, Prog cb) throws Exception {
    File dir = new File(c.getCacheDir(), "apks"); dir.mkdirs();
    File f = new File(dir, a.getString("id") + ".apk");
    HttpURLConnection h = (HttpURLConnection) new URL(a.getString("url")).openConnection();
    h.setConnectTimeout(15000); h.setReadTimeout(60000);
    if (h.getResponseCode() != 200) throw new IOException("HTTP " + h.getResponseCode());
    long total = h.getContentLengthLong(); if (total <= 0) total = a.optLong("size", 0);
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    InputStream in = h.getInputStream(); OutputStream o = new FileOutputStream(f);
    byte[] buf = new byte[32768]; int n; long got = 0; int last = -1;
    while ((n = in.read(buf)) > 0) { o.write(buf, 0, n); md.update(buf, 0, n); got += n; if (total > 0 && cb != null) { int p = (int) (got * 100 / total); if (p != last) { last = p; cb.on(p); } } }
    o.close(); in.close();
    String sha = hex(md.digest());
    if (!sha.equalsIgnoreCase(a.getString("sha256"))) { f.delete(); throw new IOException("الملف غير مطابق للبصمة المسجلة (قد يكون التحميل ناقص أو الكتالوج لم يتحدث بعد). حاول بعد دقائق");}
    String err = verify(c, f, a);
    if (err != null) { f.delete(); throw new IOException(err); }
    return f;
  }
  static String verify(Context c, File f, JSONObject a) {
    try {
      PackageInfo pi = c.getPackageManager().getPackageArchiveInfo(f.getPath(), PackageManager.GET_SIGNING_CERTIFICATES);
      if (pi == null) return "ملف التطبيق تالف";
      if (!pi.packageName.equals(a.getString("id"))) return "اسم الحزمة غير مطابق";
      if ((int) pi.getLongVersionCode() != a.getInt("versionCode")) return "رقم الإصدار غير مطابق";
      Signature[] sg = pi.signingInfo.getApkContentsSigners();
      String want = pinned(a.getString("id")); if (want == null) want = a.optString("cert", "");
      if (want.length() < 32) return "لا توجد بصمة توقيع لهذا التطبيق";
      boolean ok = false;
      for (Signature s : sg) if (hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray())).equalsIgnoreCase(want)) ok = true;
      return ok ? null : "توقيع التطبيق غير موثوق، تم إيقاف التثبيت";
    } catch (Throwable e) { return "تعذر فحص الملف: " + e.getMessage(); }
  }
  static void install(Context c, File f, String pkg, String name) throws Exception {
    PackageInstaller pi = c.getPackageManager().getPackageInstaller();
    PackageInstaller.SessionParams p = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
    p.setAppPackageName(pkg);
    if (Build.VERSION.SDK_INT >= 31) p.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
    int id = pi.createSession(p);
    PackageInstaller.Session s = pi.openSession(id);
    try {
      OutputStream o = s.openWrite("app", 0, f.length()); InputStream in = new FileInputStream(f);
      byte[] b = new byte[65536]; int n; while ((n = in.read(b)) > 0) o.write(b, 0, n);
      s.fsync(o); in.close(); o.close();
      Intent i = new Intent(c, InstallReceiver.class).setAction("com.mohamed.store.INSTALL").putExtra("pkg", pkg).putExtra("name", name);
      PendingIntent pe = PendingIntent.getBroadcast(c, id, i, PendingIntent.FLAG_UPDATE_CURRENT | 0x02000000);
      s.commit(pe.getIntentSender());
    } catch (Throwable e) { s.abandon(); throw e instanceof Exception ? (Exception) e : new Exception(e); }
  }
  static void channel(Context c) {
    NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    nm.createNotificationChannel(new NotificationChannel(CH, "تحديثات التطبيقات", NotificationManager.IMPORTANCE_DEFAULT));
  }
  static void notify(Context c, int id, String title, String text, PendingIntent pe) {
    try {
      channel(c);
      Notification.Builder b = new Notification.Builder(c, CH).setSmallIcon(c.getApplicationInfo().icon).setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setAutoCancel(true);
      if (pe != null) b.setContentIntent(pe);
      ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, b.build());
    } catch (Throwable e) {}
  }
  static PendingIntent openStore(Context c) {
    return PendingIntent.getActivity(c, 0, new Intent(c, Main.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
  }
  static void schedule(Context c) {
    try {
      JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
      js.schedule(new JobInfo.Builder(7, new ComponentName(c, UpdateJob.class)).setPeriodic(6L * 3600 * 1000).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).build());
    } catch (Throwable e) {}
  }
  // Background check: update silently where Android allows it, otherwise notify once per version.
  static void backgroundCheck(Context c) {
    try {
      if (!c.getPackageManager().canRequestPackageInstalls()) return;
      JSONArray arr = apps();
      SharedPreferences sp = c.getSharedPreferences("s", 0);
      JSONObject self = null;
      for (int i = 0; i < arr.length(); i++) {
        JSONObject a = arr.getJSONObject(i); String pkg = a.getString("id");
        int have = installedVC(c, pkg); if (have < 0 || a.getInt("versionCode") <= have) continue;
        if (pkg.equals(c.getPackageName())) { self = a; continue; }
        String key = pkg + ":" + a.getInt("versionCode");
        if (canSilent(c, pkg)) {
          if (System.currentTimeMillis() - sp.getLong("t_" + key, 0) < 5L * 3600 * 1000) continue;
          sp.edit().putLong("t_" + key, System.currentTimeMillis()).apply();
          try { install(c, download(c, a, null), pkg, a.optString("name", pkg)); } catch (Throwable e) {}
        } else if (!sp.getBoolean("n_" + key, false)) {
          sp.edit().putBoolean("n_" + key, true).apply();
          notify(c, key.hashCode(), "تحديث متاح: " + a.optString("name", pkg), "الإصدار " + a.optString("versionName") + ". افتح المتجر واضغط تحديث.", openStore(c));
        }
      }
      if (self != null && canSilent(c, c.getPackageName()) && System.currentTimeMillis() - sp.getLong("t_self", 0) > 5L * 3600 * 1000) { sp.edit().putLong("t_self", System.currentTimeMillis()).apply(); try { install(c, download(c, self, null), c.getPackageName(), "متجر التطبيقات"); self = null; } catch (Throwable e) {} }
      if (self != null) { String key = "self:" + self.getInt("versionCode"); if (!sp.getBoolean("n_" + key, false)) { sp.edit().putBoolean("n_" + key, true).apply(); notify(c, 77, "تحديث للمتجر", "افتح المتجر لتحديثه.", openStore(c)); } }
    } catch (Throwable e) {}
  }
}
