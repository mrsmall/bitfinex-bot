package com.klein.ta.funcs;

import com.klein.ta.Series;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

public class Sma extends AbstractTaFunction {

    @FunctionInput
    @DefaultIntValue(20)
    protected int period = 20;
    @FunctionInput
    protected AbstractTaFunction base;

    public Sma(Series _s) {
        super(_s);
        this.base = new Close(_s);
    }

    @Deprecated
    public Sma(int period, AbstractTaFunction base, Series _s) {
        super(_s);
        this.period = period;
        this.base = base;
    }

    @Override
    protected String getKey() {
        return "sma_" + period + "_" + base.getKey();
    }

    @Override
    protected double[] calc() {
//        System.out.println("Calc: " + getKey());
        double[] values = base.values();
//        printValues("Base values: ", values, 2);
        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();
        double[] oldValues = getSeries().getCalculatedValues(getKey());
//        if (oldValues != null)
//            printValues("Old values: ", oldValues, 2);
        int len = values.length;
        double[] sma = new double[len];
        RetCode res = core.sma(oldValues != null && oldValues.length > 1 ? oldValues.length - 2 : 0, len - 1, values, period, outBegIdx, outNBElement, sma);
//        printValues("Calc values: ", sma, 2);
        if (res == RetCode.Success) {
            double[] fullArray = createFullArray(oldValues, outBegIdx, outNBElement, sma, 0D);
//            printValues("Full array values: ", fullArray, 2);
            return fullArray;
        } else {
            return null;
        }

    }

    public int getPeriod() {
        return period;
    }

    public Sma setPeriod(int period) {
        this.period = period;
        return this;
    }

    public AbstractTaFunction getBase() {
        return base;
    }

    public Sma setBase(AbstractTaFunction base) {
        this.base = base;
        return this;
    }

    @Override
    public String getShortName() {
        return "SMA";
    }

    @Override
    public String getName() {
        return "SMA " + period + " " + base.getName();
    }

    @Override
    public void setSeries(Series _s) {
        base.setSeries(_s);
        super.setSeries(_s);
    }

    @Override
    public int getLoookbackBars() {
        return period + base.getLoookbackBars();
    }

}
