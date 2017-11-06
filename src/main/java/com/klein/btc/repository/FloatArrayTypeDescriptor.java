package com.klein.btc.repository;

import com.vladmihalcea.hibernate.type.array.internal.AbstractArrayTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
public class FloatArrayTypeDescriptor
        extends AbstractArrayTypeDescriptor<float[]> {

    public static final FloatArrayTypeDescriptor INSTANCE = new FloatArrayTypeDescriptor();

    public FloatArrayTypeDescriptor() {
        super(float[].class);
    }

    @Override
    protected String getSqlArrayType() {
        return "float";
    }
}
