package com.github.vgoliveira.panificadora;

import java.io.Serializable;
import java.util.Calendar;

/**
 * Created by vgarcia on 12/09/2016.
 */
// This class must treat the specificities of the current bakery hardware
public class Bakery implements Serializable {

    private final boolean[] TIMERAVAILABILITY = {true, true, true, true, true, false, false, true, true, true, true, true};
    private final boolean[] COLORAVAILABILITY = {true, true, true, true, true, true, true, false, false, false, false, false}; //when available default always set as medium
    private final boolean[] WEIGHTAVAILABILITY = {true, true, true, false, true, false, false, false, false, false, true, false}; //when available default always set as 2 (W900/W1200)
    private final int[] HOUR = {3, 3, 3, 1, 2, 0, 0, 1, 1, 2, 3, 1};
    private final int[] HOURWEIGHTDISCOUNTED = {2, 3, 3, 1, 2, 0, 0, 1, 1, 2, 2, 1};
    private final int[] MINUTE = {0, 50, 40, 40, 55, 58, 58, 30, 20, 50, 0, 0};
    private final int[] MINUTESWEIGHTDISCOUNTED = {53, 40, 32, 40, 50, 58, 58, 30, 20, 50, 55, 0};
    public final int COLORLIGHT = 1;
    public final int COLORMEDIUM = 2;
    public final int COLORDARK = 3;
    public final int TIMERNOTAVAILABLE = 0;
    public final int TIMEOK = 1;
    public final int TIMETOOSHORT = 2;
    public final int TIMETOOLONG = 3;
    private final int HOURMAX = 11; //it is actually 13 but 11 makes the algorithm easier and safe
    private final int TIMERSTEP = 10;

    private static final int OPERATION = 0x00;
    private static final int NO_OPERATION = 0x00;
    private static final int COMMAND = 0x01;
    private static final int PROGRAM = 0x02;
    private static final int PRESSED = 0x01;
    private static final int NOT_PRESSED = 0x00;
    private static final int TIME_MORE = 0x04;
    private static final int TIME_LESS = 0x05;
    private static final int DOUGH_QNT = 0x02;
    private static final int INIT_STOP = 0x06;
    private static final int OPTIONS = 0x01;
    private static final int COLOR = 0x03;

    private int option;
    private int selectedWeight;
    private int selectedColor;
    private int hour;
    private int minute;
    private int minHour;
    private int maxHour;
    private int minMinute;
    private int maxMinute;
    private boolean isTimerChecked;

    Bakery(String option, int selectedWeight) {
        int opt = Integer.valueOf(option);
        if (opt >= 1 && opt <= 12) {
            this.option = opt - 1;
        } else {
            this.option = 0;
        }

        switch (selectedWeight) {
            case (Recipe.W450):
                this.selectedWeight = 1;
                break;
            case (Recipe.W600):
                this.selectedWeight = 1;
                break;
            case (Recipe.W900):
                this.selectedWeight = 2;
                break;
            case (Recipe.W1200):
                this.selectedWeight = 2;
                break;
        }
        if (isColorOptionAvailable()) {
            selectedColor = COLORMEDIUM;
        } else
            selectedColor = 0;

        isTimerChecked = false;

    }

    public boolean isColorOptionAvailable() {
        return COLORAVAILABILITY[this.option];
    }

    public boolean isTimerAvailable() {
        return TIMERAVAILABILITY[this.option];
    }

    public boolean isWeightOptionAvailable() {
        return WEIGHTAVAILABILITY[this.option];
    }

    public int getHour() {
        if (selectedWeight == 2) {
            return this.HOUR[option];
        } else if (selectedWeight == 1) {
            return this.HOURWEIGHTDISCOUNTED[option];
        }
        return -1;

    }

    public int getMinute() {
        if (selectedWeight == 2) {
            return this.MINUTE[option];
        } else if (selectedWeight == 1) {
            return this.MINUTESWEIGHTDISCOUNTED[option];
        }
        return -1;
    }

    public void setColor(int color) {
        if (isColorOptionAvailable()) {
            if ((color == COLORLIGHT) || (color == COLORMEDIUM) || (color == COLORDARK)) {
                this.selectedColor = color;
            }
        }
    }

    public int getColor() {
        return this.selectedColor;
    }

    public int setTimer(int hour, int minute) {

        if (isTimerAvailable()) {
            switch (checkTime(hour, minute)) {
                case TIMEOK:
                    this.hour = hour;
                    this.minute = minute;
                    return TIMEOK;
                case TIMETOOSHORT:
                    return TIMETOOSHORT;
                case TIMETOOLONG:
                    return TIMETOOLONG;
            }
        }
        return TIMERNOTAVAILABLE;
    }

