package cn.chenhaonee.walkingman.step.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by chenhaonee on 2017/5/8.
 */

public class TimeUtil {

    public static Date getNowInSecond() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date getNowInMinute() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    public static Date getNowInHour() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
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

    public static Date[] lastMinutes(int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        long now = calendar.getTime().getTime();
        Date[] past = new Date[n];
        for (int i = 0; i < n; i++) {
            past[i] = new Date(now);
            now -= 1000 * 60;
        }
        return past;
    }

    public static long tillNextHour() {
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        if (now.getTime() % (3600 * 1000) == 0)
            return 0L;
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        return calendar.getTime().getTime() + 3600 * 1000 - now.getTime();
    }

}
