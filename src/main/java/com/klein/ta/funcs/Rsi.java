package com.klein.ta.funcs;

import com.klein.screener.strategy.Series;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

public class Rsi extends AbstractTaFunction {

    @FunctionInput
    protected AbstractTaFunction base;
    @FunctionInput
    @DefaultIntValue(1)
    protected int smaPeriod;

    public Rsi(AbstractTaFunction base, int smaPeriod, Series _s) {
        this(_s);
        this.base = base;
        this.smaPeriod = smaPeriod;
    }

    public Rsi(Series _s) {
        super(_s);
        this.base = new Close(_s);
    }

    @Override
    protected String getKey() {
        return "rsi_" + +smaPeriod + "_" + base.getKey();
    }

    @Override
    protected double[] calc() {
        double[] values = base.values();
        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();
        int len = values.length;
        double[] sma = new double[len];
        RetCode res = core.rsi(0, len - 1, values, smaPeriod, outBegIdx, outNBElement, sma);
        if (res == RetCode.Success) {
            // System.out.println(Arrays.toString(values));
            return createFullArray(outBegIdx, outNBElement, sma, 0D);
        } else {
            return null;
        }
    }

    @Override
    public String getShortName() {
        return "RSI";
    }
    @Override
    public String getName() {
        return "RSI "+base.getName();
    }

    @Override
    public void setSeries(Series _s) {
        base.setSeries(_s);
        super.setSeries(_s);
    }


    @Override
    public int getLoookbackBars() {
        return smaPeriod;
    }

    public AbstractTaFunction getBase() {
        return base;
    }

    public void setBase(AbstractTaFunction base) {
        this.base = base;
    }

    public int getSmaPeriod() {
        return smaPeriod;
    }

    public void setSmaPeriod(int smaPeriod) {
        this.smaPeriod = smaPeriod;
    }
}
