package local.lilith.qqscanfix;

final class ScanLeakFilter {
    static boolean matches(Throwable origin) {
        if (origin == null) return false;
        for (StackTraceElement frame : origin.getStackTrace()) {
            if ("com.tencent.mobileqq.olympic.activity.QQScanActivity".equals(frame.getClassName())
                    && "doOnCreate".equals(frame.getMethodName())) return true;
        }
        return false;
    }
}
