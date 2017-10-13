package com.klein.ta.funcs;

import com.klein.screener.strategy.Series;
import org.joda.time.LocalDateTime;

public class SpreadDevide extends AbstractTaFunction {

	@FunctionInput(required = true)
	protected Series series2;

	public SpreadDevide(Series _s, Series series2) {
		this(_s);
		this.series2 = series2;
	}

	public SpreadDevide(Series _s) {
		super(_s);
		series2=_s;
	}


	@Override
	protected String getKey() {
		return "spread_div_" + series2.getSymbol();
	}

	@Override
	protected double[] calc() {
		Series s1 = getSeries();
		double[] data = new double[s1.getLastIndex() + 1];

		int index1 = 0;
		int index2 = 0;
		Double lastValue = null;
		while (index1 <= s1.getLastIndex() && index2 <= series2.getLastIndex()) {
			LocalDateTime date1 = s1.getDate()[index1];
			LocalDateTime date2 = series2.getDate()[index2];
			if (date1.isAfter(date2)) {
				index2++;
				continue;
			}
			if (date1.isBefore(date2)) {
				data[index1] = lastValue != null ? lastValue : 0D;
				index1++;
				continue;
			}

			double close1 = s1.getClose()[index1];
			double close2 = series2.getClose()[index2];
			double osziValue = close1 / close2;

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
		if (!s1.lastDate().isEqual(series2.lastDate()))
			logError("Second series not the same last date: expected [{}], actual [{}]", s1.lastDate(), series2.lastDate());
//		System.out.println("Spread: " + Arrays.toString(data));
		return data;
	}

	@Override
	public String getShortName() {
		return "SpreadDivide";
	}


	@Override
	public String getName() {
		return getShortName()+" "+getSeries().getSymbol()+"/"+series2.getSymbol();
	}

	@Override
	public int getLoookbackBars() {
		return 0;
	}

	public Series getSeries2() {
		return series2;
	}

	public void setSeries2(Series series2) {
		this.series2 = series2;
	}
}
