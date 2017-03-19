package co.zync.zync.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import java.util.TimerTask;

import static android.graphics.Color.rgb;

public class ZyncCircleView extends View {
    public static final int GREEN_OK = rgb(71, 224, 20);
    public static final int RED_ERROR = rgb(255, 40, 40);
    public static final int GRAY_OFF = rgb(145, 145, 145);
    private static final float COLOR_FACTOR_BOUND = 0.15f;
    private static int RECT_ADD_BOUND = 7; // dp
    private int color = GREEN_OK;
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

    private int size() {
        return (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.74);
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
        int w = 0;
        int h = 0;

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

    private int rectBound() {
        return (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.02);
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
        private int rectBound;
        private int dpChangePerFrame;
        private int maxSize;
        private int lastSize;
        private boolean dpAddition = true;

        public SizeChangeTask(Activity activity, ZyncCircleView view, int time) {
            this.rectBound = view.rectBound();
            this.activity = activity;
            this.view = view;
            this.dpChangePerFrame = Math.max(rectBound / (time * 5), 1);
            this.lastSize = view.size() / 2;
            this.maxSize = lastSize + rectBound;
        }

        @Override
        public void run() {
            // size start
            int newSize = dpAddition ?
                    lastSize + dpChangePerFrame :
                    lastSize - dpChangePerFrame;
            int originalSize = maxSize - rectBound;

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
                        view.radius = lastSize;
                        view.invalidate();
                    }
                }
            });
        }
    }

    public static class ColorChangeTask extends TimerTask {
        private Activity activity;
        private ZyncCircleView view;
        private float currentFactor;
        private float colorChangePerFrame;
        private boolean colorSubtraction = true;


        public ColorChangeTask(Activity activity, ZyncCircleView view, int time) {
            this.activity = activity;
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

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.paint.setShader(new LinearGradient(
                            0, 0, 0,
                            view.radius,
                            modify(view.color, currentFactor * -1),
                            modify(view.color, currentFactor),
                            Shader.TileMode.CLAMP
                    ));

                    view.postInvalidate();
                }
            });
        }
    }
}
