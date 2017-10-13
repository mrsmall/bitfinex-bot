package com.klein.ta.funcs;

import com.klein.ta.Series;
import org.joda.time.LocalDateTime;

public class RelationPercent extends AbstractTaFunction {

    @FunctionInput
    protected AbstractTaFunction base1;
    @FunctionInput
    AbstractTaFunction base2;

    public RelationPercent(Series _s) {
        super(_s);
        base1 = new Close(_s);
        base2 = base1;
    }


    @Override
    protected String getKey() {
        return "rel_perc_" + base1.getKey() + "_" + base2.getKey();
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

            double close1 = base1.getSeries().getClose()[index1];
            double close2 = base2.getSeries().getClose()[index2];
            double osziValue = (close1 - close2) * 100 / close2;

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
        return "Rel. Percent";
    }


    @Override
    public String getName() {
        return getShortName() + " " + base1.getName() + "/" + base2.getName();
    }

    @Override
    public int getLoookbackBars() {
        return 0;
    }
}
