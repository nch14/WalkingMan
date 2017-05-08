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
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import cn.chenhaonee.walkingman.activity.MainActivity;
import cn.chenhaonee.walkingman.me.MyInfo;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by chenhaonee on 2017/5/8.
 */

public class MyStepService extends Service implements SensorEventListener {
    private int stepSensorType = -1;

    private PresureSensorEventListener presureSensorEventListener;

    private static Handler activityHandler;

    private int currentStep = 0;
    //历史数据-00:00以前的步数
    private int historyStep = 0;

    private Map<Timestamp, Double> heights;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        heights = new HashMap<>();
        presureSensorEventListener = new PresureSensorEventListener();
        scheduleJobs();
        startStepDetector();
        addCountStepListener();
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

    /**
     * 添加传感器监听
     */
    private void addCountStepListener() {
        //TYPE_STEP_COUNTER 计步传感器，用于记录激活后的步伐数。
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        //TYPE_STEP_DETECTOR 步行检测传感器，用户每走一步就触发一次事件。
        Sensor detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        Sensor pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        if (sensorManager != null)
            sensorManager.registerListener(presureSensorEventListener, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (countSensor != null) {
            stepSensorType = 0;
            //SensorManager.SENSOR_DELAY_NORMAL 频率：30hz——45Hz之间 一般：38Hz
            sensorManager.registerListener(MyStepService.this, countSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else if (detectorSensor != null) {
            stepSensorType = 1;
            sensorManager.registerListener(MyStepService.this, detectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // TODO: 2017/5/8 使用陀螺仪进行计步
//            addBasePedoListener();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (stepSensorType == 0) {
            int totalSteps = (int) sensorEvent.values[0];
            currentStep = totalSteps > historyStep ? totalSteps - historyStep : totalSteps;
        } else if (stepSensorType == 1) {
            if (sensorEvent.values[0] == 1.0) {
                currentStep++;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void scheduleJobs(){
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(3);
        Calendar calendar = Calendar.getInstance();
        Date tomorrow = calendar.getTime();
        tomorrow.setDate(tomorrow.getDate() + 1);
        pool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                activityHandler.sendMessage(activityHandler.obtainMessage(1, currentStep));
            }
        }, 0, 5, TimeUnit.SECONDS);

        long next = (tomorrow.getTime() - calendar.getTime().getTime());
        pool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                //todo 24小时备份步数
                historyStep = currentStep;
                //postDaliyDataToServer();
            }
        }, next, 24 * 3600 * 1000, TimeUnit.MILLISECONDS);
        pool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                //todo 海拔计算

            }
        }, next, 24 * 3600 * 1000, TimeUnit.MILLISECONDS);
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

    class PresureSensorEventListener implements SensorEventListener {
        //http://www.cnblogs.com/HackingProgramer/p/4018114.html
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float sPV = sensorEvent.values[0];

            DecimalFormat df = new DecimalFormat("0.00");
            df.getRoundingMode();
            // 计算海拔
            double height = 44330000 * (1 - (Math.pow((Double.parseDouble(df.format(sPV)) / 1013.25),
                    (float) 1.0 / 5255.0)));
            heights.put(new Timestamp(new Date().getTime()), height);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }

}
