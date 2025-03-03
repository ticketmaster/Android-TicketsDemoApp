package com.ticketmaster.sampleintegration.demo.lafc.data

import com.ticketmaster.sampleintegration.demo.lafc.TicketCSV

interface AWSS3DataSource {

    suspend fun getCSVFile(bucketName: String, fileName: String): List<TicketCSV>
}
