package com.klein.ta.funcs;

import com.klein.ta.Series;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

public class StdDev extends AbstractTaFunction {

    @FunctionInput(required = false)
    protected AbstractTaFunction base;
    @FunctionInput
    @DefaultDoubleValue(1D)
    protected double nbStdDev;
    @FunctionInput
    @DefaultIntValue(1)
    protected int smaPeriod;

    public StdDev(AbstractTaFunction base, double nbStdDev, int smaPeriod, Series _s) {
        this(_s);
        this.base = base;
        this.nbStdDev = nbStdDev;
        this.smaPeriod = smaPeriod;
    }

    public StdDev(Series _s) {
        super(_s);
        init();
    }

    public void init() {
        if (this.base == null)
            this.base = new Close(getSeries());
    }

    @Override
    protected String getKey() {
        return "stdDev_" + nbStdDev + "_" + smaPeriod + "_" + base.getKey();
    }

    @Override
    protected double[] calc() {
//        System.out.println("Calc: " + getKey());
        double[] values = base.values();
        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();
        double[] oldValues = getSeries().getCalculatedValues(getKey());
        int len = values.length;
        double[] sma = new double[len];
        RetCode res = core.stdDev(oldValues != null && oldValues.length > 1 ? oldValues.length - 2 : 0, len - 1, values, smaPeriod, nbStdDev, outBegIdx, outNBElement, sma);
        if (res == RetCode.Success) {
            // System.out.println(Arrays.toString(values));
            return createFullArray(outBegIdx, outNBElement, sma, 0D);
        } else {
            return null;
        }
    }

    @Override
    public String getShortName() {
        return "Std.Dev";
    }

    @Override
    public String getName() {
        return "StdDev " + smaPeriod + " x" + nbStdDev + " " + base.getName();
    }


    @Override
    public int getLoookbackBars() {
        return smaPeriod;
    }

    public AbstractTaFunction getBase() {
        return base;
    }

    public StdDev setBase(AbstractTaFunction base) {
        this.base = base;
        return this;

    }

    public double getNbStdDev() {
        return nbStdDev;
    }

    public StdDev setNbStdDev(double nbStdDev) {
        this.nbStdDev = nbStdDev;
        return this;

    }

    public int getSmaPeriod() {
        return smaPeriod;
    }

    public StdDev setSmaPeriod(int smaPeriod) {
        this.smaPeriod = smaPeriod;
        return this;
    }
}
