package com.ticketmaster.sampleintegration.demo.lafc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.ticketmaster.sampleintegration.demo.R
import com.ticketmaster.sampleintegration.demo.lafc.data.AWSS3DataSource
import com.ticketmaster.tickets.event_tickets.ModuleBase
import com.ticketmaster.tickets.event_tickets.TicketsSDKModule
import com.ticketmaster.tickets.event_tickets.TmxEventTicketsResponseBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AwsCSVModule(
    private val context: Context,
    private val repository: AWSS3DataSource,
    private val bucketName: String,
    private val fileName: String
) : TicketsSDKModule {

    suspend fun build(eventId: String, tickets: List<TmxEventTicketsResponseBody.EventTicket>) =
        ModuleBase(context).apply {
            withContext(Dispatchers.IO) {
                repository.getCSVFile(bucketName, fileName).takeIf { it.isNotEmpty() }
            }?.filter { csv ->
                csv.eventId == eventId &&
                    tickets.any { ticket ->
                        ticket.mSectionLabel == csv.sectionName &&
                            ticket.mRowLabel == csv.rowName &&
                            ticket.mSeatLabel == csv.seatNumber
                    }
            }?.let { data ->
                val container = LinearLayout(context).apply {
                    layoutParams = LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    orientation = LinearLayout.VERTICAL
                }
                data.forEach { metropass ->
                    container.addView(generateTicketView(context, container, metropass))
                }
                setHeader(container)
            }
        }

    private fun generateTicketView(
        context: Context,
        container: ViewGroup,
        metropass: TicketCSV
    ): View {
        val view =
            LayoutInflater.from(context).inflate(R.layout.lafc_metro_ticket, container, false)
        view.findViewById<TextView>(R.id.lafcMetroPass).text = context.getString(
            R.string.lafc_metro_title,
            metropass.sectionName,
            metropass.rowName,
            metropass.seatNumber
        )
        view.findViewById<ImageView>(R.id.qrCodeView)
            .setImageBitmap(generateQRCode(metropass.metroCode))
        val backgroundView = view.findViewById<ImageView>(R.id.background)

        Glide.with(context)
            .load("https://images.pexels.com/photos/297836/pexels-photo-297836.jpeg") // Replace with your actual image URL
            .fitCenter()
            .into(backgroundView)

        return view
    }

    private fun generateQRCode(text: String, size: Int = 400, cornerRadius: Float = 30f): Bitmap {
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, 400)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
