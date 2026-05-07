package com.justnothing.testmodule.command.base.protocol;

public class NoDefaultSupplier implements ValueSupplier {
    @Override
    public Object get() {
        throw new UnsupportedOperationException("No default value specified");
    }
    
    public static final NoDefaultSupplier INSTANCE = new NoDefaultSupplier();
}
