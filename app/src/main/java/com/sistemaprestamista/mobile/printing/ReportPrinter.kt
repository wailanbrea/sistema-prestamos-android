package com.sistemaprestamista.mobile.printing

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.sistemaprestamista.mobile.data.model.AdminReportSummary
import com.sistemaprestamista.mobile.data.model.CollectorPerformanceRow
import com.sistemaprestamista.mobile.ui.components.MoneyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Imprime (o guarda como PDF) el resumen financiero y el rendimiento por cobrador
 * usando el framework de impresión de Android: el usuario elige impresora o
 * "Guardar como PDF" en el diálogo del sistema.
 */
class ReportPrinter(
    private val context: Context,
) {
    fun printFinancialSummary(
        summary: AdminReportSummary,
        collectors: List<CollectorPerformanceRow>,
        currencyCode: String,
        companyName: String,
    ) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val printManager = context.getSystemService(PrintManager::class.java)
                val adapter = view.createPrintDocumentAdapter("reporte-financiero")
                val attributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                printManager.print("Resumen financiero", adapter, attributes)
            }
        }
        webView.loadDataWithBaseURL(
            null,
            html(summary, collectors, currencyCode, companyName),
            "text/html",
            "UTF-8",
            null,
        )
    }

    private fun html(
        summary: AdminReportSummary,
        collectors: List<CollectorPerformanceRow>,
        currencyCode: String,
        companyName: String,
    ): String {
        val money = MoneyFormatter(currencyCode.ifBlank { "RD$" })
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es")).format(Date())

        fun metric(label: String, value: String): String =
            """<tr><th>$label</th><td class="right">$value</td></tr>"""

        val collectorRows = if (collectors.isEmpty()) {
            """<tr><td colspan="6" class="muted center">Sin datos de cobradores.</td></tr>"""
        } else {
            collectors.joinToString("") { c ->
                """
                <tr>
                    <td>${c.collector}</td>
                    <td class="right">${money.format(c.collected)}</td>
                    <td class="right">${money.format(c.disbursed)}</td>
                    <td class="right">${money.format(c.interest)}</td>
                    <td class="center">${c.activeAccounts}</td>
                    <td class="center">${c.overdueAccounts}</td>
                </tr>
                """.trimIndent()
            }
        }

        return """
        <!doctype html>
        <html lang="es">
        <head>
        <meta charset="utf-8">
        <style>
            body { font-family: sans-serif; font-size: 12px; color: #111827; padding: 16px; }
            h1 { font-size: 20px; text-align: center; margin: 0 0 4px; }
            .sub { text-align: center; color: #6b7280; margin: 0 0 16px; font-size: 11px; }
            h2 { font-size: 14px; margin: 18px 0 8px; border-bottom: 1px solid #d1d5db; padding-bottom: 4px; }
            table { width: 100%; border-collapse: collapse; margin: 8px 0; }
            th, td { border: 1px solid #e5e7eb; padding: 6px 8px; text-align: left; }
            th { background: #f3f4f6; }
            .right { text-align: right; }
            .center { text-align: center; }
            .muted { color: #6b7280; }
        </style>
        </head>
        <body>
            <h1>$companyName</h1>
            <p class="sub">Resumen financiero · Generado el $date</p>

            <h2>Indicadores financieros</h2>
            <table>
                ${metric("Capital invertido", money.format(summary.capitalInvested))}
                ${metric("Capital en la calle", money.format(summary.capitalOnStreet))}
                ${metric("Capital recuperado", money.format(summary.capitalRecovered))}
                ${metric("Interés ganado", money.format(summary.interestEarned))}
                ${metric("Mora cobrada", money.format(summary.lateFeeEarned))}
                ${metric("Gastos", money.format(summary.expenses))}
                ${metric("Nuevo desembolsado", money.format(summary.newDisbursed))}
                ${metric("Balance neto", money.format(summary.netBalance))}
                ${metric("ROI", "%.2f%%".format(summary.roi))}
                ${metric("Retorno mensual", "%.2f%%".format(summary.monthlyReturn))}
            </table>

            <h2>Clientes</h2>
            <table>
                ${metric("Activos", summary.activeClients.toString())}
                ${metric("Inactivos", summary.inactiveClients.toString())}
                ${metric("En mora", summary.overdueClients.toString())}
            </table>

            <h2>Rendimiento por cobrador</h2>
            <table>
                <thead>
                    <tr>
                        <th>Cobrador</th>
                        <th class="right">Cobrado</th>
                        <th class="right">Desembolsado</th>
                        <th class="right">Interés</th>
                        <th class="center">Activas</th>
                        <th class="center">En mora</th>
                    </tr>
                </thead>
                <tbody>
                    $collectorRows
                </tbody>
            </table>
        </body>
        </html>
        """.trimIndent()
    }
}
