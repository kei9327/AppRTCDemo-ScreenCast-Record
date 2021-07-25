package org.appspot.apprtc.util;


import jp.co.cyberagent.android.gpuimage.BuildConfig;
import timber.log.Timber;

public final class AppLogger {

    private AppLogger() {
        // This utility class is not publicly instantiable
    }

    public static void init() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    public static void v(String s, Object... objects) {
        Timber.v(s, objects);
    }

    public static void v(Throwable throwable, String s, Object... objects) {
        Timber.v(throwable, s, objects);
    }

    public static void d(String s, Object... objects) {
        Timber.d(s, objects);
    }

    public static void d(Throwable throwable, String s, Object... objects) {
        Timber.d(throwable, s, objects);
    }

    public static void i(String s, Object... objects) {
        Timber.i(s, objects);
    }

    public static void i(Throwable throwable, String s, Object... objects) {
        Timber.i(throwable, s, objects);
    }

    public static void w(String s, Object... objects) {
        Timber.w(s, objects);
    }

    public static void w(Throwable throwable, String s, Object... objects) {
        Timber.w(throwable, s, objects);
    }

    public static void e(String s, Object... objects) {
        Timber.e(s, objects);
    }

    public static void e(Throwable throwable, String s, Object... objects) {
        Timber.e(throwable, s, objects);
    }

}
