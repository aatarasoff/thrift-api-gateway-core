package ru.aatarasoff.thrift.api.gateway.core;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TMemoryInputTransport;
import org.apache.thrift.transport.TTransportException;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * Created by aleksandr on 09.07.15.
 */
public class MessageTransalator {
    private static final byte[] COLON = new byte[]{(byte)58};
    private static final String ERROR_STRUCT_NAME = "result";
    private static final String ERROR_FIELD_NAME = "exception";
    private static final short ERROR_FIELD_POSITION = (short) 99;
    private static final String WRITE_METHOD_NAME = "write";
    private static final int MEMORY_BUFFER_LENGTH = 1024;

    private TProtocolFactory protocolFactory;
    private AuthTokenExchanger authTokenExchanger;

    private String methodName;
    private int seqid;

    public MessageTransalator(TProtocolFactory protocolFactory, AuthTokenExchanger authTokenExchanger) {
        this.protocolFactory = protocolFactory;
        this.authTokenExchanger = authTokenExchanger;
    }

    public byte[] process(byte[] thriftBody) throws TException {
        TProtocol protocol = createProtocol(thriftBody);

        int startPosition = findStartPosition(protocol);

        TBase userData = authTokenExchanger.process(
                extractAuthToken(protocol, authTokenExchanger.createEmptyAuthToken())
        );

        int endPosition = findEndPosition(protocol);

        return ArrayUtils.addAll(
                ArrayUtils.addAll(
                        getSkippedPart(protocol, startPosition),
                        serializeUserData(protocolFactory, userData)
                ),
                getAfterTokenPart(protocol, endPosition, thriftBody.length)
        );
    }

    public byte[] processError(TException exception) throws Exception {
        TMemoryBufferWithLength memoryBuffer = new TMemoryBufferWithLength(1024);

        TProtocol protocol = protocolFactory.getProtocol(memoryBuffer);

        try {
            throw exception;
        } catch (TApplicationException e) {
            writeTApplicationException(e, protocol);
        } catch (TProtocolException e) {
            writeTApplicationException(createApplicationException(e), protocol);
        } catch (TTransportException e) {
            writeTApplicationException(createApplicationException(e), protocol);
        } catch (TException e) {
            if (TException.class.equals(e.getClass())) {
                writeTApplicationException(createApplicationException(e), protocol);
            } else {
                writeUserDefinedException(exception, protocol);
            }
        }

        return Arrays.copyOf(memoryBuffer.getArray(), memoryBuffer.length());
    }

    private TProtocol createProtocol(byte[] thriftBody) {
        return protocolFactory.getProtocol(new TMemoryInputTransport(thriftBody));
    }

    private TBase extractAuthToken(TProtocol protocol, TBase authToken) throws TException {
        authToken.read(protocol);
        return authToken;
    }

    private byte[] serializeUserData(TProtocolFactory protocolFactory, TBase userData) throws TException {
        TMemoryBufferWithLength memoryBuffer = new TMemoryBufferWithLength(MEMORY_BUFFER_LENGTH);

        TProtocol protocol = protocolFactory.getProtocol(memoryBuffer);

        if (protocol instanceof TJSONProtocol) {
            memoryBuffer.write(COLON, 0, 1);
        }
        userData.write(protocol);

        return Arrays.copyOf(memoryBuffer.getArray(), memoryBuffer.length());
    }

    private int findStartPosition(TProtocol protocol) throws TException {
        skipMessageInfo(protocol);
        skipToFirstFieldData(protocol);

        return protocol.getTransport().getBufferPosition();
    }

    private int findEndPosition(TProtocol protocol) throws TException {
        return protocol.getTransport().getBufferPosition();
    }

    private void skipToFirstFieldData(TProtocol protocol) throws TException {
        protocol.readStructBegin();
        protocol.readFieldBegin();
    }

    private void skipMessageInfo(TProtocol protocol) throws TException {
        TMessage message = protocol.readMessageBegin();
        this.methodName = message.name;
        this.seqid = message.seqid;
    }

    private byte[] getAfterTokenPart(TProtocol protocol, int endPosition, int length) {
        return Arrays.copyOfRange(protocol.getTransport().getBuffer(), endPosition, length);
    }

    private byte[] getSkippedPart(TProtocol protocol, int startPosition) {
        return getAfterTokenPart(protocol, 0, startPosition);
    }

    private void writeTApplicationException(TApplicationException exception, TProtocol protocol) throws TException {
        protocol.writeMessageBegin(new TMessage(this.methodName, TMessageType.EXCEPTION, this.seqid));
        exception.write(protocol);
        protocol.writeMessageEnd();
    }

    private TApplicationException createApplicationException(TException e) {
        return new TApplicationException(TApplicationException.INTERNAL_ERROR, e.getMessage());
    }

    private void writeUserDefinedException(TException exception, TProtocol protocol) throws TException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        TStruct errorStruct = new TStruct(ERROR_STRUCT_NAME);
        TField errorField = new TField(ERROR_FIELD_NAME, TType.STRUCT, ERROR_FIELD_POSITION);

        protocol.writeMessageBegin(new TMessage(this.methodName, TMessageType.REPLY, this.seqid));
        protocol.writeStructBegin(errorStruct);
        protocol.writeFieldBegin(errorField);

        exception.getClass().getMethod(WRITE_METHOD_NAME, TProtocol.class).invoke(exception, protocol);

        protocol.writeFieldEnd();
        protocol.writeFieldStop();
        protocol.writeStructEnd();
        protocol.writeMessageEnd();
    }
}
