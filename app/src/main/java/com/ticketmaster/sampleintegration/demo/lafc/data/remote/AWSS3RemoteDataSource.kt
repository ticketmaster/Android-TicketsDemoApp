package com.ticketmaster.sampleintegration.demo.lafc.data.remote

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.ticketmaster.sampleintegration.demo.lafc.TicketCSV
import com.ticketmaster.sampleintegration.demo.lafc.data.AWSS3DataSource
import java.io.BufferedReader
import java.io.InputStreamReader

class AWSS3RemoteDataSource(
    clientId: String,
    secret: String,
    private val client:AmazonS3Client = AmazonS3Client(
        BasicAWSCredentials(clientId, secret),
        Region.getRegion(Regions.US_WEST_2)
    ),
) : AWSS3DataSource {
        override suspend fun getCSVFile(bucketName: String, fileName: String): List<TicketCSV> {
            val s3Object = client.getObject(GetObjectRequest(bucketName, fileName))
            val reader = BufferedReader(InputStreamReader(s3Object.objectContent))
            val csvData = mutableListOf<TicketCSV>()

            reader.useLines { lines ->
                lines.drop(1).forEach { line ->
                    val columns = line.split(",")
                    if (columns.size >= 3) {
                        val ticketCSV = TicketCSV(
                            columns[0].trim(),
                            columns[1].trim(),
                            columns[2].trim(),
                            columns[3].trim(),
                            columns[4].trim()
                        )
                        csvData.add(ticketCSV)
                    }
                }
            }
            return csvData
        }
}
