package com.ticketmaster.sampleintegration.demo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ticketmaster.discoveryapi.enums.TMMarketDomain
import com.ticketmaster.discoveryapi.models.DiscoveryAbstractEntity
import com.ticketmaster.discoveryapi.models.DiscoveryAttraction
import com.ticketmaster.discoveryapi.models.DiscoveryEvent
import com.ticketmaster.discoveryapi.models.DiscoveryVenue
import com.ticketmaster.prepurchase.TMPrePurchase
import com.ticketmaster.prepurchase.TMPrePurchaseFragmentFactory
import com.ticketmaster.prepurchase.TMPrePurchaseWebsiteConfiguration
import com.ticketmaster.prepurchase.data.Location
import com.ticketmaster.prepurchase.internal.UpdateLocationInfo
import com.ticketmaster.prepurchase.listener.TMPrePurchaseNavigationListener
import com.ticketmaster.tickets.ticketssdk.TicketsSDKSingleton
import kotlinx.parcelize.Parcelize

class PrePurchaseActivity : AppCompatActivity() {
    private lateinit var fragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tmPrePurchase = TMPrePurchase(
            discoveryAPIKey = TicketsSDKSingleton.getTMAuthentication()!!.apiKey,
            brandColor = ContextCompat.getColor(
                this@PrePurchaseActivity, R.color.colorPrimary
            )
        )

        val prePurchaseFlow = intent.getSerializableExtra(PREPURCHASE_FLOW) as PrePurchaseFlow
        val tMPrePurchaseWebsiteConfiguration = TMPrePurchaseWebsiteConfiguration(
            abstractEntity = when (prePurchaseFlow) {
                PrePurchaseFlow.VENUE -> DiscoveryVenue(
                    hostID = intent.getStringExtra(
                        PREPURCHASE_VENUE_ID
                    )
                )

                PrePurchaseFlow.ATTRACTION -> DiscoveryAttraction(
                    hostID = intent.getStringExtra(
                        PREPURCHASE_ATTRACTION_ID
                    )
                )
            }, hostType = TMMarketDomain.valueOf("US")
        )

        val bundle = tmPrePurchase.getPrePurchaseBundle(
            tMPrePurchaseWebsiteConfiguration
        )

        val factory =
            TMPrePurchaseFragmentFactory(tmPrePurchaseNavigationListener = PrePurchaseNavigationListener(
                activity = this@PrePurchaseActivity,
                apiKey = tmPrePurchase.discoveryAPIKey.orEmpty()
            ) {
                finish()
            })

        supportFragmentManager.fragmentFactory = factory

        fragment = factory.instantiatePrePurchase(classLoader).apply {
            arguments = bundle
        }

        supportFragmentManager.beginTransaction().add(android.R.id.content, fragment).commit()
    }

    companion object {
        fun newInstance(
            from: Activity, venueId: String, attractionId: String, flow: PrePurchaseFlow
        ) {

            from.startActivity(Intent(
                from, PrePurchaseActivity::class.java
            ).apply {
                putExtra(PREPURCHASE_VENUE_ID, venueId)
                putExtra(PREPURCHASE_ATTRACTION_ID, attractionId)
                putExtra(PREPURCHASE_FLOW, flow)
            })
        }

        private const val PREPURCHASE_VENUE_ID = "VENUE_ID"
        private const val PREPURCHASE_ATTRACTION_ID = "ATTRACTION_ID"
        private const val PREPURCHASE_FLOW = "FLOW"
    }

    enum class PrePurchaseFlow {
        VENUE, ATTRACTION
    }

    class PrePurchaseNavigationListener(
        private val activity: Activity,
        private val apiKey: String,
        private val closeScreen: () -> Unit
    ) : TMPrePurchaseNavigationListener {
        // openEventDetailsPage is the bridge between PrePurchase and Purchase.
        // It must be implemented
        override fun openEventDetailsPage(
            abstractEntity: DiscoveryAbstractEntity?, event: DiscoveryEvent
        ) {
            PurchaseActivity.newInstance(
                activity, apiKey, event.hostID ?: "", TMMarketDomain.US, "US"
            )
        }

        override fun updateCurrentLocation(updateLocationInfo: (UpdateLocationInfo) -> Unit) {
            TODO("Not yet implemented")
        }

        override fun onPrePurchaseClosed() {
            closeScreen.invoke()
        }

        override fun onDidRequestCurrentLocation(
            globalMarketDomain: TMMarketDomain?,
            completion: (Location?) -> Unit
        ) {
            // MUST implement if requesting location from users' as well as
            // requesting they grant your application permission to their location
        }

        override fun onDidRequestNativeLocationSelector() {
            TODO("Not yet implemented")
        }

        override fun onDidUpdateCurrentLocation(
            globalMarketDomain: TMMarketDomain?, location: Location
        ) {
        }

        override fun onPrePurchaseBackPressed() {
            TODO("Not yet implemented")
        }
    }

    @Parcelize
    data class ExtraInfo(
        val region: String
    ) : Parcelable
}
