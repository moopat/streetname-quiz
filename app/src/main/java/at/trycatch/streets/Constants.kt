package at.trycatch.streets

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class Constants {

    class Events {

        companion object {

            const val STREET_SKIPPED = "street_skipped"
            const val STREET_SOLUTION = "street_shown_solution"
            const val STREET_SOLVED = "street_solved"

            const val ANSWERED_CORRECTLY = "answered_correctly"
            const val ANSWERED_WRONGLY = "answered_wrongly"

            const val DISTRICT_PICKED = "district_picked"
            const val DISTRICT_PICKED_ARG_NAME = "district_name"

            const val TERMS_ACCEPT = "terms_accepted"
            const val TERMS_DECLINED = "terms_declined"

        }

    }

    class Broadcasts {

        companion object {

            const val ACTION_UPDATE_DONE = "at.trycatch.streets.ACTION_UPDATE_DONE"

        }

    }

}