package ru.aatarasoff.thrift.api.gateway.core;

import org.apache.thrift.transport.TMemoryBuffer;

/**
 * Created by aleksandr on 09.07.15.
 */
public class TMemoryBufferWithLength  extends TMemoryBuffer {
    private int actualLength = 0;

    public TMemoryBufferWithLength(int size) {
        super(size);
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        super.write(buf, off, len);
        actualLength += len;
    }

    @Override
    public int length() {
        return actualLength;
    }
}