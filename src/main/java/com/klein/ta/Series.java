package com.klein.ta;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Series {
    public static final String KEY_CLOSE = "close";
    private static final Logger LOG = LoggerFactory.getLogger(Series.class);
    private static final String KEY_OPEN = "open";
    private static final String KEY_HIGH = "high";
    private static final String KEY_LOW = "low";
    private static final String KEY_VOLUME = "valume";

    Timeframe timeframe;
    int lastIndex = -1;
    int tickIndex = -1;

    LocalDateTime[] date = new LocalDateTime[]{};
    Map<String, double[]> _values = new HashMap<String, double[]>();
    Map<String, Integer> calculatedOnTick = new HashMap<String, Integer>();
    Map<String, Integer> calculatedOnBar = new HashMap<String, Integer>();
    boolean historicalDataLoaded = true;
    SortedSet<Tick> bufferedTicks = new TreeSet<Tick>();
    private String product;
    private double lastAsk;
    private double lastBidSize;
    private LocalDateTime lastBidTimestamp;
    private double lastBid;
    private double lastAskSize;
    private LocalDateTime lastAskTimestamp;
    private double avgBid;
    private double avgAsk;
    private int maxBars;

    public Series(String product, Timeframe tf, int maxBars) {
        this.product = product;
        this.timeframe = tf;
        this.maxBars = maxBars;
    }

    public static Series build(List<Quote> quotes) {
        return build(quotes, null, 0);
    }

    public static Series build(List<Quote> quotes, int maxBars) {
        return build(quotes, null, maxBars);
    }

    public static Series build(List<Quote> quotes, LocalTime sessionBegin, int maxBars) {
        Quote q = quotes.get(0);
        Series s = new Series(q.getProduct(), q.getTimeframe(), maxBars);
        s.setValues(quotes);
        return s;
    }

    public void setValues(String key, double[] values) {
        _values.put(key, values);
        calculatedOnTick.put(key, tickIndex);
        calculatedOnBar.put(key, lastIndex);
    }

    public double[] getValues(String key) {
        double[] values = _values.get(key);
        if (values == null) {
            values = new double[lastIndex + 1];
            _values.put(key, values);
        } else if (values.length < lastIndex + 1) {
            values = Arrays.copyOf(values, lastIndex + 1);
            _values.put(key, values);
        }
        return values;
    }

    public void addTick(PriceType priceType, LocalDateTime ts, double price, double vol) {
        if (!historicalDataLoaded) {
            addTickToBuffer(ts, priceType, price, vol);
            return;
        }

        if (priceType == PriceType.BID) {
            if (avgBid == 0) {
                avgBid = price;
            } else {
                avgBid = avgBid * 0.5 + price * 0.5;
            }
            lastBid = price;
            lastBidSize = vol;
            lastBidTimestamp = ts;
        } else if (priceType == PriceType.ASK) {
            if (avgAsk == 0) {
                avgAsk = price;
            } else {
                avgAsk = avgAsk * 0.5 + price * 0.5;
            }
            lastAsk = price;
            lastAskSize = vol;
            lastAskTimestamp = ts;
        } else {
            tickIndex++;
            if (lastIndex == -1 || isNewBar(date[lastIndex], ts)) {
                nextBar(ts, price);
            }

            getValues(KEY_CLOSE)[lastIndex] = price;
            double[] volume = getValues(KEY_VOLUME);
            volume[lastIndex] = volume[lastIndex] + vol;

            double[] high = getValues(KEY_HIGH);
            if (high[lastIndex] < price) {
                high[lastIndex] = price;
            }
            double[] low = getValues(KEY_LOW);
            if (low[lastIndex] > price) {
                low[lastIndex] = price;
            }
        }
    }

    private void addTickToBuffer(LocalDateTime ts, PriceType priceType, double price, double vol) {
        //check if there is
        if (bufferedTicks.size()>1000){
            int i=0;
            for (Tick bufferedTick : new ArrayList<>(bufferedTicks)) {
                if (i<100){
                    bufferedTicks.remove(bufferedTick);
                } else {
                    break;
                }
                i++;
            }
        }
        bufferedTicks.add(new Tick(ts, priceType, price, vol));
    }

    void nextBar(LocalDateTime ts, double price) {
        shortenArrays();
        lastIndex++;
        LOG.trace("Next bar: {}", ts);


        date = Arrays.copyOf(date, lastIndex + 1);
        date[lastIndex] = getBeginDateOfBar(ts);

        getValues(KEY_OPEN)[lastIndex] = price;
        getValues(KEY_HIGH)[lastIndex] = price;
        getValues(KEY_LOW)[lastIndex] = price;
        getValues(KEY_CLOSE)[lastIndex] = price;
    }


    private void shortenArrays() {
        if (maxBars == 0)
            return;
        if (lastIndex < maxBars - 1)
            return;

        int oldDateSize = date.length;
        lastIndex = maxBars - 2;
        LOG.trace("Date last length: {}, maxBars: {}, lastDate: {}", date.length, maxBars, date[date.length - 1]);
        date = Arrays.copyOfRange(date, date.length - maxBars + 1, date.length);
        LOG.trace("Date new length: {}", date.length);

        for (String key : _values.keySet()) {
            double[] values = _values.get(key);
            int sizeDIffToDate = (oldDateSize - values.length);
            int cutAtIndex = values.length - maxBars + 1 - sizeDIffToDate;
            LOG.trace("Values for {} last length: {}, maxBars: {}, last: {}", key, values.length, maxBars, values[values.length - 1]);
            if (cutAtIndex > 0)
                values = Arrays.copyOfRange(values, cutAtIndex, values.length);
            LOG.trace("Values for {} new length: {}", key, values.length);
            _values.put(key, values);
        }

    }

    private LocalDateTime getBeginDateOfBar(LocalDateTime ts) {
        if (timeframe.isIntraday()) {
            DateTime tsLocalDate = ts.toDateTime().withMillisOfSecond(0);
            if (timeframe == Timeframe.M1) {
                return tsLocalDate.withSecondOfMinute(0).toLocalDateTime();
            } else if (timeframe.getSeconds() >= Timeframe.M5.getSeconds()
                    && timeframe.getSeconds() < Timeframe.H1.getSeconds()) {
                int multiplicator = timeframe.getSeconds() / 60;
                int bars = (int) Math.floor(tsLocalDate.getMinuteOfHour() / multiplicator);
                return tsLocalDate.withMinuteOfHour(bars * multiplicator).withSecondOfMinute(0).toLocalDateTime();
            } else if (timeframe == Timeframe.H1) {
                return tsLocalDate.withSecondOfMinute(0).withSecondOfMinute(0).toLocalDateTime();
            } else if (timeframe == Timeframe.H3 || timeframe == Timeframe.H6 || timeframe == Timeframe.H12) {
                double multiplikator=3D;
                if (timeframe== Timeframe.H6)
                    multiplikator=6D;
                else if (timeframe== Timeframe.H12)
                    multiplikator=12;
                int barNr = (int) Math.floor(ts.getHourOfDay() / multiplikator);
                LocalDateTime sessionBeginForTS = ts.withTime((int) (barNr * multiplikator),
                        0, 0, 0);
                return sessionBeginForTS;
            }
        } else {
            LocalDate tsLocalDate = ts.toLocalDate();
            if (timeframe == Timeframe.D1) {
                return tsLocalDate.toDateTimeAtStartOfDay().toLocalDateTime();
            } else if (timeframe == Timeframe.D7) {
                return tsLocalDate.dayOfWeek().withMinimumValue().toDateTimeAtStartOfDay().toLocalDateTime();
            } else if (timeframe == Timeframe.D14) {
                return tsLocalDate.dayOfWeek().withMinimumValue().toDateTimeAtStartOfDay().toLocalDateTime();
            } else if (timeframe == Timeframe._M1) {
                return tsLocalDate.withDayOfMonth(1).toDateTimeAtStartOfDay().toLocalDateTime();
            }
        }
        return null;
    }

    public boolean isNewBar(LocalDateTime ts) {
        if (date == null || date.length == 0)
            return true;
        else
            return isNewBar(date[lastIndex], ts);
    }

    public boolean isNewBar(LocalDateTime lastBarBegin, LocalDateTime ts) {
        if (timeframe.isIntraday()) {
            LOG.trace("Intraday timeframe");
            boolean newBar = lastBarBegin == null || !lastBarBegin.plusSeconds(timeframe.getSeconds()).isAfter(ts);
            return newBar;
        } else {
            LOG.trace("EOD timeframe");
            LocalDate tsLocalDate = ts.toLocalDate();
            LocalDate dateLocalDate = lastBarBegin.toLocalDate();

            if (timeframe == Timeframe.D1) {
                return !tsLocalDate.toDateTimeAtStartOfDay().isEqual(dateLocalDate.toDateTimeAtStartOfDay());
            } else if (timeframe == Timeframe.D7) {
                int tsYearAndWeek = tsLocalDate.getWeekyear() + tsLocalDate.getYear() * 10;
                int dateYearAndWeek = dateLocalDate.getWeekyear() + dateLocalDate.getYear() * 10;
                return tsYearAndWeek != dateYearAndWeek;
            } else if (timeframe == Timeframe.M1) {
                int tsYearAndMonth = tsLocalDate.getMonthOfYear() + tsLocalDate.getYear() * 10;
                int dateYearAndMonth = dateLocalDate.getMonthOfYear() + dateLocalDate.getYear() * 10;
                return tsYearAndMonth != dateYearAndMonth;
            }
        }

        return false;
    }

    public Timeframe getTimeframe() {
        return timeframe;
    }

    public int getLastIndex() {
        return lastIndex;
    }

    public LocalDateTime[] getDate() {
        return date;
    }

    public double[] getOpen() {
        return getValues(KEY_OPEN);
    }

    public double[] getHigh() {
        return getValues(KEY_HIGH);
    }

    public double[] getLow() {
        return getValues(KEY_LOW);
    }

    public double[] getClose() {
        return getValues(KEY_CLOSE);
    }

    public double[] getVolume() {
        return getValues(KEY_VOLUME);
    }

    public double[][] aroon(int period) {
        double[] inHigh = getHigh();
        double[] inLow = getLow();
        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();
        int len = inHigh.length;
        double[] outAroonDown = new double[len];
        double[] outAroonUp = new double[len];
        RetCode res = new Core().aroon(0, len - 1, inHigh, inLow, period, outBegIdx, outNBElement, outAroonDown,
                outAroonUp);
        if (res == RetCode.Success) {
            return new double[][]{outAroonDown, outAroonUp};
        } else {
            return null;
        }
    }

    public double[] roc(int period) {
        return roc(period, KEY_CLOSE);
    }

    public double[] roc(int period, String fieldKey) {
        double[] close = getValues(fieldKey);
        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();
        int len = close.length;
        double[] roc = new double[len];
        RetCode res = new Core().roc(0, len - 1, close, period, outBegIdx, outNBElement, roc);
        if (res == RetCode.Success) {
            return createFullArray(outBegIdx, outNBElement, roc, 0D);
        } else {
            return null;
        }
    }

    public double[] createFullArray(MInteger outBegIdx, MInteger outNBElement, double[] roc, double emptyValue) {
        // System.out.println("outBegIdx: " + outBegIdx.value);
        // System.out.println("outNBElement: " + outNBElement.value);
        roc = Arrays.copyOf(roc, outNBElement.value);
        double[] fullRoc = new double[lastIndex + 1];
        Arrays.fill(fullRoc, 0, outBegIdx.value, emptyValue);
        System.arraycopy(roc, 0, fullRoc, outBegIdx.value, roc.length);
        return fullRoc;
    }

    public double[] correlOnRoc(int rocPeriod, int period, Series series2) {
        double[] roc1 = roc(rocPeriod);
        double[] roc2 = series2.roc(rocPeriod);
        if (roc1.length != roc2.length) {
            return null;
        }

        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();
        int len = roc1.length;
        double[] corr = new double[len];
        RetCode res = new Core().correl(0, len - 1, roc1, roc2, period, outBegIdx, outNBElement, corr);
        if (res == RetCode.Success) {
            // System.out.println(Arrays.toString(corr));
            return createFullArray(outBegIdx, outNBElement, corr, 0D);
        } else {
            return null;
        }
    }

    public double[] smaOnCorrelOnRoc(int smaPeriod, int rocPeriod, int correlPeriod, Series series2) {
        double[] correl = correlOnRoc(rocPeriod, correlPeriod, series2);
        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();
        int len = correl.length;
        double[] smaCorr = new double[len];
        RetCode res = new Core().sma(0, len - 1, correl, smaPeriod, outBegIdx, outNBElement, smaCorr);
        if (res == RetCode.Success) {
            // System.out.println(Arrays.toString(corr));
            return createFullArray(outBegIdx, outNBElement, smaCorr, 0D);
        } else {
            return null;
        }

    }

    public void resetValues(List<Quote> quotes) {
        _values.clear();
        calculatedOnTick.clear();
        setValues(quotes);
    }

    private void setValues(List<Quote> quotes) {
        if (maxBars > 0 && quotes.size() > maxBars)
            quotes = quotes.subList(quotes.size() - maxBars, quotes.size());

        setSize(quotes.size());

        double[] open = getOpen();
        double[] high = getHigh();
        double[] low = getLow();
        double[] close = getClose();
        double[] volume = getVolume();

        for (int i = 0; i < quotes.size(); i++) {
            Quote q = quotes.get(i);
            open[i] = q.getOpen();
            high[i] = q.getHigh();
            low[i] = q.getLow();
            close[i] = q.getClose();
            volume[i] = q.getVolume();
            date[i] = new LocalDateTime(q.getDate());
        }
    }

    private synchronized void setSize(int size) {
        lastIndex = size - 1;
        date = Arrays.copyOf(date, lastIndex + 1);

        String[] keys = new String[]{KEY_CLOSE, KEY_HIGH, KEY_LOW, KEY_OPEN, KEY_VOLUME};
        for (String key : keys) {
            getValues(key);
        }
    }

    public void addBar(LocalDateTime ts, double open, double high, double low, double close, double volume) {
        nextBar(ts, close);
        getOpen()[lastIndex] = open;
        getHigh()[lastIndex] = high;
        getLow()[lastIndex] = low;
        getVolume()[lastIndex] = volume;
    }

    public LocalDateTime lastDate() {
        if (lastIndex != -1 && lastIndex < getDate().length) {
            return getDate()[lastIndex];
        } else {
            return null;
        }
    }

    public LocalDateTime date(int i) {
        LocalDateTime[] close = getDate();
        return close[close.length - (i + 1)];
    }

    public double lastClose() {
        if (lastIndex != -1 && lastIndex < getClose().length) {
            return getClose()[lastIndex];
        } else {
            return Double.MIN_VALUE;
        }
    }

    public double close(int i) {
        double[] close = getClose();
        return close[close.length - (i + 1)];
    }

    public double[] sma(int period) {
        return sma(period, KEY_CLOSE);
    }

    public double[] sma(int period, String fieldKey) {
        double[] inValues = getValues(fieldKey);
        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();
        int len = inValues.length;
        double[] outValues = new double[len];
        RetCode res = new Core().sma(0, len - 1, inValues, period, outBegIdx, outNBElement, outValues);
        if (res == RetCode.Success) {
            return createFullArray(outBegIdx, outNBElement, outValues, 0D);
        } else {
            return null;
        }
    }

    public double[] spread(Series s2) {
        double[] data = new double[lastIndex + 1];

        Double factor = null;
        LocalDateTime lastDate = null;
        int index1 = 0;
        int index2 = 0;
        Double lastValue = null;
        while (index1 <= getLastIndex() && index2 <= s2.getLastIndex()) {
            LocalDateTime date1 = getDate()[index1];
            LocalDateTime date2 = s2.getDate()[index2];
            if (date1.isAfter(date2)) {
                index2++;
                continue;
            }
            if (date1.isBefore(date2)) {
                data[index1] = lastValue != null ? lastClose() : 0D;
                index1++;
                continue;
            }

            if (lastDate != null && !date1.withTime(0, 0, 0, 0).isEqual(lastDate.withTime(0, 0, 0, 0))) {
                factor = null;
                // osziLastValue = null;
                // osziMa = null;
            }

            double close_s2 = 0D;
            double close1 = getClose()[index1];
            double close2 = s2.getClose()[index2];
            if (factor == null) {
                factor = close1 / close2;
            }
            close_s2 = close2 * factor;
            // System.out.println("Factor: " + factor);
            double osziValue = Math.abs(close1 - close_s2);
            // System.out.println("oszi: " + osziValue);

            if (lastValue == null) {
                lastValue = osziValue;
            }
            data[index1] = osziValue;

            lastDate = date1;
            lastValue = osziValue;

            index1++;
            index2++;
        }
        if (!lastDate().isEqual(s2.lastDate())) {
            LOG.error("Second series not the same last date: expected [{}], actual [{}]", lastDate(), s2.lastDate());
        }
        return data;
    }

    public double[] spreadDevide(Series s2) {
        String key = "spread_" + s2.getSymbol();
        Integer lastCalcTick = calculatedOnTick.get(key);
        if (lastCalcTick != null && lastCalcTick == tickIndex) {
            return _values.get(key);
        } else {
            double[] data = calcSpreadDevide(s2);
            _values.put(key, data);
            return data;
        }
    }

    private double[] calcSpreadDevide(Series s2) {
        double[] data = new double[lastIndex + 1];

        int index1 = 0;
        int index2 = 0;
        Double lastValue = null;
        while (index1 <= getLastIndex() && index2 <= s2.getLastIndex()) {
            LocalDateTime date1 = getDate()[index1];
            LocalDateTime date2 = s2.getDate()[index2];
            if (date1.isAfter(date2)) {
                index2++;
                continue;
            }
            if (date1.isBefore(date2)) {
                data[index1] = lastValue != null ? lastValue : 0D;
                index1++;
                continue;
            }

            double close1 = getClose()[index1];
            double close2 = s2.getClose()[index2];
            double osziValue = close1 / close2;

            if (lastValue == null) {
                lastValue = osziValue;
            }
            data[index1] = osziValue;

            lastValue = osziValue;

            index1++;
            index2++;
        }
        if (!lastDate().isEqual(s2.lastDate())) {
            LOG.error("Second series not the same last date: expected [{}], actual [{}]", lastDate(), s2.lastDate());
        }
        return data;
    }

    public String getSymbol() {
        return product;
    }

    public double[] smaOnSpread(int smaPeriod, Series s2) {
        double[] values = spread(s2);
        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();
        int len = values.length;
        double[] sma = new double[len];
        RetCode res = new Core().sma(0, len - 1, values, smaPeriod, outBegIdx, outNBElement, sma);
        if (res == RetCode.Success) {
            // System.out.println(Arrays.toString(values));
            return createFullArray(outBegIdx, outNBElement, sma, 0D);
        } else {
            return null;
        }
    }

    public double[] smaOnSpreadDevide(int smaPeriod, Series s2) {
        double[] values = spreadDevide(s2);
        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();
        int len = values.length;
        double[] sma = new double[len];
        RetCode res = new Core().sma(0, len - 1, values, smaPeriod, outBegIdx, outNBElement, sma);
        if (res == RetCode.Success) {
            // System.out.println(Arrays.toString(values));
            return createFullArray(outBegIdx, outNBElement, sma, 0D);
        } else {
            return null;
        }
    }

    private double lastDayClose() {
        int lastDayIndex = getLastIndexOfLastDay();
        return getClose()[lastDayIndex];
    }

    private int getLastIndexOfLastDay() {
        return getLastIndexOfPreviousDay(lastIndex);
    }

    protected int getLastIndexOfPreviousDay(int beforeIndex) {
        LocalDateTime lastDate = lastDate();
        int dayNr = getYearAndDayNr(lastDate);
        for (int i = beforeIndex; i >= 0; i--) {
            LocalDateTime date = getDate()[i];
            if (dayNr != getYearAndDayNr(date)) {
                return i;
            }
        }
        return 0;
    }

    private int getYearAndDayNr(LocalDateTime date) {
        return date.getYear() * 100 + date.getDayOfYear();
    }

    public double[] stdDevOnSpread(double nbStdDev, int smaPeriod, Series s2) {
        double[] values = spreadDevide(s2);
        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();
        int len = values.length;
        double[] sma = new double[len];
        RetCode res = new Core().stdDev(0, len - 1, values, smaPeriod, nbStdDev, outBegIdx, outNBElement, sma);
        if (res == RetCode.Success) {
            // System.out.println(Arrays.toString(values));
            return createFullArray(outBegIdx, outNBElement, sma, 0D);
        } else {
            return null;
        }

    }

    public boolean needRecalc(String key) {
        Integer lastCalcTick = calculatedOnTick.get(key);
        if (lastCalcTick != null && lastCalcTick == tickIndex) {
            return false;
        } else {
            return true;
        }
    }

    public double getAvgSpread() {
        return avgBid - avgAsk;
    }

    public double getLastAsk() {
        return lastAsk;
    }

    public double getLastBidSize() {
        return lastBidSize;
    }

    public double getLastBid() {
        return lastBid;
    }

    public double getLastAskSize() {
        return lastAskSize;
    }

    public String getProduct() {
        return product;
    }

    public LocalDateTime getLastAskTimestamp() {
        return lastAskTimestamp;
    }

    public LocalDateTime getLastBidTimestamp() {
        return lastBidTimestamp;
    }

    public boolean isHistoricalDataLoaded() {
        return historicalDataLoaded;
    }

    public void setHistoricalDataLoaded(boolean historicalDataLoaded) {
        if (historicalDataLoaded) {
            this.historicalDataLoaded = true;
            bufferedTicks.stream().forEach(t -> {
                addTick(t.priceType, t.ts, t.price, t.vol);
            });
            bufferedTicks.clear();
        } else {
            this.historicalDataLoaded = historicalDataLoaded;
        }
    }

    class Tick implements Comparable<Tick> {
        PriceType priceType;
        LocalDateTime ts;
        double price;
        double vol;

        public Tick(LocalDateTime ts, PriceType priceType, double price, double vol) {
            super();
            this.priceType = priceType;
            this.ts = ts;
            this.price = price;
            this.vol = vol;
        }

        @Override
        public int compareTo(Tick other) {
            return Long.compare(ts.toDate().getTime(), other.ts.toDate().getTime());
        }

    }

    @Override
    public String toString() {
        return "Series{" +
                "product=" + product +
                ", timeframe=" + timeframe +
                '}';
    }

    public double[] getCalculatedValues(String key) {
        Integer calculatedOnBar = this.calculatedOnBar.get(key);
        if (calculatedOnBar == null)
            return null;
        else {
            double[] values = _values.get(key);

            if (values != null && values.length - 1 > calculatedOnBar) {
                return Arrays.copyOf(values, calculatedOnBar + 1);
            } else
                return values;
        }
    }

}
