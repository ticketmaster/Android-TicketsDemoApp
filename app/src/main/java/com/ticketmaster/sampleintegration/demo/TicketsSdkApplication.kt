package com.ticketmaster.sampleintegration.demo

import android.app.Application
import com.ticketmaster.tickets.ticketssdk.TicketsSDKSingleton

class TicketsSdkApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        TicketsSDKSingleton.init(this)
    }
}
