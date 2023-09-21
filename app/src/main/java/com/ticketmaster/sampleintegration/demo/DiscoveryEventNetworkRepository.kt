package com.ticketmaster.sampleintegration.demo

import com.ticketmaster.discoveryapi.repository.DiscoveryEventService
import com.ticketmaster.tickets.event_tickets.modules.model.EventDetails
import com.ticketmaster.tickets.event_tickets.modules.next_home_game.model.EventSearchResults
import com.ticketmaster.tickets.event_tickets.modules.repository.DiscoveryEventRepository
import com.ticketmaster.tickets.ticketssdk.TicketsSDKSingleton
import com.ticketmaster.voltron.DiscoveryApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DiscoveryEventNetworkRepository : DiscoveryEventRepository {
    private val retrofit = Retrofit.Builder()
        .client(
            OkHttpClient.Builder()
                .addInterceptor(Interceptor { chain ->
                    val originalRequest = chain.request()
                    val originalUrl = originalRequest.url

                    val newUrlBuilder = originalUrl.newBuilder()
                    newUrlBuilder.addQueryParameter(
                        "apikey",
                        TicketsSDKSingleton.getTMAuthentication()!!.apiKey
                    )

                    chain.proceed(
                        originalRequest
                            .newBuilder()
                            .url(newUrlBuilder.build())
                            .build()
                    )
                })
                .build()
        )
        .baseUrl("https://app.ticketmaster.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    override suspend fun getEventDetails(eventId: String): EventDetails? {
        val response = retrofit.create(DiscoveryEventService::class.java)
            .getEventDetails("", eventId, "", "", "").body()
        if (response != null) {
            val embeddedContainer = response.embeddedContainer
            return EventDetails(embeddedContainer?.discoveryAttractionItemList?.map {
                EventDetails.Attractions(it.id)
            } ?: emptyList(), embeddedContainer?.discoveryVenueItemList?.map {
                EventDetails.Venues(it.id ?: "")
            } ?: emptyList())
        }
        return null
    }

    override suspend fun searchEvents(params: Map<String, String>): EventSearchResults? {
        val response =
            retrofit.create(DiscoveryApi::class.java).getEventsList(params).execute().body()
        if (response != null) {
            val eventsEmbedded = response.eventsEmbedded
            return EventSearchResults(eventsEmbedded!!.events!!.map {
                EventSearchResults.Event(
                    it.id,
                    it.name,
                    it.eventDatesResponse.start.dateTime,
                    it.images.map { EventSearchResults.Event.Image(it.ratio, it.width, it.url) })
            })
        }
        return null
    }
}