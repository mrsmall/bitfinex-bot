package com.klein.ta.funcs;

import com.klein.ta.Series;

public class Multiplier extends AbstractTaFunction {

    @FunctionInput
    @DefaultDoubleValue(1D)
    protected double offset = 1;
    @FunctionInput
    protected AbstractTaFunction base;

    public Multiplier(int period, AbstractTaFunction base, Series _s) {
        this(period, _s);
        this.base = base;
    }

    public Multiplier(int period, Series _s) {
        this(_s);
        this.offset = period;
    }

    public Multiplier(Series _s) {
        super(_s);
        this.base = new Close(_s);
    }

    @Override
    protected String getKey() {
        return "mult_" + offset + "_" + base.getKey();
    }

    @Override
    public String getShortName() {
        return "Multiplier";
    }

    @Override
    public String getName() {
        return "Multiplier " + offset + " " + base.getName();
    }

    @Override
    protected double[] calc() {
        double[] values = base.values();

        double[] roc = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            roc[i] = values[i] * offset;
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
        return 0;
    }

    public double getOffset() {
        return offset;
    }

    public void setOffset(double offset) {
        this.offset = Math.round(offset * 100) / 100D;
    }

    public AbstractTaFunction getBase() {
        return base;
    }

    public void setBase(AbstractTaFunction base) {
        this.base = base;
    }
}
