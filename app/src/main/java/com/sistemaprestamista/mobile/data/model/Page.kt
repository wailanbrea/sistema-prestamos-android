package com.sistemaprestamista.mobile.data.model

/**
 * Una página de resultados de un listado paginado del backend, junto con la metadata
 * necesaria para saber si quedan más páginas (carga incremental "cargar más").
 */
data class Page<T>(
    val items: List<T>,
    val currentPage: Int,
    val lastPage: Int,
) {
    val hasMore: Boolean get() = currentPage < lastPage
}
