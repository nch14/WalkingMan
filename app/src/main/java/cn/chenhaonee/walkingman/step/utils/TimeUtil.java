package cn.chenhaonee.walkingman.step.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by chenhaonee on 2017/5/8.
 */

public class TimeUtil {

    public static Date getNow() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date[] lastSeconds(int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        long now = calendar.getTime().getTime();
        Date[] past = new Date[n];
        for (int i = 0; i < n; i++) {
            past[i] = new Date(now);
            now -= 1000;
        }
        return past;
    }
}
