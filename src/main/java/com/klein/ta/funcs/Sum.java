package com.klein.ta.funcs;

import com.klein.screener.strategy.Series;
import org.joda.time.LocalDateTime;

public class Sum extends AbstractTaFunction {

    @FunctionInput(required = true)
    protected AbstractTaFunction base1;
    @FunctionInput(required = true)
    protected AbstractTaFunction base2;

    public Sum(Series _s) {
        super(_s);
    }


    @Override
    protected String getKey() {
        return "sum_" + base1.getKey() + "_" + base2.getKey();
    }

    @Override
    protected double[] calc() {
        Series s1 = base1.getSeries();
        Series series2 = base2.getSeries();
        double[] data = new double[s1.getLastIndex() + 1];

        int index1 = 0;
        int index2 = 0;
        Double lastValue = null;
        while (index1 <= s1.getLastIndex() && index2 <= series2.getLastIndex()) {
            LocalDateTime date1 = s1.getDate()[index1];
            LocalDateTime date2 = series2.getDate()[index2];
            if (date1.isAfter(date2)) {
                index2++;
                continue;
            }
            if (date1.isBefore(date2)) {
                data[index1] = lastValue != null ? lastValue : 0D;
                index1++;
                continue;
            }

            double val1 = base1.values()[index1];
            double val2 = base2.values()[index2];
            double osziValue = val1 + val2;

            if (lastValue == null) {
                lastValue = osziValue;
            }
            data[index1] = osziValue;

            lastValue = osziValue;

            index1++;
            index2++;
        }
        if (!s1.lastDate().isEqual(series2.lastDate()))
            logError("Second series not the same last date: expected [{}], actual [{}]", s1.lastDate(), series2.lastDate());
//		System.out.println("Spread: " + Arrays.toString(data));
        return data;
    }

    @Override
    public String getShortName() {
        return "Sum";
    }


    @Override
    public String getName() {
        return getShortName() + " " + base1.getName() + "+" + base2.getName();
    }

    @Override
    public int getLoookbackBars() {
        return 0;
    }

    public AbstractTaFunction getBase1() {
        return base1;
    }

    public void setBase1(AbstractTaFunction base1) {
        this.base1 = base1;
    }

    public AbstractTaFunction getBase2() {
        return base2;
    }

    public void setBase2(AbstractTaFunction base2) {
        this.base2 = base2;
    }
}
