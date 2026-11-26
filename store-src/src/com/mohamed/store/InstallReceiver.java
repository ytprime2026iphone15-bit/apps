package com.mohamed.store;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageInstaller;
public class InstallReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context c, Intent i) {
    int st = i.getIntExtra(PackageInstaller.EXTRA_STATUS, -999);
    String name = i.getStringExtra("name"); if (name == null) name = "التطبيق";
    if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) {
      Intent confirm = i.getParcelableExtra(Intent.EXTRA_INTENT);
      if (confirm == null) return;
      confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      if (Main.fg) { try { c.startActivity(confirm); return; } catch (Throwable e) {} }
      Core.notify(c, name.hashCode(), "اضغط لإكمال تثبيت " + name, "أندرويد يطلب تأكيدك.", PendingIntent.getActivity(c, name.hashCode(), confirm, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
    } else if (st == PackageInstaller.STATUS_SUCCESS) {
      if (!Main.fg) Core.notify(c, name.hashCode(), "تم تحديث " + name, "التطبيق جاهز.", null);
      Main.poke();
    } else {
      String m = i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
      Main.fail(name + ": " + (m == null ? "فشل التثبيت" : m));
      if (!Main.fg) Core.notify(c, name.hashCode(), "فشل تثبيت " + name, m == null ? "حاول مرة تانية من المتجر" : m, Core.openStore(c));
    }
  }
}
