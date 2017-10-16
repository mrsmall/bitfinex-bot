package com.klein.ta.funcs;

import com.klein.ta.Series;

public class FutureMaxPlLong extends AbstractTaFunction {

    @FunctionInput
    @DefaultIntValue(1)
    protected int period = 1;

    public FutureMaxPlLong(int period, Series _s) {
        this(_s);
        this.period = period;
    }

    public FutureMaxPlLong(Series _s) {
        super(_s);
    }

    @Override
    protected String getKey() {
        return "fut_maxpl_long_" + period;
    }

    @Override
    public int getLoookbackBars() {
        return period;
    }

    @Override
    public String getShortName() {
        return "Fut. max PL long";
    }

    @Override
    public String getName() {
        return "Fut. max PL long " + getSeries().getSymbol() + " " + period;
    }

    @Override
    protected double[] calc() {
        double[] values = getSeries().getClose();

        double[] roc = new double[values.length];
        roc[0] = 0;
        for (int i = 0; i < values.length; i++) {
//            System.out.println("i: " + i);
            if (i > values.length - period - 1)
                roc[i] = 0;
            else {
                double max = Double.MIN_VALUE;
                for (int n = 0; n < period; n++)
                    max = Math.max(max, values[i + n]);
                roc[i] = (max - values[i]) * 100 / values[i];
            }
//            System.out.println("ROC["+i+"]: "+roc[i]);
        }
        return roc;
    }

    public void setPeriod(int period) {
        this.period = period;
    }
}
