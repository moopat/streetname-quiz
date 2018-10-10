package at.trycatch.streets.animator

import android.animation.ObjectAnimator
import android.view.View
import com.daimajia.androidanimations.library.BaseViewAnimator

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class DelayedBounceAnimator : BaseViewAnimator() {

    override fun prepare(target: View?) {
        val animator = ObjectAnimator.ofFloat(target, "translationY", 0f, 0f, -30f, 0f, -15f, 0f, 0f)
        animator.startDelay = 2500
        animatorAgent.playTogether(animator)
    }

}