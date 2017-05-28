package cn.chenhaonee.walkingman.step.pojo;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by chenhaonee on 2017/5/8.
 */

public class StepRecord implements Serializable {
    private Date timestamp;
    private int count;

    public StepRecord(Date timestamp, int count) {
        this.timestamp = timestamp;
        this.count = count;
    }

    public StepRecord() {

    }

    public int getCount() {

        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Date getTimestamp() {

        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
