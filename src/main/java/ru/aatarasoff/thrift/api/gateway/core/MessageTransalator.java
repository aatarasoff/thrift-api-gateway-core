package ru.aatarasoff.thrift.api.gateway.core;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TMemoryInputTransport;

import java.util.Arrays;

/**
 * Created by aleksandr on 09.07.15.
 */
public class MessageTransalator {
    private static final byte[] COLON = new byte[]{(byte)58};

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

        if (TApplicationException.class.equals(exception.getClass())) {
            protocol.writeMessageBegin(new TMessage(this.methodName, TMessageType.EXCEPTION, this.seqid));

            ((TApplicationException) exception).write(protocol);

            protocol.writeMessageEnd();
        } else {
            TStruct errorStruct = new TStruct(this.methodName + "_result");
            TField errorField = new TField("exception", TType.STRUCT, (short) 99);

            protocol.writeMessageBegin(new TMessage(this.methodName, TMessageType.REPLY, this.seqid));
            protocol.writeStructBegin(errorStruct);
            protocol.writeFieldBegin(errorField);

            exception.getClass().getMethod("write", TProtocol.class).invoke(exception, protocol);

            protocol.writeFieldEnd();
            protocol.writeFieldStop();
            protocol.writeStructEnd();
            protocol.writeMessageEnd();
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
        TMemoryBufferWithLength memoryBuffer = new TMemoryBufferWithLength(1024);

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
}
