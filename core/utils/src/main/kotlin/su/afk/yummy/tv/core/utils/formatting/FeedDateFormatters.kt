package su.afk.yummy.tv.core.utils.formatting

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Formato de fecha único para los feeds (reseñas, publicaciones, vídeos de blogueros): «día mes, horas:minutos».
 * La entrada es tiempo Unix en segundos. Fuente única para no duplicar [java.text.DateFormat] en cada feature.
 */
fun Long.formatFeedDateTime(): String =
    SimpleDateFormat("d MMMM, HH:mm", Locale.getDefault()).format(Date(this * 1_000L))

/**
 * Fecha de estreno del episodio para las tarjetas: «3 oct» o «3 oct, 17:30» si se conoce la hora.
 * El mes es abreviado: la etiqueta vive en una tarjeta estrecha. La entrada es tiempo Unix en segundos.
 *
 * La medianoche exacta en UTC significa que la fuente solo conoce el día: en yani la mitad de las fechas
 * del calendario llegan así. Esas fechas se formatean en UTC: en zonas al oeste de Greenwich la medianoche
 * cae en el día anterior y no se puede mostrar la hora, parecería real (03:00 para MSK).
 */
fun Long.formatAirDate(): String {
    val dayOnly = this % SECONDS_IN_DAY == 0L
    val format = SimpleDateFormat(if (dayOnly) "d MMM" else "d MMM, HH:mm", Locale.getDefault())
    if (dayOnly) format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date(this * 1_000L))
}

private const val SECONDS_IN_DAY = 24L * 60L * 60L

/**
 * Fecha completa del comentario: «20 nov 2024 a las 18:30» para es,
 * «20 нояб. 2024 в 18:30» para ru y «20 Nov 2024, 18:30» para el resto de locales.
 * La entrada es tiempo Unix en segundos.
 */
fun Long.formatCommentDateTime(): String {
    val locale = Locale.getDefault()
    val pattern = when (locale.language) {
        "ru" -> "d MMM yyyy 'в' HH:mm"
        "es" -> "d MMM yyyy 'a las' HH:mm"
        else -> "d MMM yyyy, HH:mm"
    }
    return SimpleDateFormat(pattern, locale).format(Date(this * 1_000L))
}
