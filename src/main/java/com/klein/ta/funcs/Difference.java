package com.klein.ta.funcs;

import com.klein.ta.Series;
import org.joda.time.LocalDateTime;

public class Difference extends AbstractTaFunction {

    @FunctionInput
    protected AbstractTaFunction base1;
    @FunctionInput
    protected AbstractTaFunction base2;

    public Difference(Series _s) {
        super(_s);
        base1 = new Close(_s);
        base2 = base1;
    }


    @Override
    protected String getKey() {
        return "diff_" + base1.getKey() + "_" + base2.getKey();
    }

    @Override
    protected double[] calc() {
        double[] data = new double[base1.getSeries().getLastIndex() + 1];

        int index1 = 0;
        int index2 = 0;
        Double lastValue = null;
        while (index1 <= base1.getSeries().getLastIndex() && index2 <= base2.getSeries().getLastIndex()) {
            LocalDateTime date1 = base1.getSeries().getDate()[index1];
            LocalDateTime date2 = base2.getSeries().getDate()[index2];
            if (date1.isAfter(date2)) {
                index2++;
                continue;
            }
            if (date1.isBefore(date2)) {
                data[index1] = lastValue != null ? lastValue : 0D;
                index1++;
                continue;
            }

            double close1 = base1.values()[index1];
            double close2 = base2.values()[index2];
            double osziValue = close1 - close2;

            if (lastValue == null) {
                lastValue = osziValue;
            }
            data[index1] = osziValue;

//			System.out.println("Close1: " + close1);
//			System.out.println("Close2: " + close2);
//			System.out.println("Spread: " + data[index1]);

            lastValue = osziValue;

            index1++;
            index2++;
        }
        if (!base1.getSeries().lastDate().isEqual(base2.getSeries().lastDate()))
            logError("Second series not the same last date: expected [{}], actual [{}]", base1.getSeries().lastDate(), base2.getSeries().lastDate());
//		System.out.println("Spread: " + Arrays.toString(data));
        return data;
    }

    @Override
    public String getShortName() {
        return "Difference";
    }


    @Override
    public String getName() {
        return getShortName() + " " + base1.getName() + "/" + base2.getName();
    }

    @Override
    public int getLoookbackBars() {
        return 0;
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
}
