package com.klein.ta;

import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.*;
import java.util.List;

public class StrategyInput {

    DecimalFormat df = new DecimalFormat("0.#####", DecimalFormatSymbols.getInstance(Locale.US));
    private String name;
    private String description;
    private InputType type;
    private Object value;
    private Object defaultValue;
    private boolean optimization = false;
    private double minValue;
    private double maxValue;
    private double step;
    private List<String> messages = new ArrayList<>();

    public StrategyInput(String name, InputType type, Object defaultValue) {
        super();
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public StrategyInput(String name, InputType type, Object defaultValue, double min, double max, double step) {
        this(name, type, defaultValue);
        this.optimization = true;
        this.minValue = min;
        this.maxValue = max;
        this.step = step;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        if (type == InputType.INT)
            return getIntValue();
        if (type == InputType.DECIMAL)
            return getDoubleValue();
        else
            return value;
    }

    public void setValue(Object val) {
        this.value = val;
    }

    public InputType getType() {
        return type;
    }

    public void setType(InputType type) {
        this.type = type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValueAsString() {
        return getValueAsString(value);
    }

    public void setValueAsString(String val) {
        if (val == null)
            return;

        switch (type) {
            case DECIMAL:
                try {
                    value = parse(val).doubleValue();
                    break;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            case INT:
                try {
                    value = parse(val).intValue();
                    break;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            default:
                value = val.toString();
                break;
        }
    }

    public Number parse(String val) throws ParseException {
        return df.parse(val);
    }

    public String getDefaultValueAsString() {
        return getValueAsString(defaultValue);
    }

    public String getValueAsString(Object val) {
        if (val == null)
            return null;

        switch (type) {
            case DECIMAL:
                if (val instanceof BigDecimal)
                    return df.format(((BigDecimal) val).doubleValue());
                else
                    return df.format((double) val);
            case INT:
                return df.format(val);
            default:
                return val != null ? val.toString() : null;
        }
    }

    public boolean isString() {
        return InputType.STRING == type;
    }

    public boolean isInteger() {
        return InputType.INT == type;
    }

    public boolean isDecimal() {
        return InputType.DECIMAL == type;
    }

    public boolean isOptimization() {
        return optimization;
    }

    public void setOptimization(boolean optimization) {
        this.optimization = optimization;
    }

    public Object getMinValue() {
        return minValue;
    }

    public void setMinValue(Object value) {
        if (value instanceof Integer)
            this.minValue = (Integer) value;
        else if (value instanceof BigDecimal)
            this.minValue = ((BigDecimal) value).doubleValue();
        else if (value instanceof Double)
            this.minValue = (double) value;
        else
            throw new RuntimeException("Unsupported type: " + value.getClass().getSimpleName());
    }

    public Object getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Object value) {
        if (value instanceof Integer)
            this.maxValue = (Integer) value;
        else if (value instanceof BigDecimal)
            this.maxValue = ((BigDecimal) value).doubleValue();
        else if (value instanceof Double)
            this.maxValue = (double) value;
        else
            throw new RuntimeException("Unsupported type: " + value.getClass().getSimpleName());
    }

    public Object getStep() {
        return step;
    }

    public void setStep(Object value) {
//        System.out.println("Setting step for " + getName() + ": " + value);
        if (value instanceof Integer)
            this.step = (Integer) value;
        else if (value instanceof BigDecimal)
            this.step = ((BigDecimal) value).doubleValue();
        else if (value instanceof Double)
            this.step = (double) value;
        else
            throw new RuntimeException("Unsupported type: " + value.getClass().getSimpleName());
//        System.out.println("Setting step for " + getName() + ": " + this.step);
    }

    public int getValuesCount() {
        if (optimization && maxValue - minValue > 0)
            return (int) ((maxValue - minValue) / step);
        else
            return 0;
    }

    public boolean isOptimizationValuesValide() {
        messages.clear();

        if (!optimization)
            return true;

        if (step == 0) {
            messages.add("Step is " + step);
            return false;
        }

        if (maxValue - minValue <= 0) {
            messages.add("Min > Max");
            return false;
        }

        if (step * 2 > (maxValue - minValue + 1 * step)) {
            messages.add("Step is too high");
            return false;
        }

        return true;
    }

    public double getDoubleValue() {
        if (value == null)
            return 0D;

        if (type == InputType.DECIMAL) {
            if (value instanceof BigDecimal)
                return ((BigDecimal) value).doubleValue();
            else if (value instanceof Double)
                return (Double) value;
            if (value instanceof Integer)
                return ((Integer) value).doubleValue();
        }
        return Double.MIN_VALUE;
    }

    public void setDoubleValue(double value) {
        this.value = value;
    }

    public int getIntValue() {
        if (value == null)
            return 0;

        if (type == InputType.INT)
            if (value instanceof Double)
                return ((Double) value).intValue();
            else if (value instanceof Long)
                return ((Long) value).intValue();
            else
                return (Integer) value;
        else
            return Integer.MIN_VALUE;
    }

    public void setIntValue(int value) {
        this.value = value;
    }

    public List<String> getMessages() {
        return messages;
    }

    public String getMessagesAsText() {
        String out = "";
        for (String message : messages) {
            out += message + "\n";
        }
        return out;
    }
}
