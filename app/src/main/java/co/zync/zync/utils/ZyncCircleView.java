package co.zync.zync.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import java.util.TimerTask;

public class ZyncCircleView extends View {
    private static final float COLOR_FACTOR_BOUND = 0.15f;
    private static int RECT_ADD_BOUND = 5; // dp
    private int color = Color.rgb(71, 224, 20);
    private int radius = -1;
    private Paint paint;

    public ZyncCircleView(Context context) {
        super(context);
        init();
    }

    public ZyncCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ZyncCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ZyncCircleView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(new LinearGradient(
                0, 0, 0,
                getHeight(),
                modify(color, COLOR_FACTOR_BOUND * -1),
                modify(color, COLOR_FACTOR_BOUND),
                Shader.TileMode.CLAMP
        ));
    }

    private void defineRadius() {
        int w = getLayoutParams().width;
        int h = getLayoutParams().height;

        int pl = getPaddingLeft();
        int pr = getPaddingRight();
        int pt = getPaddingTop();
        int pb = getPaddingBottom();

        int usableWidth = w - (pl + pr);
        int usableHeight = h - (pt + pb);
        radius = Math.min(usableWidth, usableHeight) / 2;
    }

    public int color() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (radius == -1) {
            defineRadius();
        }

        canvas.drawCircle(getWidth()/2, getHeight()/2, radius, paint);
    }

    private int convertDpToPixel(float dp) {
        Resources resources = getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) Math.floor(dp * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private float convertPixelsToDp(int px) {
        Resources resources = getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    private static int modify(int color, float fraction) {
        int red = modifyColor(Color.red(color), fraction);
        int green = modifyColor(Color.green(color), fraction);
        int blue = modifyColor(Color.blue(color), fraction);
        int alpha = modifyColor(Color.alpha(color), fraction / 5);

        return Color.argb(alpha, red, green, blue);
    }

    // if fraction is negative, color will be lightened
    // if fraction is positive, color will be darkened

    private static int modifyColor(int color, float fraction) {
        int modified = (int) (color - (color * fraction));
        return fraction < 0 ? Math.min(modified, 255) : Math.max(modified, 0);
    }

    public static class SizeChangeTask extends TimerTask {
        private Activity activity;
        private ZyncCircleView view;
        private float dpChangePerFrame;
        private float maxSize;
        private float lastSize;
        private boolean dpAddition = true;

        public SizeChangeTask(Activity activity, ZyncCircleView view, int time) {
            this.activity = activity;
            this.view = view;
            this.dpChangePerFrame = (float) RECT_ADD_BOUND / (float) (time * 5);
            this.lastSize = 180;
            this.maxSize = lastSize + RECT_ADD_BOUND;
        }

        @Override
        public void run() {
            // size start
            float newSize = dpAddition ?
                    lastSize + dpChangePerFrame :
                    lastSize - dpChangePerFrame;
            float originalSize = maxSize - RECT_ADD_BOUND;

            if (newSize >= maxSize) {
                newSize = maxSize;
                dpAddition = false;
            }

            if (newSize <= originalSize) {
                newSize = originalSize;
                dpAddition = true;
            }

            lastSize = newSize;
            // size end

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (view.radius != lastSize) {
                        view.radius = view.convertDpToPixel(lastSize);
                        view.invalidate();
                    }
                }
            });
        }
    }

    public static class ColorChangeTask extends TimerTask {
        private ZyncCircleView view;
        private float currentFactor;
        private float colorChangePerFrame;
        private boolean colorSubtraction = true;


        public ColorChangeTask(ZyncCircleView view, int time) {
            this.view = view;
            this.currentFactor = COLOR_FACTOR_BOUND;
            this.colorChangePerFrame = (currentFactor * 2) / (time * 20);
        }

        @Override
        public void run() {
            this.currentFactor = colorSubtraction ?
                    currentFactor - colorChangePerFrame :
                    currentFactor + colorChangePerFrame;

            if (currentFactor <= -COLOR_FACTOR_BOUND) {
                colorSubtraction = false;
            }

            if (currentFactor >= COLOR_FACTOR_BOUND) {
                currentFactor = COLOR_FACTOR_BOUND;
                colorSubtraction = true;
            }

            view.paint.setShader(new LinearGradient(
                    0, 0, 0,
                    view.radius,
                    modify(view.color, currentFactor * -1),
                    modify(view.color, currentFactor),
                    Shader.TileMode.CLAMP
            ));

            view.postInvalidate();
        }
    }
}
