package com.klein.ta.funcs;

import com.klein.ta.Series;

public class Momentum extends AbstractTaFunction {

    @FunctionInput
    @DefaultIntValue(1)
    protected int period = 1;
    @FunctionInput
    protected AbstractTaFunction base;

    public Momentum(int period, AbstractTaFunction base, Series _s) {
        this(period, _s);
        this.base = base;
    }

    public Momentum(int period, Series _s) {
        this(_s);
        this.period = period;
    }

    public Momentum(Series _s) {
        super(_s);
        this.base = new Close(_s);
    }

    @Override
    protected String getKey() {
        return "mom_" + period + "_" + base.getKey();
    }

    @Override
    public int getLoookbackBars() {
        return period;
    }

    @Override
    public String getShortName() {
        return "Mom";
    }

    @Override
    public String getName() {
        return "Momentum " + base.getName();
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
                roc[i] = Math.abs((values[i] - values[i - period]) / values[i - period]);
//            System.out.println("ROC["+i+"]: "+roc[i]);
        }
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
