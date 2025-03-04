package com.ticketmaster.sampleintegration.demo.lafc.data.stub

import com.ticketmaster.sampleintegration.demo.lafc.TicketCSV
import com.ticketmaster.sampleintegration.demo.lafc.data.AWSS3DataSource

object AWSS3TestDataSource : AWSS3DataSource {

    //To use this test data source, you will need to change the source of data in TicketsSdkHostActivity.kt to point here,
    //and add some TicketCSV data to the list.
    override suspend fun getCSVFile(bucketName: String, fileName: String): List<TicketCSV> = listOf()
}
