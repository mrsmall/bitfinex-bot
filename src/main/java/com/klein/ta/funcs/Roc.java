package com.klein.ta.funcs;

import com.klein.ta.Series;

import java.util.Arrays;

public class Roc extends AbstractTaFunction {

    @FunctionInput
    @DefaultIntValue(1)
    protected int period = 1;
    @FunctionInput
    protected AbstractTaFunction base;

    public Roc(int period, AbstractTaFunction base, Series _s) {
        this(period, _s);
        this.base = base;
    }

    public Roc(int period, Series _s) {
        this(_s);
        this.period = period;
    }

    public Roc(Series _s) {
        super(_s);
        this.base = new Close(_s);
    }

    @Override
    protected String getKey() {
        return "roc_" + period + "_" + base.getKey();
    }

    @Override
    public int getLoookbackBars() {
        return period + base.getLoookbackBars();
    }

    @Override
    public String getShortName() {
        return "ROC";
    }

    @Override
    public String getName() {
        return "ROC " + base.getName();
    }

    @Override
    protected double[] calc() {
//        System.out.println("Calc: " + getKey());
        double[] values = base.values();
//        printValues("Base values: ", values, 5);

        double[] oldValues = getSeries().getCalculatedValues(getKey());

        double[] roc;
        int calculateFromBar = 0;
        if (oldValues != null && oldValues.length > 1) {
            calculateFromBar = oldValues.length - 2;
            roc = Arrays.copyOf(oldValues, values.length);
        } else {
            roc = new double[values.length];
            roc[0] = 0;
        }
        
        for (int i = calculateFromBar; i < values.length; i++) {
            if (i < period)
                roc[i] = 0;
            else {
                if (values[i - period] != 0)
                    roc[i] = (values[i] - values[i - period]) / values[i - period];
                else
                    roc[i] = 0;
            }
        }
//        printValues("Calc values: ", roc, 5);
        return roc;
    }

    @Override
    public void setSeries(Series _s) {
        base.setSeries(_s);
        super.setSeries(_s);
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public AbstractTaFunction getBase() {
        return base;
    }

    public void setBase(AbstractTaFunction base) {
        this.base = base;
    }
}
