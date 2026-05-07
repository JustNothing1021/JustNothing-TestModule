package com.justnothing.testmodule.command.base.protocol;

import java.util.ArrayList;

public interface ValueSupplier {
    Object get();
    
    class EmptyListSupplier implements ValueSupplier {
        @Override
        public Object get() {
            return new ArrayList<>();
        }
    }
    
    class ZeroSupplier implements ValueSupplier {
        @Override
        public Object get() {
            return 0;
        }
    }
    
    class FalseSupplier implements ValueSupplier {
        @Override
        public Object get() {
            return false;
        }
    }
    
    class EmptyStringSupplier implements ValueSupplier {
        @Override
        public Object get() {
            return "";
        }
    }
}
