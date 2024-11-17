package mobi.nftblockchain.app

import android.icu.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Constants {
    const val BANNER_KEY = "3a7f34c8a0f27465"
    const val OPEN_APP_KEY = "7aacb59047962247"
    const val INTERSTITIAL_KEY = "a74fa9ec3c4b1b0a"
    const val lastAdShowed: String = "lastAdShowed"
    const val ISBANNERSHOW: String = "isBannerShow"
    const val ISOPENAPPSHOW: String = "isOpeanAppShow"
    const val ISINTERSHOW: String = "isInterShow"
    const val BASE_URL: String = "BASE_URL"
    const val BASE_URL_PREFERENCE: String = "BASE_URL_PREFERENCE"
    var KEY_PREFERENCE: String = "KEY_PREFERENCE"

    const val SDK_KEY =
        "grwb0S72XXXzTWTybLKW7IWPWw_2muGRVyNix7kJ1kl7zxA1cVmokRl_liObGcBEEzo5CKut3oR8xaNmxEzuxd"
}

fun Long.toMinutes(): Long {
    val sdf = SimpleDateFormat("mm", Locale.getDefault())
    return sdf.format(Date(this)).toLong()
}