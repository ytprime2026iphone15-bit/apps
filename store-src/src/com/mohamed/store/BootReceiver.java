package com.mohamed.store;
import android.content.*;
public class BootReceiver extends BroadcastReceiver { @Override public void onReceive(Context c, Intent i) { Core.schedule(c); } }
