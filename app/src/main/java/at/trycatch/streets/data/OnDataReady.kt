package at.trycatch.streets.data

/**
 * @author Markus Deutsch <markus@moop.at>
 */
interface OnDataReady<T> {

    fun onReady(data: T)

}
