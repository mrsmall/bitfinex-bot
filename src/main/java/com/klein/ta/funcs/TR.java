package com.klein.ta.funcs;

import com.klein.ta.Series;

public class TR extends AbstractTaFunction {

    public TR(Series _s) {
        super(_s);
    }

    @Override
    protected String getKey() {
        return "tr";
    }

    @Override
    public String getShortName() {
        return "TrueRange";
    }

    @Override
    public String getName() {
        return "TrueRange " + getSeries().getSymbol();
    }

    @Override
    protected double[] calc() {
        Series s = getSeries();

        double[] high = s.getHigh();
        double[] low = s.getLow();
        double[] close = s.getClose();

        double[] roc = new double[s.getDate().length];

        roc[0] = 0D;
        for (int i = 1; i < s.getDate().length; i++) {
            double val1 = high[i] - low[i];
            double val2 = high[i] - close[i - 1];
            double val3 = close[i - 1] - low[i];

            roc[i] = Math.max(Math.max(val1, val2), val3) * 100 / close[i];
        }
        return roc;
    }

}
