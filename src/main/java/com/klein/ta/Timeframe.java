package com.klein.ta;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

public enum Timeframe {
	NA(Integer.MIN_VALUE), M1(60), M5(300), M15(900), M30(1800), H1(3600), H3(3600*3), H6(3600*6), H12(3600*12), D1(86400), D7(86400*7), D14(86400*14), _M1(86400*30);

	private int seconds = 0;

	Timeframe(int s) {
		seconds = s;
	}

	public int getSeconds() {
		return seconds;
	}

	public boolean isIntraday() {
		return seconds < D1.seconds;
	}

	public LocalDateTime getBeginDateOfBar(LocalDateTime ts) {
		if (isIntraday()) {
			DateTime tsLocalDate = ts.toDateTime().withMillisOfSecond(0);
            if (getSeconds() >= Timeframe.M5.getSeconds()
					&& getSeconds() < Timeframe.H1.getSeconds()) {
				int multiplicator = getSeconds() / 60;
				int bars = (int) Math.floor(tsLocalDate.getMinuteOfHour() / multiplicator);
				return tsLocalDate.withMinuteOfHour(bars * multiplicator).withSecondOfMinute(0).toLocalDateTime();
			} else if (this == Timeframe.H1) {
				return tsLocalDate.withSecondOfMinute(0).withSecondOfMinute(0).toLocalDateTime();
            } else if (this == Timeframe.H3 || this == Timeframe.H6 || this == Timeframe.H12) {
                double multiplikator=3D;
                if (this == Timeframe.H6)
                    multiplikator=6D;
                else if (this == Timeframe.H12)
                    multiplikator=12;
                int barNr = (int) Math.floor(ts.getHourOfDay() / multiplikator);
                LocalDateTime sessionBeginForTS = ts.withTime((int) (barNr * multiplikator),
                        0, 0, 0);
                return sessionBeginForTS;
			}
		} else {
			LocalDate tsLocalDate = ts.toLocalDate();
			if (this == Timeframe.D1) {
				return tsLocalDate.toDateTimeAtStartOfDay().toLocalDateTime();
            } else if (this == Timeframe.D7) {
                return tsLocalDate.dayOfWeek().withMinimumValue().toDateTimeAtStartOfDay().toLocalDateTime();
            } else if (this == Timeframe.D14) {
                return tsLocalDate.dayOfWeek().withMinimumValue().toDateTimeAtStartOfDay().toLocalDateTime();
			} else if (this == Timeframe._M1) {
				return tsLocalDate.withDayOfMonth(1).toDateTimeAtStartOfDay().toLocalDateTime();
			}
		}
		return null;
	}

	public boolean isNewBar(LocalDateTime lastBarBegin, LocalDateTime ts) {
		if (isIntraday()) {
			boolean newBar = !lastBarBegin.plusSeconds(getSeconds()).isAfter(ts);
			return newBar;
		} else {
			LocalDate tsLocalDate = ts.toLocalDate();
			LocalDate dateLocalDate = lastBarBegin.toLocalDate();

			if (this == Timeframe.D1) {
				return !tsLocalDate.toDateTimeAtStartOfDay().isEqual(dateLocalDate.toDateTimeAtStartOfDay());
			} else if (this == Timeframe.D7) {
				int tsYearAndWeek = tsLocalDate.getWeekyear() + tsLocalDate.getYear() * 10;
				int dateYearAndWeek = dateLocalDate.getWeekyear() + dateLocalDate.getYear() * 10;
				return tsYearAndWeek != dateYearAndWeek;
			} else if (this == Timeframe._M1) {
				int tsYearAndMonth = tsLocalDate.getMonthOfYear() + tsLocalDate.getYear() * 10;
				int dateYearAndMonth = dateLocalDate.getMonthOfYear() + dateLocalDate.getYear() * 10;
				return tsYearAndMonth != dateYearAndMonth;
			}
		}

		return false;
	}

}
