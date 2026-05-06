package com.sistemaprestamista.mobile.printing

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import java.text.NumberFormat
import java.util.Locale

class A4ReceiptPrinter(
    private val context: Context,
) {
    fun printReceipt(receipt: PaymentReceipt) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val printManager = context.getSystemService(PrintManager::class.java)
                val adapter = view.createPrintDocumentAdapter("recibo-${receipt.receiptNumber}")
                val attributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                printManager.print("Recibo ${receipt.receiptNumber}", adapter, attributes)
            }
        }
        webView.loadDataWithBaseURL(null, html(receipt), "text/html", "UTF-8", null)
    }

    private fun html(receipt: PaymentReceipt): String {
        val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-DO"))

        return """
            <!doctype html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    @page { size: A4; margin: 18mm; }
                    body { font-family: sans-serif; color: #111827; }
                    h1 { font-size: 22px; margin: 0 0 4px; }
                    h2 { font-size: 16px; margin: 0 0 24px; font-weight: normal; color: #4b5563; }
                    .box { border: 1px solid #d1d5db; border-radius: 8px; padding: 16px; margin-bottom: 18px; }
                    .row { display: flex; justify-content: space-between; padding: 7px 0; border-bottom: 1px solid #e5e7eb; }
                    .row:last-child { border-bottom: 0; }
                    .label { color: #6b7280; }
                    .value { font-weight: 700; }
                    .total { font-size: 20px; }
                    .signature { margin-top: 70px; display: flex; gap: 48px; }
                    .line { flex: 1; border-top: 1px solid #111827; text-align: center; padding-top: 8px; }
                </style>
            </head>
            <body>
                <h1>Sistema Prestamista</h1>
                <h2>Recibo de pago ${receipt.receiptNumber}</h2>
                <div class="box">
                    ${row("Cliente", receipt.client?.fullName ?: "-")}
                    ${row("Prestamo", receipt.loanNumber ?: receipt.loanId.toString())}
                    ${row("Fecha", receipt.paymentDate ?: "-")}
                    ${row("Metodo", receipt.paymentMethod)}
                    ${row("Estado", receipt.status)}
                </div>
                <div class="box">
                    ${row("Monto pagado", currency.format(receipt.amount), "total")}
                    ${row("Capital aplicado", currency.format(receipt.principalPaid))}
                    ${row("Interes aplicado", currency.format(receipt.interestPaid))}
                    ${row("Mora aplicada", currency.format(receipt.lateFeePaid))}
                    ${row("Balance anterior", currency.format(receipt.previousBalance))}
                    ${row("Balance nuevo", currency.format(receipt.newBalance))}
                </div>
                <div class="signature">
                    <div class="line">Cliente</div>
                    <div class="line">Cobrador</div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun row(label: String, value: String, extraClass: String = ""): String {
        return """
            <div class="row $extraClass">
                <span class="label">${escape(label)}</span>
                <span class="value">${escape(value)}</span>
            </div>
        """.trimIndent()
    }

    private fun escape(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
