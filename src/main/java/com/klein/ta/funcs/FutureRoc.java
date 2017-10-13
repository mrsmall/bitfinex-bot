package com.klein.ta.funcs;

import com.klein.screener.strategy.Series;

public class FutureRoc extends AbstractTaFunction {

    @FunctionInput
    @DefaultIntValue(1)
    protected int period = 1;
    @FunctionInput
    protected AbstractTaFunction base;

    public FutureRoc(int period, AbstractTaFunction base, Series _s) {
        this(period, _s);
        this.base = base;
    }

    public FutureRoc(int period, Series _s) {
        this(_s);
        this.period = period;
    }

    public FutureRoc(Series _s) {
        super(_s);
        this.base = new Close(_s);
    }

    @Override
    protected String getKey() {
        return "fut_roc_" + period + "_" + base.getKey();
    }

    @Override
    public int getLoookbackBars() {
        return period;
    }

    @Override
    public String getShortName() {
        return "Fut.ROC";
    }

    @Override
    public String getName() {
        return "Fut. ROC " + period + " " + base.getName();
    }

    @Override
    protected double[] calc() {
        double[] values = base.values();

        double[] roc = new double[values.length];
        roc[0] = 0;
        for (int i = 0; i < values.length; i++) {
//            System.out.println("i: " + i);
            if (i > values.length - period - 1)
                roc[i] = 0;
            else
                roc[i] = (values[i] - values[i + period]) / values[i];
//            System.out.println("ROC["+i+"]: "+roc[i]);
        }
        return roc;
    }

    @Override
    public void setSeries(Series _s) {
        base.setSeries(_s);
        super.setSeries(_s);
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public void setBase(AbstractTaFunction base) {
        this.base = base;
    }
}
