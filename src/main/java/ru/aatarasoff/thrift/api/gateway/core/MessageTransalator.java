package ru.aatarasoff.thrift.api.gateway.core;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TMemoryInputTransport;

import java.util.Arrays;

/**
 * Created by aleksandr on 09.07.15.
 */
public class MessageTransalator {
    private static final byte[] COLON = new byte[]{(byte)58};

    private TProtocolFactory protocolFactory;
    private AuthTokenExchanger authTokenExchanger;

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

        return  ArrayUtils.addAll(
                ArrayUtils.addAll(
                        Arrays.copyOfRange(protocol.getTransport().getBuffer(), 0, startPosition),
                        serializeUserData(protocolFactory, userData)
                ),
                Arrays.copyOfRange(protocol.getTransport().getBuffer(), endPosition, thriftBody.length)
        );
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
        protocol.readMessageBegin();
    }
}
