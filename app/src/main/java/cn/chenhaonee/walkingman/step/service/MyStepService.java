package cn.chenhaonee.walkingman.step.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import cn.chenhaonee.walkingman.activity.MainActivity;
import cn.chenhaonee.walkingman.me.MyInfo;
import cn.chenhaonee.walkingman.step.pojo.StepRecord;
import cn.chenhaonee.walkingman.step.utils.TimeUtil;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by chenhaonee on 2017/5/8.
 */

public class MyStepService extends Service implements SensorEventListener {
    private int stepSensorType = -1;

    private PresureSensorEventListener presureSensorEventListener;

    private CountListener countListener = new CountListener();

    private static Handler activityHandler;

    private int currentStep = 0;

    private int lastTotalStep = 0;

    private int oldStepForHeightDetect = 0;

    private Map<Date, Integer> counts;
    private Map<Date, Double> heights;

    private double totalHeight = 0;
    private double perHeight = 3.5;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        counts = new HashMap<>();
        heights = new HashMap<>();
        Executors.newFixedThreadPool(1).submit(new Runnable() {
            @Override
            public void run() {
                getToadyCount();
            }
        });
        presureSensorEventListener = new PresureSensorEventListener();
        scheduleJobs();
        startStepDetector();
    }

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    public static void myStart(Context context) {
        activityHandler = ((MainActivity) context).getUpdateHandler();
        Intent intent = new Intent(context, MyStepService.class);
        context.startService(intent);
    }

    private SensorManager sensorManager;

    /**
     * 获取传感器实例
     */
    private void startStepDetector() {
        if (sensorManager != null) {
            sensorManager = null;
        }
        // 获取传感器管理器的实例
        sensorManager = (SensorManager) this
                .getSystemService(SENSOR_SERVICE);
        int VERSION_CODES = Build.VERSION.SDK_INT;
        if (VERSION_CODES >= 19) {
            addCountStepListener();
        } else {
            // TODO: 2017/5/8

        }
    }

    Sensor countSensor;
    Sensor detectorSensor;
    Sensor pressureSensor;

    /**
     * 添加传感器监听
     */
    private void addCountStepListener() {
        //TYPE_STEP_COUNTER 计步传感器，用于记录激活后的步伐数。
        countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        //TYPE_STEP_DETECTOR 步行检测传感器，用户每走一步就触发一次事件。
        detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        if (pressureSensor != null)
            sensorManager.registerListener(presureSensorEventListener, pressureSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);

        if (countSensor != null) {
            stepSensorType = 0;
            sensorManager.registerListener(MyStepService.this, countSensor, SensorManager.SENSOR_DELAY_FASTEST);
        } else if (detectorSensor != null) {
            stepSensorType = 1;
        } else {
            // TODO: 2017/5/8 使用陀螺仪进行计步
//            addBasePedoListener();
        }
        sensorManager.registerListener(countListener, detectorSensor, SensorManager.SENSOR_DELAY_FASTEST);

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (stepSensorType == 0) {
            int totalSteps = (int) sensorEvent.values[0];
            int gapStep = 0;
            if (lastTotalStep == 0) {
                lastTotalStep = totalSteps;
                gapStep = 0;
            } else {
                if (totalSteps < lastTotalStep)
                    gapStep = totalSteps;
                else
                    gapStep = totalSteps - lastTotalStep;
            }
            Date now = TimeUtil.getNowInMinute();
            currentStep += gapStep;

            if (counts.containsKey(now))
                gapStep += counts.get(now);
            counts.put(now, gapStep);

            lastTotalStep = totalSteps;
        } else if (stepSensorType == 1) {
            if (sensorEvent.values[0] == 1.0) {
                currentStep++;
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void scheduleJobs() {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(4);
        final Calendar calendar = Calendar.getInstance();
        Date tomorrow = calendar.getTime();
        tomorrow.setDate(tomorrow.getDate() + 1);
        pool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                activityHandler.sendMessage(activityHandler.obtainMessage(1, currentStep));
                activityHandler.sendMessage(activityHandler.obtainMessage(2, totalHeight / perHeight));
            }
        }, 0, 5, TimeUnit.SECONDS);
        long next = (tomorrow.getTime() - calendar.getTime().getTime());
        pool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                currentStep = 0;
                totalHeight = 0;
                postDaliyDataToServer();
            }
        }, next, 24 * 3600 * 1000, TimeUnit.MILLISECONDS);
        pool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                int deltStep = oldStepForHeightDetect;
                oldStepForHeightDetect = currentStep;
                Date[] past = TimeUtil.lastSeconds(5);
                double[] heights = new double[5];
                for (int i = 0; i < 10; i++) {
                    heights[i] = MyStepService.this.heights.get(past[i]);
                }
                Arrays.sort(heights);
                double deltHeight = heights[9] - heights[0];
                if (deltStep < 9 && deltHeight > 2)
                    totalHeight += Math.abs(deltHeight);
                oldStepForHeightDetect = 0;
            }
        }, 0, 5, TimeUnit.SECONDS);
        long nextHour = TimeUtil.tillNextHour();
        pool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                postHourDataToServer();
            }
        }, nextHour, 3600 * 1000, TimeUnit.MILLISECONDS);
    }

    private void postDaliyDataToServer() {
        OkHttpClient client = new OkHttpClient();
        JSONObject object = new JSONObject();
        try {
            object.put("username", MyInfo.username);
            object.put("count", currentStep);
            RequestBody params = RequestBody.create(JSON, object.toString());
            Request request = new Request.Builder().url("http://").post(params).build();
            client.newCall(request).execute();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getToadyCount() {
        OkHttpClient client = new OkHttpClient();
        try {
            Request request = new Request.Builder().url("http://step.chenhaonee.cn/user/data/today").build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String value = response.body().string();
                currentStep = Integer.parseInt(value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void postHourDataToServer() {
        Date[] past = TimeUtil.lastMinutes(60);
        List<StepRecord> records = new ArrayList<>();
        for (Date d : past) {
            if (counts.containsKey(d)) {
                records.add(new StepRecord(d, counts.get(d)));
                counts.remove(d);
            }
        }

        OkHttpClient client = new OkHttpClient();
        JSONObject object = new JSONObject();
        try {
            object.put("username", MyInfo.username);
            object.put("records", records);
            RequestBody params = RequestBody.create(JSON, object.toString());
            Request request = new Request.Builder().url("http://step.chenhaonee.cn/user/data/hour").post(params).build();
            client.newCall(request).execute();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class CountListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.values[0] == 1.0) {
                oldStepForHeightDetect++;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    class PresureSensorEventListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float sPV = sensorEvent.values[0];

            DecimalFormat df = new DecimalFormat("0.00");
            //df.getRoundingMode();
            double height = 44330000 * (1 - (Math.pow((Double.parseDouble(df.format(sPV)) / 1013.25),
                    (float) 1.0 / 5255.0)));
            heights.put(TimeUtil.getNowInSecond(), height);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this, countSensor);
        sensorManager.unregisterListener(this, detectorSensor);
        sensorManager.unregisterListener(presureSensorEventListener, pressureSensor);
    }
}
