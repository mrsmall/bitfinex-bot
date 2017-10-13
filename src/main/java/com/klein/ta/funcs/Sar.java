package com.klein.ta.funcs;

import com.klein.ta.Series;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

public class Sar extends AbstractTaFunction {

    @FunctionInput
    @DefaultDoubleValue(0.1)
    protected double acceleration;
    @FunctionInput
    @DefaultDoubleValue(1)
    protected double maximum;

    public Sar(Series _s) {
        super(_s);
    }

    @Override
    protected String getKey() {
        return "sar_" + acceleration + "_" + maximum;
    }

    @Override
    protected double[] calc() {
        double[] valuesHigh = getSeries().getHigh();
        double[] valuesLow = getSeries().getLow();
//        System.out.println(Arrays.toString(values));
        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();
        double[] oldValues = null;//getSeries().getValues(getKey());
        int len = valuesHigh.length;
        double[] sar = new double[len];

        RetCode res = core.sar(oldValues != null ? oldValues.length - 1 : 0, len - 1, valuesLow, valuesHigh, acceleration, maximum, outBegIdx, outNBElement, sar);
        if (res == RetCode.Success) {
            double[] fullArray = createFullArray(oldValues, outBegIdx, outNBElement, sar, 0D);
//            System.out.println(Arrays.toString(fullArray));
            return fullArray;
        } else {
            return null;
        }

    }

    @Override
    public String getShortName() {
        return "Par.SAR";
    }

    @Override
    public int getLoookbackBars() {
        return 0;
    }

}
