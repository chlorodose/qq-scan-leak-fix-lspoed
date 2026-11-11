package local.lilith.qqscanfix;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Hook implements IXposedHookLoadPackage {
    private static final String TAG = "QQScanLeakFix";

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!"com.tencent.mobileqq".equals(lp.packageName)
                || !"com.tencent.mobileqq".equals(lp.processName)) return;
        try {
            java.util.Set<XC_MethodHook.Unhook> hooks = XposedBridge.hookAllMethods(
                    XposedHelpers.findClass("android.os.StrictMode", null),
                    "onVmPolicyViolation", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    Throwable origin = (Throwable) param.args[0];
                    if (!"android.os.strictmode.IntentReceiverLeakedViolation".equals(origin.getClass().getName())
                            || !ScanLeakFilter.matches(origin)) return;
                    // Only this known scanner leak is exempted. Do not alter VmPolicy,
                    // receiver registration, other leak types, or signal delivery.
                    Log.w(TAG, "Bypassed QQScanActivity receiver-leak fatal path; origin follows", origin);
                    param.setResult(null);
                }
            });
            if (hooks.isEmpty()) throw new IllegalStateException("No VM violation overloads found");
            Log.i(TAG, "v2 active in QQ main process; VM violation overloads=" + hooks.size());
        } catch (Throwable error) {
            Log.e(TAG, "Hook installation failed; no policy was changed", error);
        }
    }
}
