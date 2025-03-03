package com.ticketmaster.sampleintegration.demo.lafc.data.remote

import com.ticketmaster.sampleintegration.demo.lafc.TicketCSV
import com.ticketmaster.sampleintegration.demo.lafc.data.AWSS3DataSource
import com.ticketmaster.tickets.util.Log
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.io.BufferedReader
import java.io.InputStreamReader

class AWSS3RemoteDataSource(
    clientId: String,
    secret: String,
    private val client: S3Client = S3Client.builder()
        .httpClient(UrlConnectionHttpClient.create())
        .region(Region.US_WEST_2)
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    clientId,
                    secret
                )
            )
        )
        .build(),
) : AWSS3DataSource {

    override suspend fun getCSVFile(bucketName: String, fileName: String): List<TicketCSV> {
        val request =
            GetObjectRequest.builder().bucket(bucketName).key("$bucketName/$fileName").build()
        val inputStream = client.getObject(request)
        val reader = BufferedReader(InputStreamReader(inputStream))
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
                    Log.i("LAFCMetro", "Tickets CSV: $ticketCSV")
                    csvData.add(ticketCSV)
                }
            }
        }
        return csvData
    }
}
