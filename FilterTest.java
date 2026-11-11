package local.lilith.qqscanfix;
public final class FilterTest {
    public static void main(String[] args) {
        if (ScanLeakFilter.matches(null)) throw new AssertionError("null origin");
        Throwable t = new Throwable();
        t.setStackTrace(new StackTraceElement[]{new StackTraceElement(
            "com.tencent.mobileqq.olympic.activity.QQScanActivity", "doOnCreate", null, -1)});
        if (!ScanLeakFilter.matches(t)) throw new AssertionError("scanner leak missed");
        t.setStackTrace(new StackTraceElement[]{new StackTraceElement(
            "com.tencent.mobileqq.activity.SplashActivity", "doOnCreate", null, -1)});
        if (ScanLeakFilter.matches(t)) throw new AssertionError("unrelated leak suppressed");
        t.setStackTrace(new StackTraceElement[]{new StackTraceElement(
            "com.tencent.mobileqq.olympic.activity.QQScanActivity", "doOnDestroy", null, -1)});
        if (ScanLeakFilter.matches(t)) throw new AssertionError("non-registration origin suppressed");
        System.out.println("Filter checks passed");
    }
}
