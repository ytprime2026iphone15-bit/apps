package com.mohamed.store;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.File;
import java.util.*;

public class Main extends Activity {
  static boolean fg = false; static Main cur;
  static void poke() { final Main m = cur; if (m != null) m.runOnUiThread(new Runnable() { public void run() { m.load(false); } }); }
  static void fail(final String s) { final Main m = cur; if (m != null) m.runOnUiThread(new Runnable() { public void run() { m.banner(s, 0xFFE57373); m.load(false); } }); }
  LinearLayout list; TextView bn; JSONArray apps; Handler h = new Handler(Looper.getMainLooper());
  Map<String, String> busy = new HashMap<String, String>();
  int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
  GradientDrawable bg(int col, int r) { GradientDrawable g = new GradientDrawable(); g.setColor(col); g.setCornerRadius(dp(r)); return g; }
  TextView tv(String t, int sz, int col) { TextView v = new TextView(this); v.setText(t); v.setTextSize(sz); v.setTextColor(col); v.setTextDirection(View.TEXT_DIRECTION_RTL); return v; }
  Button btn(String t, int col, int txt) { Button b = new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(txt); b.setBackground(bg(col, 10)); b.setTextSize(15); return b; }

  @Override protected void onCreate(Bundle b) {
    super.onCreate(b); cur = this;
    getWindow().setStatusBarColor(0xFF0F0F17); getWindow().setNavigationBarColor(0xFF0F0F17);
    LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(0xFF0F0F17); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    TextView t = tv("🛍 متجر التطبيقات", 24, 0xFFD4A63A); t.setPadding(dp(18), dp(22), dp(18), dp(6)); t.setGravity(Gravity.CENTER); root.addView(t);
    TextView s = tv("تطبيقات سامر، بتتحدث لوحدها", 14, 0xFF999999); s.setGravity(Gravity.CENTER); root.addView(s);
    LinearLayout bar = new LinearLayout(this); bar.setPadding(dp(12), dp(10), dp(12), dp(4));
    Button r = btn("🔄 تحديث القائمة", 0xFF262636, 0xFFEEEEEE); r.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { load(true); } });
    Button all = btn("⬆️ تحديث الكل", 0xFFB88A28, 0xFF111111); all.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { updateAll(); } });
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1); lp.setMargins(dp(4), 0, dp(4), 0);
    bar.addView(r, lp); bar.addView(all, lp); root.addView(bar);
    bn = tv("", 15, 0xFF111111); bn.setPadding(dp(14), dp(10), dp(14), dp(10)); bn.setGravity(Gravity.CENTER); bn.setVisibility(View.GONE);
    bn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { bn.setVisibility(View.GONE); } }); root.addView(bn);
    ScrollView sv = new ScrollView(this); list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(10), dp(6), dp(10), dp(30)); sv.addView(list); root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
    setContentView(root);
    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
    Core.schedule(this);
  }
  void banner(String m, int col) { bn.setText(m); bn.setBackground(bg(col, 0)); bn.setVisibility(View.VISIBLE); }
  @Override protected void onResume() { super.onResume(); fg = true; cur = this; load(true); }
  @Override protected void onPause() { super.onPause(); fg = false; }
  boolean canInstall() { return getPackageManager().canRequestPackageInstalls(); }

  void load(final boolean net) {
    if (!net && apps != null) { render(); return; }
    if (apps == null) { list.removeAllViews(); list.addView(tv("جاري تحميل القائمة...", 17, 0xFFCCCCCC)); }
    new Thread(new Runnable() { public void run() {
      try { final JSONArray a = Core.apps(); runOnUiThread(new Runnable() { public void run() { apps = a; render(); } }); }
      catch (final Throwable e) { runOnUiThread(new Runnable() { public void run() { if (apps == null) { list.removeAllViews(); list.addView(tv("تعذر تحميل القائمة. تأكد من الإنترنت ثم اضغط تحديث القائمة.\n(" + e.getMessage() + ")", 16, 0xFFE57373)); } else banner("تعذر تحديث القائمة: " + e.getMessage(), 0xFFE57373); } }); }
    } }).start();
  }
  boolean selfTried = false;
  void selfUpdate() {
    try {
      if (selfTried || apps == null || !canInstall()) return;
      for (int i = 0; i < apps.length(); i++) {
        final JSONObject a = apps.getJSONObject(i);
        if (!a.getString("id").equals(getPackageName()) || a.getInt("versionCode") <= Core.installedVC(this, getPackageName())) continue;
        selfTried = true; banner("فيه تحديث للمتجر نفسه، جاري تحميله. اضغط تأكيد لما أندرويد يسأل", 0xFFFFD77A);
        new Thread(new Runnable() { public void run() {
          try { Core.install(Main.this, Core.download(Main.this, a, null), getPackageName(), "متجر التطبيقات"); }
          catch (final Throwable e) { runOnUiThread(new Runnable() { public void run() { banner("فشل تحديث المتجر: " + e.getMessage(), 0xFFE57373); } }); }
        } }).start();
      }
    } catch (Throwable e) {}
  }
  void render() {
    selfUpdate();
    list.removeAllViews();
    if (!canInstall()) {
      Button b = btn("⚠ اضغط هنا للسماح للمتجر بتثبيت التطبيقات", 0xFFE57373, 0xFF111111);
      b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()))); } });
      list.addView(b);
      TextView t = tv("أندرويد لازم يسمح لمرة واحدة. فعّل \"السماح من هذا المصدر\" ثم ارجع للمتجر.", 13, 0xFF999999); t.setPadding(0, dp(6), 0, dp(10)); list.addView(t);
    }
    try {
      for (int i = 0; i < apps.length(); i++) list.addView(card(apps.getJSONObject(i)));
    } catch (Throwable e) { list.addView(tv("خطأ في القائمة: " + e.getMessage(), 15, 0xFFE57373)); }
  }
  View card(final JSONObject a) throws Exception {
    final String pkg = a.getString("id"), name = a.optString("name", pkg);
    int have = Core.installedVC(this, pkg), vc = a.getInt("versionCode");
    LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(16), dp(14), dp(16), dp(14)); c.setBackground(bg(0xFF1D1D2B, 14));
    LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(-1, -2); cl.setMargins(dp(4), dp(6), dp(4), dp(6)); c.setLayoutParams(cl);
    c.addView(tv(name, 20, 0xFFFFFFFF));
    String d = a.optString("desc", ""); if (d.length() > 0) { TextView x = tv(d, 14, 0xFFBBBBBB); x.setPadding(0, dp(4), 0, 0); c.addView(x); }
    String st; int col = 0xFF999999;
    if (have < 0) st = "غير مثبت • الإصدار " + a.optString("versionName") + " (" + sz(a.optLong("size")) + ")";
    else if (have < vc) { st = "مثبت " + Core.installedVN(this, pkg) + " • يتوفر تحديث " + a.optString("versionName"); col = 0xFFFFD77A; }
    else { st = "مثبت وآخر إصدار ✔ " + Core.installedVN(this, pkg); col = 0xFF7FD48A; }
    TextView s = tv(st, 13, col); s.setPadding(0, dp(6), 0, dp(2)); c.addView(s);
    String n = a.optString("notes", ""); if (n.length() > 0 && have < vc) { TextView x = tv("الجديد: " + n, 13, 0xFFCCCCCC); c.addView(x); }
    if (have >= 0 && have < vc) { TextView x = tv(Core.canSilent(this, pkg) ? "التحديث بيتم تلقائيًا (أندرويد 12+)" : "التحديث بيطلب تأكيد منك من أندرويد", 12, 0xFF888888); c.addView(x); }
    final ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); pb.setMax(100); pb.setVisibility(View.GONE); c.addView(pb);
    final TextView msg = tv("", 13, 0xFFFFD77A); msg.setVisibility(View.GONE); c.addView(msg);
    final Button b;
    if (busy.containsKey(pkg)) { b = btn(busy.get(pkg), 0xFF444444, 0xFFEEEEEE); b.setEnabled(false); }
    else if (have < 0) b = btn("⬇ تثبيت", 0xFFB88A28, 0xFF111111);
    else if (have < vc) b = btn("⬆ تحديث", 0xFFB88A28, 0xFF111111);
    else if (getPackageManager().getLaunchIntentForPackage(pkg) != null) b = btn("فتح", 0xFF262636, 0xFFEEEEEE);
    else b = btn("مثبت", 0xFF262636, 0xFFEEEEEE);
    LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, -2); bl.topMargin = dp(8); c.addView(b, bl);
    final boolean needs = have < vc;
    b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
      if (!needs) { Intent li = getPackageManager().getLaunchIntentForPackage(pkg); if (li != null) startActivity(li); return; }
      go(a, b, pb, msg);
    } });
    return c;
  }
  String sz(long b) { return b > 1048576 ? String.format("%.1f MB", b / 1048576.0) : (b / 1024) + " KB"; }
  void go(final JSONObject a, final Button b, final ProgressBar pb, final TextView msg) {
    final String pkg = a.optString("id");
    if (!canInstall()) { banner("اسمح للمتجر بالتثبيت أولاً (الزر الأحمر فوق)", 0xFFFFD77A); render(); return; }
    busy.put(pkg, "جاري التحميل..."); b.setEnabled(false); b.setText("جاري التحميل..."); pb.setVisibility(View.VISIBLE);
    new Thread(new Runnable() { public void run() {
      try {
        File f = Core.download(Main.this, a, new Core.Prog() { public void on(final int p) { runOnUiThread(new Runnable() { public void run() { pb.setProgress(p); } }); } });
        runOnUiThread(new Runnable() { public void run() { b.setText("جاري التثبيت..."); pb.setVisibility(View.GONE); } });
        Core.install(Main.this, f, pkg, a.optString("name", pkg));
      } catch (final Throwable e) {
        runOnUiThread(new Runnable() { public void run() { busy.remove(pkg); pb.setVisibility(View.GONE); banner("فشل: " + e.getMessage(), 0xFFE57373); render(); } });
        return;
      }
      h.postDelayed(new Runnable() { public void run() { busy.remove(pkg); render(); } }, 25000);
    } }).start();
  }
  void updateAll() {
    if (apps == null) return;
    if (!canInstall()) { banner("اسمح للمتجر بالتثبيت أولاً", 0xFFFFD77A); return; }
    banner("بدأ تحديث كل التطبيقات اللي لها تحديث...", 0xFFFFD77A);
    new Thread(new Runnable() { public void run() {
      try {
        for (int i = 0; i < apps.length(); i++) {
          JSONObject a = apps.getJSONObject(i); String pkg = a.getString("id");
          if (pkg.equals(getPackageName())) continue;
          int have = Core.installedVC(Main.this, pkg); if (have < 0 || a.getInt("versionCode") <= have) continue;
          try { Core.install(Main.this, Core.download(Main.this, a, null), pkg, a.optString("name", pkg)); Thread.sleep(2500); } catch (Throwable e) { fail(a.optString("name", pkg) + ": " + e.getMessage()); }
        }
      } catch (Throwable e) {}
    } }).start();
  }
}
