package co.zync.android.utils;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import co.zync.android.R;

import java.text.NumberFormat;

public class ZyncSliderPreference extends DialogPreference {
    private int value = 10;

    public ZyncSliderPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public ZyncSliderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ZyncSliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ZyncSliderPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setDialogLayoutResource(R.layout.slider_layout);
    }

    @Override
    protected View onCreateDialogView() {
        value = getPersistedInt(10);
        final View view = super.onCreateDialogView();
        SeekBar bar = (SeekBar) view.findViewById(R.id.slider_seekbar);
        bar.setMax(100);
        bar.setProgress(value);
        updateText(view);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    value = progress;
                    updateText(view);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        return view;
    }

    private void updateText(View view) {
        ((TextView) view.findViewById(R.id.ms_slider_text))
                .setText(view.getContext().getString(
                        R.string.max_size_number,
                        NumberFormat.getInstance().format(value)));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && callChangeListener(value)) {
            setValue(value);
            notifyChanged();
        }
    }

    public int value() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
        getEditor().putInt(getKey(), value).apply();
    }
}
