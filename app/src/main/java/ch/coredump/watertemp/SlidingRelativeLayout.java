package ch.coredump.watertemp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

/**
 * A fragment subclass that can be rolled up/down in an animation.
 *
 * Based on http://trickyandroid.com/fragments-translate-animation/
 */
public class SlidingRelativeLayout extends RelativeLayout {

    private float yFraction = 0.0f;

    public SlidingRelativeLayout(Context context) {
        super(context);
    }

    public SlidingRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SlidingRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private ViewTreeObserver.OnPreDrawListener preDrawListener = null;

    /**
     * Specify how many percent (0.0-1.0) of the view should be visible.
     *
     * @param fraction Number between 0.0 and 1.0
     */
    public void setYFraction(float fraction) {
        this.yFraction = fraction;

        if (getHeight() == 0) {
            if (preDrawListener == null) {
                preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
                        setYFraction(yFraction);
                        return true;
                    }
                };
                getViewTreeObserver().addOnPreDrawListener(preDrawListener);
            }
            return;
        }

        float translationY = getHeight() * (1.0f - fraction);
        setTranslationY(translationY);
    }

    public float getYFraction() {
        return this.yFraction;
    }

}