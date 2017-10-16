package com.klein.ta.funcs;

import com.klein.ta.Series;

public class Offset extends AbstractTaFunction {

    @FunctionInput
    @DefaultDoubleValue(0D)
    protected double offset = 0;
    @FunctionInput
    private AbstractTaFunction base;

    public Offset(int period, AbstractTaFunction base, Series _s) {
        this(period, _s);
        this.base = base;
    }

    public Offset(int period, Series _s) {
        this(_s);
        this.offset = period;
    }

    public Offset(Series _s) {
        super(_s);
        this.base = new Close(_s);
    }

    @Override
    protected String getKey() {
        return "offset_" + offset + "_" + base.getKey();
    }

    @Override
    public String getShortName() {
        return "Offset";
    }

    @Override
    public String getName() {
        return "OFFSET " + base.getName();
    }

    @Override
    protected double[] calc() {
        double[] values = base.values();

        double[] roc = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            roc[i] = values[i] + offset;
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
}
