package com.mohamed.store;
import android.app.job.*;
public class UpdateJob extends JobService {
  @Override public boolean onStartJob(final JobParameters p) {
    new Thread(new Runnable() { public void run() { Core.backgroundCheck(UpdateJob.this); jobFinished(p, false); } }).start();
    return true;
  }
  @Override public boolean onStopJob(JobParameters p) { return true; }
}
