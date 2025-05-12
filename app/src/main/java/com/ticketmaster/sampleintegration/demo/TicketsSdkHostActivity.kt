package com.ticketmaster.sampleintegration.demo

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.ticketmaster.authenticationsdk.AuthSource
import com.ticketmaster.authenticationsdk.TMAuthentication
import com.ticketmaster.authenticationsdk.TMXDeploymentEnvironment
import com.ticketmaster.authenticationsdk.TMXDeploymentRegion
import com.ticketmaster.sampleintegration.demo.databinding.ActivityTicketsSdkHostBinding
import com.ticketmaster.sampleintegration.demo.databinding.LayoutLoadingViewBinding
import com.ticketmaster.tickets.EventOrders
import com.ticketmaster.tickets.TicketsModuleDelegate
import com.ticketmaster.tickets.event_tickets.DirectionsModule
import com.ticketmaster.tickets.event_tickets.ModuleBase
import com.ticketmaster.tickets.event_tickets.NAMWebPageSettings
import com.ticketmaster.tickets.event_tickets.SeatUpgradesModule
import com.ticketmaster.tickets.event_tickets.TicketsSDKModule
import com.ticketmaster.tickets.event_tickets.modules.next_home_game.NextHomeGameModule
import com.ticketmaster.tickets.event_tickets.modules.upcoming_artist_team.UpcomingArtistTeamModule
import com.ticketmaster.tickets.event_tickets.modules.upcoming_venue.UpcomingVenueModule
import com.ticketmaster.tickets.eventanalytic.UserAnalyticsDelegate
import com.ticketmaster.tickets.ticketssdk.TicketsColors
import com.ticketmaster.tickets.ticketssdk.TicketsSDKClient
import com.ticketmaster.tickets.ticketssdk.TicketsSDKSingleton
import com.ticketmaster.tickets.util.TMTicketsBrandingColor
import com.ticketmaster.tickets.venuenext.VenueNextModule
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TicketsSdkHostActivity : AppCompatActivity() {

    private val binding by lazy { ActivityTicketsSdkHostBinding.inflate(layoutInflater) }

    private val progressDialog: AlertDialog by lazy {
        AlertDialog.Builder(this)
            .setView(LayoutLoadingViewBinding.inflate(layoutInflater).root)
            .setCancelable(false)
            .create().apply {
                setCanceledOnTouchOutside(false)
            }
    }
    private val mCancelledDialog: AlertDialog by lazy {
        AlertDialog.Builder(this@TicketsSdkHostActivity)
            .setTitle(R.string.configuration_error_title)
            .setMessage(R.string.configuration_error_message)
            .setPositiveButton(R.string.retry) { _, _ ->
                progressDialog.show()
                setupAuthenticationSDK()
            }.setNegativeButton(R.string.cancel, null).setCancelable(false).create().apply {
                setCanceledOnTouchOutside(false)
            }
    }

    private val ticketColor: Int by lazy { "#ef3e42".toColorInt() }

    private val brandingColor: Int by lazy { BuildConfig.BRANDING_COLOR.toColorInt() }

    private val headerColor: Int by lazy { BuildConfig.BRANDING_COLOR.toColorInt() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tickets_sdk_host)
        binding.layoutInstructionView.gettingStartedContainer.isVisible = false
        progressDialog.show()
        setupAuthenticationSDK()
        setupAnalytics()
        setCustomModules()
        TicketsSDKSingleton.sessionExpiredDelegate.observe(this) {
            TicketsSDKSingleton.logout(this) {
                onLogout()
            }
        }
    }

    private fun onLogout() {
        launchLogin()
    }

    private fun launchLogin() {
        TicketsSDKSingleton.getLoginIntent(this)?.let { resultLauncher.launch(it) }
    }

    private fun setupAuthenticationSDK() {
        /** After creating the TMAuthentication.Builder object, the build() function is a suspend function,
         * that is way it is required to be called inside a coroutine.
         *
         * When calling the build function there are two options:
         * - By calling the build(fragmentActivity) without the callback, will return a TMAuthentication object
         * - using build(fragmentActivity, callback), in the callback will retrieve a TMAuthentication object
         * **/
        lifecycleScope.launch {
            // After creating the TMAuthentication.Builder object, the build() function is a suspend function,
            // that is way it is required to be called inside a coroutine.
            val authentication = createTMAuthenticationBuilder().build(this@TicketsSdkHostActivity)
            setupTicketsSDKClient(authentication)
        }
    }

    private fun createTMAuthenticationBuilder(): TMAuthentication.Builder =
        TMAuthentication.Builder().apiKey(BuildConfig.CONSUMER_KEY) // Your consumer key
            .clientName(BuildConfig.TEAM_NAME) // Team name to be displayed
            //Optional value to show screen previous to login
            .modernAccountsAutoQuickLogin(false)
            //Optional value to define the colors for the Authentication page
            .colors(createTMAuthenticationColors(brandingColor))
            .region(TMXDeploymentRegion.US) // Region that the SDK will use. Default is US
            .environment(TMXDeploymentEnvironment.Production) // Environment that the SDK will use. Default is Production

    @SuppressLint("ConflictingOnColor")
    private fun createTMAuthenticationColors(color: Int): TMAuthentication.ColorTheme =
        TMAuthentication.ColorTheme(
            //The Color class is part of the Compose library
            lightColors(
                primary = Color(color),
                primaryVariant = Color(color),
                secondary = Color(color),
                onPrimary = Color.White // Color used for text and icons displayed on top of the primary color.
            ), darkColors(
                primary = Color(color),
                primaryVariant = Color(color),
                secondary = Color(color),
                onPrimary = Color.White // Color used for text and icons displayed on top of the primary color.
            )
        )

    private suspend fun setupTicketsSDKClient(authentication: TMAuthentication) {
        //After called the build function of TMAuthentication.Builder object, we validate if configuration is different
        //from null, to verify if it was retrieved satisfactory a configuration file from the given params.
        authentication.configuration?.let {
            val tokenMap = validateAuthToken(authentication)

            TicketsSDKClient.Builder()
                //Authentication object
                .authenticationSDKClient(authentication)
                //Optional value to define the colors for the Tickets page
                .colors(createTicketsColors(android.graphics.Color.parseColor(BuildConfig.BRANDING_COLOR)))
                //Function that generates a TicketsSDKClient object
                .build(this@TicketsSdkHostActivity).apply {
                    //After creating the TicketsSDKClient object, add it into the TicketsSDKSingleton
                    TicketsSDKSingleton.setTicketsSdkClient(this)

                    setupTicketsColors()

                    //Validate if there is an active token.
                    if (tokenMap.isNotEmpty()) {
                        //If there is an active token, it launches the event fragment
                        launchTicketsView()
                    } else {
                        //If there is no active token, it launches a login intent. Launch an ActivityForResult, if result
                        //is RESULT_OK, there is an active token to be retrieved.
                        TicketsSDKSingleton
                            .getLoginIntent(this@TicketsSdkHostActivity)
                            ?.let { resultLauncher.launch(it) }
                    }
                }
            /**
             * For testing purposes, you can use this to set to preprod etc.
             */
            TicketsSDKSingleton.setEnvironment(
                this,
                TicketsSDKSingleton.SDKEnvironment.Production,
                TicketsSDKSingleton.HostEnvironment.US
            )
        }
        if (authentication.configuration == null) {
            progressDialog.dismiss()
            mCancelledDialog.show()
        }
    }

    private suspend fun validateAuthToken(authentication: TMAuthentication): Map<AuthSource, String> {
        val tokenMap = mutableMapOf<AuthSource, String>()
        AuthSource.entries.forEach {
            //Validate if there is an active token for the AuthSource, if not it returns null.
            authentication.getToken(it)?.let { token ->
                tokenMap[it] = token
            }
        }
        return tokenMap
    }

    private fun createTicketsColors(color: Int): TicketsColors = TicketsColors(
        lightColors(
            primary = Color(color), primaryVariant = Color(color), secondary = Color(color)
        ), darkColors(
            primary = Color(color), primaryVariant = Color(color), secondary = Color(color)
        )
    )

    private fun setupTicketsColors() {
        //Affects the color of the container of ticket.
        TMTicketsBrandingColor.setTicketColor(this@TicketsSdkHostActivity, ticketColor)

        //Affects the branding color, like the color of the buttons
        TMTicketsBrandingColor.setBrandingColor(this@TicketsSdkHostActivity, brandingColor)

        //Affects the header color
        TMTicketsBrandingColor.setHeaderColor(this@TicketsSdkHostActivity, headerColor)
    }

    private fun launchTicketsView() {
        binding.layoutInstructionView.gettingStartedContainer.isVisible = false
        progressDialog.dismiss()
        //Retrieve an EventFragment
        TicketsSDKSingleton.getEventsFragment(this@TicketsSdkHostActivity)?.let {
            supportFragmentManager.beginTransaction().replace(R.id.tickets_sdk_view, it).commit()
        }

    }

    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            RESULT_OK -> {
                launchTicketsView()
            }

            RESULT_CANCELED -> {
                progressDialog.dismiss()
            }
        }
    }

    private fun setupAnalytics() {
        //Initialize observer that will handle the analytics events
        //Must be called the observeForever as this will kept alive the observer until
        //the Activity is destroyed
        UserAnalyticsDelegate.handler.getLiveData().observeForever(userAnalyticsObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        //Remove the observer in the onDestroy, as it won't be needed to keep traking
        //the analytics events.
        UserAnalyticsDelegate.handler.getLiveData().removeObserver(userAnalyticsObserver)
    }

    private val userAnalyticsObserver = Observer<UserAnalyticsDelegate.AnalyticsData?> {
        it?.let {
            Log.d("Analytics", "Action name: ${it.actionName}, data: ${it.data}")
        }
    }

    private fun setCustomModules() {
        TicketsSDKSingleton.moduleDelegate = object : TicketsModuleDelegate {

            override fun getCustomModulesLiveData(order: TicketsModuleDelegate.Order): LiveData<List<TicketsSDKModule>> {
                val liveData: MutableLiveData<List<TicketsSDKModule>> =
                    MutableLiveData<List<TicketsSDKModule>>()
                val modules: ArrayList<TicketsSDKModule> = ArrayList()

                val venueNextModule = VenueNextModule.Builder(order.venueId).build()
                modules.add(venueNextModule.createVenueNextView(this@TicketsSdkHostActivity) {
                    //Venue next click event
                })

                modules.add(getDirectionsModule(order.orderInfo.latLng))

                //Validation that retrieve the event source type. In case of null,
                // we recommend to don't display the Seat Upgrade module.
                val firstTicketSource = order.tickets.firstOrNull()?.source
                if (firstTicketSource != null) {
                    modules.add(
                        SeatUpgradesModule(
                            webPageSettings = NAMWebPageSettings(
                                this@TicketsSdkHostActivity,
                                firstTicketSource
                            ),
                            eventId = order.eventId
                        )
                    )
                }

                runBlocking {
                    modules.add(
                        NextHomeGameModule(
                            this@TicketsSdkHostActivity,
                            order.eventId,
                            DiscoveryEventNetworkRepository()
                        ) { attractionId ->
                            PrePurchaseActivity.newInstance(
                                this@TicketsSdkHostActivity,
                                attractionId = attractionId,
                                venueId = order.venueId,
                                flow = PrePurchaseActivity.PrePurchaseFlow.ATTRACTION
                            )
                        }.build()
                    )
                }
                modules.add(UpcomingVenueModule(this@TicketsSdkHostActivity).build())
                runBlocking {
                    modules.add(
                        UpcomingArtistTeamModule(
                            this@TicketsSdkHostActivity,
                            DiscoveryEventNetworkRepository(),
                            order.eventId,
                            { attractionId ->
                                PrePurchaseActivity.newInstance(
                                    this@TicketsSdkHostActivity,
                                    attractionId = attractionId,
                                    venueId = order.venueId,
                                    flow = PrePurchaseActivity.PrePurchaseFlow.ATTRACTION
                                )
                            },
                            { attractionId ->
                                PrePurchaseActivity.newInstance(
                                    this@TicketsSdkHostActivity,
                                    attractionId = attractionId,
                                    venueId = order.venueId,
                                    flow = PrePurchaseActivity.PrePurchaseFlow.VENUE
                                )
                            }).build()
                    )

                }
                liveData.value = modules
                return liveData
            }

            override fun userDidPressActionButton(
                buttonTitle: String?, callbackValue: String?, eventOrders: EventOrders?
            ) {
            }
        }
    }

    private fun getDirectionsModule(
        latLng: TicketsModuleDelegate.LatLng?
    ): ModuleBase {
        return DirectionsModule(
            this, latLng?.latitude, latLng?.longitude
        ).build()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            //Validates if there is an active user logged in.
            if (isLoggedIn()) {
                binding.layoutInstructionView.gettingStartedContainer.visibility = View.GONE
            } else {
                binding.layoutInstructionView.gettingStartedContainer.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val logoutMenu = menu.findItem(R.id.action_logout)
        val loginMenu = menu.findItem(R.id.action_login)
        lifecycleScope.launch {
            //Validates if there is an active user logged in.
            if (isLoggedInSync()) {
                logoutMenu.isVisible = true
                loginMenu.isVisible = false
            } else {
                logoutMenu.isVisible = false
                loginMenu.isVisible = true
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_logout -> {
            invalidateOptionsMenu()
            logout()
            true
        }

        R.id.action_login -> {
            setupAuthenticationSDK()
            true
        }

        else -> false
    }

    private fun logout() {
        //Logout from TicketsSDKClient and TMAuthentication
        TicketsSDKSingleton.logout(this@TicketsSdkHostActivity) {
            //listener call after the logout process is completed.
            TicketsSDKSingleton.getLoginIntent(this)?.let { resultLauncher.launch(it) }

            //remove the fragment from the container
            supportFragmentManager.findFragmentById(R.id.tickets_sdk_view)?.let {
                supportFragmentManager.beginTransaction().remove(it).commit()
            }
        }
    }

    //By calling the runBlocking function, it runs the suspend function in the Main Thread.
    private fun isLoggedInSync(): Boolean = runBlocking {
        isLoggedIn()
    }

    //Validates if there is an access token for each AuthSource, if there is one active, returns true.
    private suspend fun isLoggedIn(): Boolean =
        TicketsSDKSingleton.getTMAuthentication()?.let { authentication ->
            AuthSource.entries.forEach {
                if (authentication.getToken(it)?.isNotBlank() == true) {
                    return true
                }
            }
            return false
        } ?: false

}
