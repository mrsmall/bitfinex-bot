package com.klein.ta.funcs;

import com.klein.ta.Series;

/**
 * Created by mresc on 11.01.16.
 */
public class Close extends AbstractTaFunction {

    public Close(Series _s) {
        super(_s);
    }

    @Override
    protected String getKey() {
        return Series.KEY_CLOSE;
    }

    @Override
    protected double[] calc() {
        double[] data = getSeries().getClose();
//        if (getSeries().getSymbol().equals("CLR"))
//            System.out.print(Arrays.toString(data));
        return data;
    }

    @Override
    public String getShortName() {
        return "Close";
    }

    @Override
    public String getName() {
        return "Close " + getSeries().getSymbol();
    }

    @Override
    public int getLoookbackBars() {
        return 0;
    }

}