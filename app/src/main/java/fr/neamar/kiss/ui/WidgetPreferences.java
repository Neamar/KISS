package fr.neamar.kiss.ui;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;


/**
 * Created by TBog on 5/10/2018.
 */
public class WidgetPreferences implements Serializable {
    public int position = WidgetLayout.LayoutParams.POSITION_MIDDLE;
    public int width = 0;
    public int height = 0;
    public int offsetTop = 0;

    public void showEditMenu(MainActivity mainActivity, SharedPreferences widgetPrefs, AppWidgetHostView hostView) {
        Menu menu = new Menu(mainActivity, widgetPrefs);
        Point windowSize = new Point();
        mainActivity.getWindowManager()
                .getDefaultDisplay()
                .getSize(windowSize);
        menu.setScreenSize(windowSize);
        menu.show(mainActivity, this, hostView);
        mainActivity.registerPopup(menu);
    }

    public static String serialize(WidgetPreferences o) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(o);
            objectOutputStream.close();
        } catch (IOException e) {
            Log.e("Widget", "Serialize WidgetPreferences", e);
        }
        try {
            return byteArrayOutputStream.toString("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public static WidgetPreferences unserialize(String data) {
        if (data == null || data.isEmpty())
            return null;
        ObjectInputStream objectInputStream;
        try {
            objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data.getBytes("ISO-8859-1")));
            return (WidgetPreferences) objectInputStream.readObject();
        } catch (Exception e) {
            Log.e("Widget", "UnSerialize WidgetPreferences", e);
            return null;
        }
    }

    static class Menu extends PopupWindow {
        private final Point mWindowSize = new Point(1, 1);
        private final SharedPreferences prefs;

        public Menu(Context context, SharedPreferences widgetPrefs) {
            super(context, null, android.R.attr.popupMenuStyle);
            prefs = widgetPrefs;
            ScrollView scrollView = new ScrollView(context);
            LayoutInflater.from(context).inflate(R.layout.widget_customize, scrollView);
            setContentView(scrollView);
            setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
            setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        public void show(final MainActivity mainActivity, final WidgetPreferences widgetPreferences, final AppWidgetHostView hostView) {
            final View contentView = getContentView();
            final AppWidgetProviderInfo info = hostView.getAppWidgetInfo();

            contentView.findViewById(R.id.btn_apply).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                    SeekBar seek;
                    //Width
                    seek = contentView.findViewById(R.id.seek_width);
                    widgetPreferences.width = seek.getProgress() + info.minWidth;
                    //Height
                    seek = contentView.findViewById(R.id.seek_height);
                    widgetPreferences.height = seek.getProgress() + info.minHeight;
                    int appWidgetId = hostView.getAppWidgetId();
                    prefs.edit().putString(String.valueOf(appWidgetId), serialize(widgetPreferences)).apply();
                    mainActivity.refreshWidget(appWidgetId);
                }
            });
            contentView.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
            SeekBar seek;
            TextView text;

            //Width
            text = contentView.findViewById(R.id.value_width);
            seek = contentView.findViewById(R.id.seek_width);
            seek.setProgress(widgetPreferences.width - info.minWidth);
            seek.setMax(mWindowSize.x - info.minWidth);
            seek.setOnSeekBarChangeListener(new SeekBarSync(text, info.minWidth));

            //Height
            text = contentView.findViewById(R.id.value_height);
            seek = contentView.findViewById(R.id.seek_height);
            seek.setProgress(widgetPreferences.height - info.minHeight);
            seek.setMax(mWindowSize.y - info.minHeight);
            seek.setOnSeekBarChangeListener(new SeekBarSync(text, info.minHeight));

            showAtLocation(mainActivity.emptyListView, Gravity.CENTER, 0, 0);
        }

        void setScreenSize(Point size) {
            mWindowSize.x = size.x;
            mWindowSize.y = size.y;
        }

        static class SeekBarSync implements SeekBar.OnSeekBarChangeListener {
            final TextView mTextView;
            final int mMin;

            SeekBarSync(TextView text, int min) {
                mTextView = text;
                mMin = min;
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTextView.setText(String.valueOf(progress + mMin));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        }
    }
}
