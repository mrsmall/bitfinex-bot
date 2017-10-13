package com.klein.ta.funcs;

import com.klein.screener.strategy.Series;

import java.util.Arrays;

public class BollingerLB extends AbstractTaFunction {

    @FunctionInput(required = false)
    protected AbstractTaFunction baseFunction;
    @FunctionInput
    @DefaultDoubleValue(1D)
    protected double nbStdDev;
    @FunctionInput
    @DefaultIntValue(10)
    protected int smaPeriod;

    Sma sma = null;
    StdDev sdvDevFunc = null;

    public BollingerLB(Series _s) {
        super(_s);
        init();
    }

    public void init() {
        super.init();
        if (baseFunction == null)
            baseFunction = new Close(getSeries());
        if (sma == null)
            sma = new Sma(getSeries());
        sma.setBase(baseFunction);
        sma.setPeriod(smaPeriod);

        if (sdvDevFunc == null)
            sdvDevFunc = new StdDev(getSeries());
        sdvDevFunc.setBase(baseFunction);
        sdvDevFunc.setNbStdDev(nbStdDev);
        sdvDevFunc.setSmaPeriod(smaPeriod);
    }


    @Override
    protected String getKey() {
        return "bblb_" + nbStdDev + "_" + smaPeriod + "_" + baseFunction.getKey();
    }

    @Override
    public String getShortName() {
        return "Bollinger LB";
    }

    @Override
    public String getName() {
        return "Bollinger LB  " + baseFunction.getName();
    }

    @Override
    protected double[] calc() {
//        System.out.println("Calc: " + getKey());
        double[] values = sdvDevFunc.values();
        double[] smaValues = sma.values();

        double[] oldValues = getSeries().getCalculatedValues(getKey());
        if (oldValues == null)
            oldValues = new double[]{};
        double[] bb = Arrays.copyOf(oldValues, values.length);
        for (int i = oldValues.length; i < values.length; i++) {
            bb[i] = smaValues[i] - values[i];
        }
        return bb;
    }

    @Override
    public void setSeries(Series _s) {
        sma.setSeries(_s);
        sdvDevFunc.setSeries(_s);
        super.setSeries(_s);
    }

    @Override
    public int getLoookbackBars() {
        return smaPeriod;
    }

    public AbstractTaFunction getBaseFunction() {
        return baseFunction;
    }

    public BollingerLB setBaseFunction(AbstractTaFunction baseFunction) {
        this.baseFunction = baseFunction;
        init();
        return this;
    }

    public BollingerLB setBase(AbstractTaFunction base) {
        return setBaseFunction(base);
    }

    public double getNbStdDev() {
        return nbStdDev;
    }

    public BollingerLB setNbStdDev(double nbStdDev) {
        this.nbStdDev = nbStdDev;
        init();
        return this;
    }

    public int getSmaPeriod() {
        return smaPeriod;
    }

    public BollingerLB setSmaPeriod(int smaPeriod) {
        this.smaPeriod = smaPeriod;
        init();
        return this;
    }

}
