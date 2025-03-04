package com.ticketmaster.sampleintegration.demo.lafc

data class TicketCSV(
    val eventId: String,
    val sectionName: String,
    val rowName: String,
    val seatNumber: String,
    val metroCode: String
)
