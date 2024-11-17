package mobi.nftblockchain.app

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAppOpenAd
import com.applovin.sdk.AppLovinSdk
import mobi.nftblockchain.app.Constants.OPEN_APP_KEY

class ExampleAppOpenManager(applicationContext: Context) : LifecycleObserver, MaxAdListener
{
    private var appOpenAd: MaxAppOpenAd = MaxAppOpenAd(OPEN_APP_KEY, applicationContext)
    private var context: Context = applicationContext

    init
    {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        appOpenAd.setListener(this)
        appOpenAd.loadAd()
    }

    private fun showAdIfReady()
    {
        if (!AppLovinSdk.getInstance(context).isInitialized) return

        if (appOpenAd.isReady)
        {
            appOpenAd.showAd(OPEN_APP_KEY)
        }
        else
        {
            appOpenAd.loadAd()
//            Toast.makeText(context, "OpenApp is not ready", Toast.LENGTH_SHORT).show()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart()
    {
        showAdIfReady()
    }

    override fun onAdLoaded(ad: MaxAd) {}
    override fun onAdLoadFailed(adUnitId: String, error: MaxError) {}
    override fun onAdDisplayed(ad: MaxAd) {}
    override fun onAdClicked(ad: MaxAd) {}

    override fun onAdHidden(ad: MaxAd)
    {
        appOpenAd.loadAd()
    }

    override fun onAdDisplayFailed(ad: MaxAd, error: MaxError)
    {
        appOpenAd.loadAd()
    }
}