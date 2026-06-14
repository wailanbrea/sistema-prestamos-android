package com.sistemaprestamista.mobile.ui

/**
 * Traducciones centralizadas de los estados que devuelve el backend (en inglés)
 * a etiquetas en español para mostrar en la UI.
 *
 * Punto único de cambio: cuando se agregue soporte bilingüe (es/en) con recursos
 * de strings de Android, basta con cambiar estas funciones para que lean de
 * `stringResource(...)` según el idioma activo.
 */

fun loanStatusLabel(status: String?): String = when (status?.trim()?.lowercase()) {
    "active" -> "Activo"
    "late", "overdue" -> "En mora"
    "paid", "completed" -> "Pagado"
    "pending", "pending_approval" -> "Pendiente"
    "refinanced" -> "Refinanciado"
    "legal" -> "Legal"
    "written_off" -> "Castigado"
    "rejected" -> "Rechazado"
    "cancelled", "canceled" -> "Cancelado"
    else -> status?.replaceFirstChar { it.uppercase() }.orEmpty()
}

fun installmentStatusLabel(status: String?): String = when (status?.trim()?.lowercase()) {
    "pending" -> "Pendiente"
    "partial" -> "Pago parcial"
    "late", "overdue" -> "Atrasada"
    "paid" -> "Pagada"
    "cancelled", "canceled" -> "Cancelada"
    else -> status?.replaceFirstChar { it.uppercase() }.orEmpty()
}

fun clientStatusLabel(status: String?): String = when (status?.trim()?.lowercase()) {
    "active" -> "Activo"
    "inactive" -> "Inactivo"
    "blocked" -> "Bloqueado"
    "late" -> "En mora"
    else -> status?.replaceFirstChar { it.uppercase() }.orEmpty()
}

fun paymentStatusLabel(status: String?): String = when (status?.trim()?.lowercase()) {
    "valid", "", null -> "Válido"
    "cancelled", "canceled" -> "Anulado"
    "pending" -> "Pendiente"
    else -> status?.replaceFirstChar { it.uppercase() }.orEmpty()
}

fun commissionStatusLabel(status: String?): String = when (status?.trim()?.lowercase()) {
    "pending", "", null -> "Pendiente"
    "paid" -> "Pagada"
    "cancelled", "canceled" -> "Anulada"
    else -> status?.replaceFirstChar { it.uppercase() }.orEmpty()
}

fun contractStatusLabel(status: String?): String = when (status?.trim()?.lowercase()) {
    "draft" -> "Borrador"
    "generated" -> "Generado"
    "sent" -> "Enviado"
    "viewed" -> "Visto"
    "signed" -> "Firmado"
    "rejected" -> "Rechazado"
    "cancelled", "canceled" -> "Anulado"
    "expired" -> "Vencido"
    else -> status?.replaceFirstChar { it.uppercase() }.orEmpty()
}
