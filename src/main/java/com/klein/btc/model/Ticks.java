package com.klein.btc.model;

import com.klein.btc.repository.FloatArrayType;
import com.vladmihalcea.hibernate.type.array.IntArrayType;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.util.Arrays;

/**
 * Created by mresc on 05.11.17.
 */
@TypeDefs({
        @TypeDef(
                name = "string-array",
                typeClass = StringArrayType.class
        ),
        @TypeDef(
                name = "int-array",
                typeClass = IntArrayType.class
        ),
        @TypeDef(
                name = "float-array",
                typeClass = FloatArrayType.class
        )
})
@Entity
public class Ticks {

    @EmbeddedId
    private TicksId id;
    @Type( type = "int-array" )
    @Column(
            name = "ticks_millis",
            columnDefinition = "array"
    )
    private int[] ticksMillis;
    @Type( type = "float-array" )
    @Column(
            name = "ticks_best_ask",
            columnDefinition = "array"
    )
    private float[] ticksBestAsk;
    @Type( type = "float-array" )
    @Column(
            name = "ticks_best_bid",
            columnDefinition = "array"
    )
    private float[] ticksBestBid;
    @Type( type = "float-array" )
    @Column(
            name = "ticks_last_price",
            columnDefinition = "array"
    )
    private float[] ticksLastPrice;
    @Type( type = "float-array" )
    @Column(
            name = "ticks_last_volume",
            columnDefinition = "array"
    )
    private float[] ticksLastVolume;

    public TicksId getId() {
        return id;
    }

    public void setId(TicksId id) {
        this.id = id;
    }

    public int[] getTicksMillis() {
        return ticksMillis;
    }

    public void setTicksMillis(int[] ticksMillis) {
        this.ticksMillis = ticksMillis;
    }

    public float[] getTicksBestAsk() {
        return ticksBestAsk;
    }

    public void setTicksBestAsk(float[] ticksBestAsk) {
        this.ticksBestAsk = ticksBestAsk;
    }

    public float[] getTicksBestBid() {
        return ticksBestBid;
    }

    public void setTicksBestBid(float[] ticksBestBid) {
        this.ticksBestBid = ticksBestBid;
    }

    public float[] getTicksLastPrice() {
        return ticksLastPrice;
    }

    public void setTicksLastPrice(float[] ticksLastPrice) {
        this.ticksLastPrice = ticksLastPrice;
    }

    public float[] getTicksLastVolume() {
        return ticksLastVolume;
    }

    public void setTicksLastVolume(float[] ticksLastVolume) {
        this.ticksLastVolume = ticksLastVolume;
    }

    public void addTick(int millis, float bestAsk, float bestBid, float lastPrice, float lastVolume){
        if (ticksMillis==null){
            ticksMillis=new int[1];
            ticksBestAsk=new float[1];
            ticksBestBid=new float[1];
            ticksLastPrice=new float[1];
            ticksLastVolume=new float[1];
        } else {
            int newLength=ticksMillis.length+1;
            ticksMillis=Arrays.copyOf(ticksMillis, newLength);
            ticksBestAsk=Arrays.copyOf(ticksBestAsk, newLength);
            ticksBestBid=Arrays.copyOf(ticksBestBid, newLength);
            ticksLastPrice=Arrays.copyOf(ticksLastPrice, newLength);
            ticksLastVolume=Arrays.copyOf(ticksLastVolume, newLength);
        }
        int lastIndex = ticksMillis.length - 1;
        ticksMillis[lastIndex]=millis;
        ticksBestAsk[lastIndex]=bestAsk;
        ticksBestBid[lastIndex]=bestBid;
        ticksLastPrice[lastIndex]=lastPrice;
        ticksLastVolume[lastIndex]= lastVolume;
    }

    public float getOpenAsk(){
        if (ticksMillis.length>0)
            return ticksBestAsk[0];
        else
            return -1f;
    }

    public float getOpenBid(){
        if (ticksMillis.length>0)
            return ticksBestBid[0];
        else
            return -1f;
    }
}