    private int checkTime(int hour, int minute) {
        boolean sameDay = false;
        Calendar c = Calendar.getInstance();
        setMinTime();
        setMaxTime();
        if (maxHour > c.get(Calendar.HOUR_OF_DAY)) {
            sameDay = true;
        }
        if (sameDay) {
            return sameDaycheckTime(hour, minute, c.get(Calendar.HOUR_OF_DAY), maxHour, minHour);
        } else {
            //shift the hours to get rid of day change in the calculation
            int shiftedHour = addHour(hour, HOURMAX);
            int shiftedCurrentHour = addHour(c.get(Calendar.HOUR_OF_DAY), HOURMAX);
            int shiftedMinHour = addHour(minHour, HOURMAX);
            int shiftedMaxHour = addHour(maxHour, HOURMAX);
            return sameDaycheckTime(shiftedHour, minute, shiftedCurrentHour, shiftedMaxHour, shiftedMinHour);
        }
    }

    private int addHour(int hour, int add) {
        if ((hour + add) < 24) {
            return hour + add;
        } else {
            return hour + add - 24;
        }
    }

    private int sameDaycheckTime(int hour, int minute, int currentHour, int maxHour, int minHour) {
        if (hour > maxHour) {
            return TIMETOOLONG;
        } else if (hour == maxHour) {
            if (minute > maxMinute) {
                return TIMETOOLONG;
            }
        } else if (hour < minHour) {
            return TIMETOOSHORT;
        } else if (hour == minHour) {
            if (minute < minMinute) {
                return TIMETOOSHORT;
            }
        }
        return TIMEOK;
    }

    private void setMinTime() {
        Calendar c = Calendar.getInstance();
        if ((c.get(Calendar.MINUTE) + getMinute()) < 60) {
            minMinute = c.get(Calendar.MINUTE) + getMinute();
            if ((c.get(Calendar.HOUR_OF_DAY) + getHour()) < 24) {
                minHour = c.get(Calendar.HOUR_OF_DAY) + getHour();
            } else {
                minHour = c.get(Calendar.HOUR_OF_DAY) + getHour() - 24;
            }
        } else {
            minMinute = c.get(Calendar.MINUTE) + getMinute() - 60;
            if ((c.get(Calendar.HOUR_OF_DAY) + (getHour() + 1)) < 24) {
                minHour = c.get(Calendar.HOUR_OF_DAY) + (getHour() + 1);
            } else {
                minHour = c.get(Calendar.HOUR_OF_DAY) + (getHour() + 1) - 24;
            }
        }
    }

    private void setMaxTime() {
        Calendar c = Calendar.getInstance();
        maxMinute = c.get(Calendar.MINUTE);
        maxHour = addHour(c.get(Calendar.HOUR_OF_DAY), HOURMAX);
    }

    public int getMinHour() {
        setMinTime();
        return minHour;
    }

    public int getMinMinute() {
        setMinTime();
        return minMinute;
    }

    public void timerChecked(boolean status) {
        this.isTimerChecked = status;
    }

    public boolean isTimerChecked() {
        return this.isTimerChecked;
    }

    public byte[] setProgram(byte[] value) {

        value = new byte[]{PROGRAM, 0, 0, 0, 0, 0, 1}; // set program and INT/STOP
        //set options
        value[OPTIONS] = (byte) option;
        //set dough quantity
        if (isWeightOptionAvailable() && selectedWeight == 1) {
            value[DOUGH_QNT] = 1;
        }
        //set color
        if (isColorOptionAvailable()) {
            switch (getColor()) {
                case COLORDARK:
                    value[COLOR] = 1;
                    break;
                case COLORLIGHT:
                    value[COLOR] = 2;
                    break;
            }
        }
        //set timer
        if (isTimerAvailable() && isTimerChecked()) {
            value[TIME_MORE] = calculateTimerClicks();
        }

        return value;
    }

    private byte calculateTimerClicks() {
        int roundedFinishMinute;
        int roundedRecipeMinute;
        int adjustment = 0;
        int clickCount = 0;

        roundedRecipeMinute = getMinMinute();
        adjustment -= getUnit(roundedRecipeMinute);
        adjustment += getUnit(this.minute);
        roundedRecipeMinute = roundDownDecimal(getMinMinute());
        roundedFinishMinute = roundDownDecimal(this.minute);

        if(adjustment <= 5){
            clickCount--;
            if(adjustment>=5) {
                clickCount++;
            }
        }
        clickCount = clickCount + ((roundedFinishMinute - roundedRecipeMinute) / 10); //can be positive or negative

        if (this.hour >= getMinHour()) { // finishes at the same day
            clickCount = clickCount +((this.hour - getMinHour()) * 6);
        }
        else { // finishes at the next day
            int shiftedFinishHour = addHour(this.hour,HOURMAX);
            int shiftedRecipeHour = addHour(getMinHour(),HOURMAX);
            clickCount = clickCount +((shiftedFinishHour - shiftedRecipeHour) * 6);
        }

         return (byte)clickCount;
    }

    private boolean isRoundable(int num) {
        int decimal = num / 10;
        decimal = decimal * 10;
        if (decimal == num) {
            return false;
        }
        return true;
    }

    private int getUnit(int num) {
        int decimal = num / 10;
        decimal = decimal * 10;
        return num - decimal;
    }

    private int roundDownDecimal(int decimal){
        decimal =  decimal/10;
        return decimal * 10;
    }
}