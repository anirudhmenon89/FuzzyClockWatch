package cloudnine.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.Gravity;
import android.view.SurfaceHolder;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by DELL on 12/25/2014.
 */
public class TextWatchFace extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    private class Engine extends CanvasWatchFaceService.Engine {

        private final int MESSAGE_UPDATE_TIME = 0;
        private final int LINE_GAP = 13;
        private final int TEXT_SIZE = 50;

        private final long INTERACTIVE_MODE_UPDATE_RATE = TimeUnit.MINUTES.toMillis(1);

        private final String TAG = "TextWatchFace.java";
        private final String BACKGROUND_COLOUR = "#38433f";
        private final String TEXT_COLOUR = "#FFFFFF";
        private final String NEW_LINE = "\n";
        private final String[] SINGLE_DIGITS = {
            "o'", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"
        };
        private final String[] DOUBLE_DIGITS_TEENS = {
            "ten","eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
            "eighteen", "nineteen",
        };
        private final String[] DOUBLE_DIGITS_TENS = {
            "twenty", "thirty", "forty", "fifty", "o' clock"
        };

        private boolean mbRegisteredTimeZoneReceiver = false;
        private boolean mbLowBitAmbient;
        private boolean mbBurnInProtection;
        private boolean mbAmbientForce = false;

        private Typeface mtfMedium;
        private Typeface mtfLight;

        private Bitmap mbitBackground;
        private Bitmap mbitBackgroundScaled = null;

        private Paint mPaintTextHours;

        private WatchFaceStyle.Builder mwfBuilder;

        private Time mTime;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            initViews();
            setFont();

            mwfBuilder.setCardPeekMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT);
            mwfBuilder.setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE);
            mwfBuilder.setHotwordIndicatorGravity(Gravity.LEFT | Gravity.TOP);
            mwfBuilder.setStatusBarGravity(Gravity.LEFT | Gravity.TOP);
            setWatchFaceStyle(mwfBuilder.build());

            mPaintTextHours.setAntiAlias(true);
            mPaintTextHours.setTextAlign(Paint.Align.LEFT);
            mPaintTextHours.setColor(Color.parseColor(TEXT_COLOUR));
            mPaintTextHours.setTextSize(TEXT_SIZE);

            /* Load background gradient */
            Resources resources = TextWatchFace.this.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.blackandwhitegradient);
            mbitBackground = ((BitmapDrawable) backgroundDrawable).getBitmap();
        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            mTime.setToNow();

            int x = 60;
            int y = 0;

            if(mbitBackgroundScaled == null) {
                mbitBackgroundScaled = Bitmap.createScaledBitmap(mbitBackground,
                        bounds.width(), bounds.height(), true);
            }
            canvas.drawBitmap(mbitBackgroundScaled, 0, 0, null);

            String time = getHours(mTime.hour) + NEW_LINE + getMins(mTime.minute);
            String[] splitTime = time.split(NEW_LINE);

            if(splitTime.length == 2) {
                y = bounds.height() - 140 ;
                y = (int) (y - ((mPaintTextHours.descent() + mPaintTextHours.ascent()) / 2));
            } else {
                y = bounds.height() - 110 ;
                y = (int) (y - ((mPaintTextHours.descent() + mPaintTextHours.ascent()) / 2));
            }

            for(int i = splitTime.length - 1; i>=0; i--) {
                if (i == 0) {
                    mPaintTextHours.setTypeface(mtfMedium);
                } else {
                    mPaintTextHours.setTypeface(mtfLight);
                }
                canvas.drawText(splitTime[i], x, y, mPaintTextHours);
                y = (int) (y + (mPaintTextHours.ascent() + mPaintTextHours.descent())) - LINE_GAP;
            }
        }


        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            mbLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mbBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }


        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }


        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            /* Hack to show alias in LG G watch since it does not have low bit and
             * burn in protection */
            if(inAmbientMode && (mbLowBitAmbient || mbBurnInProtection || mbAmbientForce)) {
                mPaintTextHours.setAntiAlias(false);
                mPaintTextHours.setStyle(Paint.Style.STROKE);
            } else {
                mPaintTextHours.setAntiAlias(true);
                mPaintTextHours.setStyle(Paint.Style.FILL_AND_STROKE);
            }

            updateTimer();
            invalidate();
        }


        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if(visible) {
                registerReceiver();

                /* Update timezone */
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
                invalidate();
            } else {
                unRegisterReceiver();
            }

            updateTimer();
        }

        private void initViews() {
            mTime = new Time();

            mwfBuilder = new WatchFaceStyle.Builder(TextWatchFace.this);
            mPaintTextHours = new Paint();
        }


        private void setFont() {
            mtfLight = Typeface.createFromAsset(getAssets(), "light.ttf");
            mtfMedium = Typeface.createFromAsset(getAssets(), "medium.ttf");
        }


        private boolean isTimerRunning() {
            return isVisible() && !isInAmbientMode();
        }


        private void updateTimer() {
            updateTimeHandler.removeCallbacksAndMessages(MESSAGE_UPDATE_TIME);
            if(isTimerRunning()) {
                updateTimeHandler.sendEmptyMessage(MESSAGE_UPDATE_TIME);
            }
        }


        private String getHours(int hour) {

            if(hour > 12) {
                hour = hour - 12;
            }

            if(hour == 0 || hour == 12) {
                //Return Twelve
                return DOUBLE_DIGITS_TEENS[2];
            }

            if(hour > 0 && hour <= 9) {
                return SINGLE_DIGITS[hour];
            }

            if(hour >= 10) {
                int secondDigit = hour % 10;
                return DOUBLE_DIGITS_TEENS[secondDigit];
            }

            return "";
        }


        private String getMins(int minutes) {

            if(minutes == 0) {
                return "o'"+ NEW_LINE +"clock";
            }

            if(minutes > 0 && minutes < 10) {
                return "o'"+ NEW_LINE + SINGLE_DIGITS[minutes];
            }

            if (minutes >= 10 && minutes <= 19) {
                int secondNumber = minutes % 10;
                return DOUBLE_DIGITS_TEENS[secondNumber];
            }

            if(minutes > 19) {

                int firstNumber = (minutes / 10) - 2;
                int secondNumber = (minutes % 10);

                String text = DOUBLE_DIGITS_TENS[firstNumber];

                if(secondNumber > 0)
                    text =  text + NEW_LINE + SINGLE_DIGITS[secondNumber];
                return  text;
            }

            return "";
        }


        private void registerReceiver() {

            if(!mbRegisteredTimeZoneReceiver) {

                mbRegisteredTimeZoneReceiver = true;
                IntentFilter iFilter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
                TextWatchFace.this.registerReceiver(timeZoneReceiver, iFilter);
            }
        }


        private void unRegisterReceiver() {
            if(mbRegisteredTimeZoneReceiver) {
                mbRegisteredTimeZoneReceiver = false;
                TextWatchFace.this.unregisterReceiver(timeZoneReceiver);
            }
        }


        /* Broadcast receiver if time zone is changed */
        BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        /* Handler to update time once a minute */
        final Handler updateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if(msg.what == MESSAGE_UPDATE_TIME) {
                    invalidate();

                    if(isTimerRunning()) {
                        long timeMs = System.currentTimeMillis();
                        long delayMs = INTERACTIVE_MODE_UPDATE_RATE -
                                (timeMs % INTERACTIVE_MODE_UPDATE_RATE);
                        updateTimeHandler.sendEmptyMessageDelayed(MESSAGE_UPDATE_TIME, delayMs);
                    }
                }
            }
        };
    }
}
