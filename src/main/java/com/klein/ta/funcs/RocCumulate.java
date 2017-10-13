package com.klein.ta.funcs;

import com.klein.screener.strategy.Series;

public class RocCumulate extends AbstractTaFunction {

    @FunctionInput
    @DefaultIntValue(1)
    protected int period = 1;
    private AbstractTaFunction base;

    public RocCumulate(int period, AbstractTaFunction base, Series _s) {
        this(period, _s);
        this.base = base;
    }

    public RocCumulate(int period, Series _s) {
        this(_s);
        this.period = period;
    }

    public RocCumulate(Series _s) {
        super(_s);
        this.base = new Close(_s);
    }

    @Override
    protected String getKey() {
        return "roc_cum_" + period + "_" + base.getKey();
    }

    @Override
    public String getShortName() {
        return "RocCum";
    }

    @Override
    public String getName() {
        return "RocCum " + base.getName();
    }

    @Override
    protected double[] calc() {
        double[] values = base.values();

        double[] roc = new double[values.length];
        roc[0] = 0;
        for (int i = 0; i < values.length; i++) {
            if (i < period)
                roc[i] = 0;
            else
                roc[i] = roc[i - 1] + (values[i] - values[i - period]) / values[i - period];
            System.out.println("RocCum[" + i + "]: " + roc[i]);
        }
        return roc;
    }

    @Override
    public void setSeries(Series _s) {
        base.setSeries(_s);
        super.setSeries(_s);
    }

    @Override
    public int getLoookbackBars() {
        return period;
    }
}
