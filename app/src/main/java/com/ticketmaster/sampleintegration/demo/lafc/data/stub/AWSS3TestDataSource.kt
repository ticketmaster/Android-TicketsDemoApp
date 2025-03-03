package com.ticketmaster.sampleintegration.demo.lafc.data.stub

import com.ticketmaster.sampleintegration.demo.lafc.TicketCSV
import com.ticketmaster.sampleintegration.demo.lafc.data.AWSS3DataSource

object AWSS3TestDataSource : AWSS3DataSource {

    override suspend fun getCSVFile(bucketName: String, fileName: String): List<TicketCSV> = listOf(
        TicketCSV("ELF50222", "CVLG1", "A", "1", "PLZEWUUVSEJZASCZETQ"),
        TicketCSV("ELF50222", "CVLG1", "A", "2", "TTMMRWLYNHQJBPTQDD")
    )
}
