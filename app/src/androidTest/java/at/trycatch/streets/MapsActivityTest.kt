package at.trycatch.streets

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import android.util.Log
import kotlinx.android.synthetic.main.activity_maps.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Markus Deutsch <markus@moop.at>
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MapsActivityTest {

    @get:Rule
    var mActivityRule: ActivityTestRule<MapsActivity> = ActivityTestRule(MapsActivity::class.java)

    @Test
    fun testIteratingAllStreets() {
        onView(withId(R.id.btnStart)).perform(click())
        onView(withId(R.id.btnSolution)).check(matches(withText("Lösung")))

        sleep(5000) // Initial wait.

        mActivityRule.activity.model.testMode = true
        mActivityRule.activity.model.startNewRound()

        val firstStreet = mActivityRule.activity.tvStreetName.text
        var previousStreet = "not this one"

        while (firstStreet != previousStreet) {
            Log.d("MapsActivityTest", "Testing " + mActivityRule.activity.tvStreetName.text + "...")
            onView(withId(R.id.btnSolution)).perform(click())
            //sleep(800)
            onView(withId(R.id.btnNext)).perform(click())
            previousStreet = mActivityRule.activity.tvStreetName.text.toString()
        }
    }

    /**
     * Working around some Espresso bugs...
     * https://code.google.com/p/android-test-kit/issues/detail?id=79
     * https://code.google.com/p/android-test-kit/issues/detail?id=44
     */
    private fun sleep(duration: Long) {
        try {
            Thread.sleep(duration)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

}