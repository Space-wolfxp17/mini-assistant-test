package com.ordis.app.device

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs

object PhoneAnalyzer {
    fun analyze(context: Context): String {
        val pm = context.packageManager
        val appCount = pm.getInstalledApplications(0)
            .count { pm.getLaunchIntentForPackage(it.packageName) != null }

        val stat = StatFs(Environment.getDataDirectory().path)
        val freeGb = stat.availableBytes / (1024.0 * 1024 * 1024)
        val totalGb = stat.totalBytes / (1024.0 * 1024 * 1024)

        return """
            Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            Устройство: ${Build.MANUFACTURER} ${Build.MODEL}
            Приложений: $appCount
            Память: ${"%.1f".format(freeGb)} ГБ из ${"%.1f".format(totalGb)} ГБ свободно
        """.trimIndent()
    }
}
