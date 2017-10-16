package com.klein.ta.funcs;

import com.klein.ta.Series;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

/**
 * Created by mresc on 27.05.16.
 */
public class SpreadLinear extends AbstractTaFunction {

    @FunctionInput
    protected AbstractTaFunction base1;
    @FunctionInput
    protected AbstractTaFunction base2;
    @FunctionInput
    @DefaultIntValue(value = 10)
    protected int period = 10;

    public SpreadLinear(Series series) {
        super(series);
        base1 = new Close(getSeries());
        base2 = new Close(getSeries());
    }

    @Override
    protected String getKey() {
        return "spread_linearb_" + period + "_" + base1.getKey() + "_" + base2.getKey();
    }

    @Override
    public String getShortName() {
        return "Spread Linear";
    }

    @Override
    protected double[] calc() {
        int length = getSeries().getLastIndex() + 1;
        double[] data = new double[length];

        int base2offset = base1.values().length - base2.values().length;

        for (int n = length - 1; n >= 0; n--) {

            if (n - period - 1 >= 0 && n - period - base2offset - 1 >= 0) {
                double[] x = new double[period];
                double[][] y = new double[period][1];
                for (int i = 0; i < period; i++) {
                    x[i] = base1.values()[n - i];
                    y[i][0] = base2.values()[n - i - base2offset];
//                    System.out.println(x[i] + "\t" + y[i][0]);
                }

                OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
                ols.setNoIntercept(true);
                ols.newSampleData(x, y);
                data[n] = base1.values()[n] - ols.estimateRegressionParameters()[0] * base2.values()[n - base2offset];
            } else {
                data[n] = -1D;
            }
        }
        if (!base1.getSeries().lastDate().isEqual(base2.getSeries().lastDate()))
            logError("Second series not the same last date: expected [{}], actual [{}]", base1.getSeries().lastDate(), base2.getSeries().lastDate());
//		System.out.println("Spread: " + Arrays.toString(data));
        return data;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public AbstractTaFunction getBase1() {
        return base1;
    }

    public void setBase1(AbstractTaFunction base1) {
        this.base1 = base1;
    }

    public AbstractTaFunction getBase2() {
        return base2;
    }

    public void setBase2(AbstractTaFunction base2) {
        this.base2 = base2;
    }

    @Override
    public int getLoookbackBars() {
        return period;
    }
}
