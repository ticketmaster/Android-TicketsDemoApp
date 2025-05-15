package com.ticketmaster.sampleintegration.demo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ticketmaster.authenticationsdk.TMXDeploymentEnvironment
import com.ticketmaster.authenticationsdk.TMXDeploymentRegion
import com.ticketmaster.discoveryapi.enums.TMEnvironment
import com.ticketmaster.discoveryapi.enums.TMMarketDomain
import com.ticketmaster.foundation.entity.TMAuthenticationParams
import com.ticketmaster.purchase.TMPurchase
import com.ticketmaster.purchase.TMPurchaseFragmentFactory
import com.ticketmaster.purchase.TMPurchaseWebsiteConfiguration
import com.ticketmaster.purchase.exception.TmInvalidConfigurationException
import com.ticketmaster.purchase.listener.TMPurchaseNavigationListener

class PurchaseActivity : AppCompatActivity() {
    companion object {

        fun newInstance(
            from: Activity,
            apiKey: String,
            eventId: String,
            market: TMMarketDomain,
            region: String
        ) {

            val tmPurchase = TMPurchase(
                apiKey = apiKey,
                brandColor = ContextCompat.getColor(
                    from,
                    R.color.colorPrimary
                ),
                environment = TMEnvironment.Production,
                purchaseUrlScheme = null,
            )
            val tmPurchaseWeb = TMPurchaseWebsiteConfiguration(
                eventId,
                market,
            )
            val extraInfo = PrePurchaseActivity.ExtraInfo(
                region
            )

            from.startActivity(
                Intent(
                    from, PurchaseActivity::class.java
                ).apply {
                    putExtra(TMPurchase::class.java.name, tmPurchase)
                    putExtra(
                        TMPurchaseWebsiteConfiguration::class.java.name,
                        tmPurchaseWeb
                    )
                    putExtra(PrePurchaseActivity.ExtraInfo::class.java.name, extraInfo)
                }
            )
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val factory = TMPurchaseFragmentFactory(
            tmPurchaseNavigationListener = PurchaseNavigationListener { finish() }
        ).apply {
            supportFragmentManager.fragmentFactory = this
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout_purchase)

        if (savedInstanceState == null) {

            val tmPurchase: TMPurchase =
                intent.extras?.getParcelable(TMPurchase::class.java.name)
                    ?: throw TmInvalidConfigurationException()

            val tmPurchaseWebsiteConfiguration: TMPurchaseWebsiteConfiguration =
                intent.extras?.getParcelable(TMPurchaseWebsiteConfiguration::class.java.name)
                    ?: throw TmInvalidConfigurationException()

            // ExtraInfo exists to pass in data that TicketsSDK might need from RetailSDK, such
            // as the deployment region
            val extraInfo: PrePurchaseActivity.ExtraInfo =
                intent.extras?.getParcelable(PrePurchaseActivity.ExtraInfo::class.java.name)
                    ?: throw TmInvalidConfigurationException()


            val bundle = tmPurchase.getPurchaseBundle(
                tmPurchaseWebsiteConfiguration,
                setupTMAuthParams(
                    tmPurchase,
                    getRegion(extraInfo.region)
                )
            )

            val fragment = factory.instantiatePurchase(classLoader).apply {
                arguments = bundle
            }

            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, fragment)
                .commit()
        }
    }

    private fun getRegion(region: String): TMXDeploymentRegion {
        return when (region) {
            "UK" -> TMXDeploymentRegion.UK
            else -> TMXDeploymentRegion.US
        }
    }

    private fun setupTMAuthParams(
        tmPurchase: TMPurchase,
        region: TMXDeploymentRegion
    ): TMAuthenticationParams = TMAuthenticationParams(
        apiKey = tmPurchase.apiKey,
        clientName = "Ticketmaster Demo",
        environment = TMXDeploymentEnvironment.Production,
        region = region,
        quickLogin = false,
        autoQuickLogin = true
    )
}

class PurchaseNavigationListener(private val closeScreen: () -> Unit) :
    TMPurchaseNavigationListener {
    override fun errorOnEventDetailsPage(error: Exception) {}

    override fun onPurchaseClosed() {
        closeScreen.invoke()
    }
}
