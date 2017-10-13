package com.klein.ta.funcs;

import com.klein.ta.InputType;
import com.klein.ta.Series;
import com.klein.ta.StrategyInput;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractTaFunction {

    protected Core core;
    private Logger LOG;
    private Series series;

    public AbstractTaFunction(Series series) {
        super();
        this.series = series;
        core = new Core();
        loadDefaults(this);
        init();
    }

    private void loadDefaults(Object object) {
        Class clazz = object.getClass();
        for (Field f : clazz.getDeclaredFields()) {
            FunctionInput[] annotations = f.getAnnotationsByType(FunctionInput.class);
            if (annotations != null && annotations.length > 0) {
                FunctionInput annotation = annotations[0];
                DefaultIntValue[] defaultIntValues = f.getAnnotationsByType(DefaultIntValue.class);
                if (defaultIntValues != null && defaultIntValues.length > 0)
                    setValue(f, object, defaultIntValues[0].value());
                DefaultDoubleValue[] defaultDoubleValues = f.getAnnotationsByType(DefaultDoubleValue.class);
                if (defaultDoubleValues != null && defaultDoubleValues.length > 0)
                    setValue(f, object, defaultDoubleValues[0].value());
            }
        }
    }

    public List<StrategyInput> getInputs() {
        return getInputs(this.getClass());
    }

    public static List<StrategyInput> getInputs(Class clazz) {
        List<StrategyInput> inputs = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            FunctionInput[] annotations = f.getAnnotationsByType(FunctionInput.class);
            if (annotations != null && annotations.length > 0) {
                FunctionInput annotation = annotations[0];
                DefaultIntValue[] defaultIntValues = f.getAnnotationsByType(DefaultIntValue.class);
                Object defaultValue = null;
                if (defaultIntValues != null && defaultIntValues.length > 0)
                    defaultValue = defaultIntValues[0].value();
                DefaultDoubleValue[] defaultDoubleValues = f.getAnnotationsByType(DefaultDoubleValue.class);
                if (defaultDoubleValues != null && defaultDoubleValues.length > 0)
                    defaultValue = defaultDoubleValues[0].value();
                InputType type = InputType.STRING;
                String className = f.getType().getSimpleName().toLowerCase();
                if (className.equals("int") || className.equals("integer") || className.equals("long"))
                    type = InputType.INT;
                else if (className.equals("double") || className.equals("float"))
                    type = InputType.DECIMAL;
                else if (className.equals("series"))
                    type = InputType.SERIES;
                else if (isAssignable(f.getType(), AbstractTaFunction.class))
                    type = InputType.TA_FUNC;

                System.out.println("Input [" + f.getName() + "] type: " + type);
                StrategyInput input = new StrategyInput(f.getName(), type, defaultValue);
                inputs.add(input);
            }
        }
        return inputs;
    }

    private static boolean isAssignable(Class<?> type, Class superClass) {
        Class clazz = type;
        while (clazz != null) {
            if (type.getSimpleName().equals(superClass.getSimpleName()))
                return true;

            clazz = type.getSuperclass();
        }
        return false;
    }

    public void setValue(String fieldName, Object value) {
//        System.out.println("Setting field "+fieldName+": "+value);
        try {
            Field field = getClass().getDeclaredField(fieldName);
            setValue(field, this, value);
            init();
        } catch (NoSuchFieldException e) {
            String setterName = "set" + new String(fieldName.substring(0, 1)).toUpperCase() + fieldName.substring(1, fieldName.length());
//            System.out.println(setterName);
            try {
                Method setter = getClass().getMethod(setterName, value.getClass());
                setter.invoke(this, value);
            } catch (Exception e1) {
                System.err.println("No field or setter available for [" + fieldName + "]");
            }
        }
    }

    public Object getValue(String fieldName) {
//        System.out.println("Setting field "+fieldName+": "+value);
        try {
            Field field = getClass().getDeclaredField(fieldName);
            return getValue(field, this);
        } catch (NoSuchFieldException e) {
            String getterName = "get" + new String(fieldName.substring(0, 1)).toUpperCase() + fieldName.substring(1, fieldName.length());
//            System.out.println(setterName);
            try {
                Method getter = getClass().getMethod(getterName);
                return getter.invoke(this);
            } catch (Exception e1) {
                System.err.println("No field or setter available for [" + fieldName + "]");
            }
        }
        return null;
    }

    public void setValue(Field f, Object object, Object value) {
        try {
            f.set(object, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public Object getValue(Field f, Object object) {
        try {
            return f.get(object);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected abstract String getKey();

    public int getLoookbackBars() {
        return 0;
    }

    protected void printValues(String prefix, double[] values, int decimal) {
        DecimalFormat sd = new DecimalFormat();
        sd.setMaximumIntegerDigits(1);
        sd.setMaximumFractionDigits(decimal);
        sd.setMinimumFractionDigits(decimal);
        StringBuffer sb = new StringBuffer(values.length * (3 + decimal));
        sb.append("[");
        for (double value : values) {
            sb.append(sd.format(value));
            sb.append(" ");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        System.out.println(prefix + " " + sb.toString());
    }

    public abstract String getShortName();

    public String getName() {
        return getShortName() + " " + getSeries().getSymbol();
    }

    protected abstract double[] calc();

    public double value() {
        return values()[values().length - 1];
    }

    public double[] values() {
        if (series.needRecalc(getKey())) {
            getLog().trace("Recalc needed");
            double[] data = calc();
            getLog().trace("Calc results: " + (data != null ? Arrays.toString(data) : "NULL"));
            series.setValues(getKey(), data);
            return data;
        } else {
            getLog().trace("No recalc needed");
            return series.getValues(getKey());
        }
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series _s) {
        this.series = _s;
    }

    protected void logError(String string, Object... params) {
        getLog().error(string, params);
    }

    private Logger getLog() {
        if (LOG == null) {
            LOG = LoggerFactory.getLogger("ta." + getKey());
        }
        return LOG;
    }

    protected double[] createFullArray(MInteger outBegIdx, MInteger outNBElement, double[] roc, double emptyValue) {
//        System.out.println("outBegIdx: " + outBegIdx.value);
//        System.out.println("outNBElement: " + outNBElement.value);
//        System.out.println("IN: " + Arrays.toString(roc));
        roc = Arrays.copyOf(roc, outNBElement.value);
//        System.out.println("COPY IN: " + Arrays.toString(roc));

        double[] fullRoc = new double[getSeries().getLastIndex() + 1];
        Arrays.fill(fullRoc, 0, outBegIdx.value, emptyValue);
        System.arraycopy(roc, 0, fullRoc, outBegIdx.value, roc.length);
//        System.out.println("OUT: " + Arrays.toString(fullRoc));
        return fullRoc;
    }

    protected double[] createFullArray(double[] oldValues, MInteger outBegIdx, MInteger outNBElement, double[] roc, double emptyValue) {
        return createFullArray(outBegIdx, outNBElement, roc, emptyValue);
//        if (oldValues == null) {
//        }
//
//        System.out.println("outBegIdx: " + outBegIdx.value);
//        System.out.println("outNBElement: " + outNBElement.value);
//
//        System.out.println("IN: " + Arrays.toString(roc));
//        roc = Arrays.copyOf(roc, outNBElement.value);
//        System.out.println("COPY IN: " + Arrays.toString(roc));
//
//        double[] fullRoc = new double[getSeries().getLastIndex() + 1];
//        Arrays.fill(fullRoc, 0, outBegIdx.value, emptyValue);
//        System.arraycopy(roc, 0, fullRoc, outBegIdx.value, roc.length);
//        System.out.println("OUT: " + Arrays.toString(fullRoc));
//        return fullRoc;
    }

    public void init() {
    }

//    public void dumpValues() {
//        System.out.println(getKey() + ": " + Arrays.toString(values()));
//    }
}
