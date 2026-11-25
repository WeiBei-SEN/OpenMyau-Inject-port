package myau.inject;

import myau.Myau;

public final class Bootstrap {
    private static volatile boolean requested, started, attempted;
    private Bootstrap() {}
    public static void requestStart() { requested = true; }
    public static void tick() {
        if (!requested || attempted) return;
        attempted = true;
        try {
            Log.line("constructing client on game thread");
            new myau.init.Initializer();
            new Myau();
            started = true;
            Log.line("client ready");
        } catch (Throwable error) {
            Log.throwable("client initialization failed", error);
        }
    }
    public static boolean isStarted() { return started; }
}
