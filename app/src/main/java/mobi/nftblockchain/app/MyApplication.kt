package mobi.nftblockchain.app

import android.app.Application
import com.applovin.sdk.AppLovinSdk
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        AppLovinSdk.getInstance(this).initializeSdk()
    }
}
