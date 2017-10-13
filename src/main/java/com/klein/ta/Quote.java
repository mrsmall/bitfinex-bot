package com.klein.ta;

import java.io.Serializable;
import java.util.Date;

public class Quote implements Serializable {

	private Long id;
	private String product;
	private Timeframe timeframe=Timeframe.NA;
	private Date date;
	private Double open;
	private Double high;
	private Double low;
	private Double close;
	private Double volume;


	public Quote(String product, Timeframe t, Date date, Double open,
				 Double high, Double low, Double close, Double volume) {
		this(product, t);
		this.date = date;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.volume = volume;
	}

	public Quote(String product, Timeframe t) {
		this();
		this.product = product;
		this.timeframe = t;
	}

	public Quote() {
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public String getProduct() {
		return product;
	}
	public void setProduct(String product) {
		this.product = product;
	}

	public Timeframe getTimeframe() {
		return timeframe;
	}
	public void setTimeframe(Timeframe t) {
		this.timeframe = t;
	}

	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}

	public Double getOpen() {
		return open;
	}
	public void setOpen(Double open) {
		this.open = open;
	}

	public Double getHigh() {
		return high;
	}
	public void setHigh(Double high) {
		this.high = high;
	}

	public Double getLow() {
		return low;
	}
	public void setLow(Double low) {
		this.low = low;
	}

	public Double getClose() {
		return close;
	}
	public void setClose(Double close) {
		this.close = close;
	}

	public Double getVolume() {
		return volume;
	}
	public void setVolume(Double volume) {
		this.volume = volume;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Quote)) {
			return false;
		}
		Quote other = (Quote) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Quote [product=" + product + ", timeframe=" + timeframe + ", date=" + date + ", open=" + open
				+ ", high=" + high + ", low=" + low + ", close=" + close + ", volume=" + volume + "]";
	}

}
