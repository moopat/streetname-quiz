package at.trycatch.streets.util

import at.trycatch.streets.R

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class MessageUtil {

    fun getRandomErrorMessage(): Int {
        return listOf(
                R.string.quiz_result_failed_1,
                R.string.quiz_result_failed_2,
                R.string.quiz_result_failed_3,
                R.string.quiz_result_failed_4
        ).shuffled().first()
    }

    fun getRandomSuccessMessage(): Int {
        return listOf(
                R.string.quiz_result_success_1,
                R.string.quiz_result_success_2,
                R.string.quiz_result_success_3,
                R.string.quiz_result_success_4,
                R.string.quiz_result_success_5
        ).shuffled().first()
    }

}
