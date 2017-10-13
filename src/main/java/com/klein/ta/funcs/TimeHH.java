package com.klein.ta.funcs;

import com.klein.screener.strategy.Series;
import org.joda.time.LocalDateTime;

import java.util.Date;

public class TimeHH extends AbstractTaFunction {

    public TimeHH(Series _s) {
        super(_s);
    }

    @Override
    protected String getKey() {
        return "time_hh";
    }

    @Override
    public String getShortName() {
        return "HourOfDay";
    }

    @Override
    public String getName() {
        return "Hour of Day";
    }

    @Override
    protected double[] calc() {
        LocalDateTime[] values = getSeries().getDate();

        double[] roc = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            roc[i] = new Double(values[i].getHourOfDay());
        }
        return roc;
    }

}