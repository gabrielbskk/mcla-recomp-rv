package org.mcla.recomp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

final class MclaBuildVariant {
    private MclaBuildVariant() {}

    static boolean isMali(Context context) {
        try {
            Bundle metadata = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA).metaData;
            return metadata != null && "mali".equals(metadata.getString("org.mcla.recomp.GPU_VARIANT"));
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
