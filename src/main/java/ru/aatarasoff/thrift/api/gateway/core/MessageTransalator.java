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
    private AuthenticationDataExchanger authenticationDataExchanger;

    public MessageTransalator(TProtocolFactory protocolFactory, AuthenticationDataExchanger authenticationDataExchanger) {
        this.protocolFactory = protocolFactory;
        this.authenticationDataExchanger = authenticationDataExchanger;
    }

    public byte[] process(byte[] thriftBody) throws TException, InstantiationException, IllegalAccessException {
        TProtocol protocol = createProtocol(thriftBody);

        int startPosition = findStartPosition(protocol);

        TBase userData = authenticationDataExchanger.process(
                extractAuthenticationData(protocol, authenticationDataExchanger.getAuthenticationDataClass())
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

    private byte[] serializeUserData(TProtocolFactory protocolFactory, TBase userData) throws TException {
        TMemoryBufferWithLength memoryBuffer = new TMemoryBufferWithLength(1024);

        TProtocol protocol = protocolFactory.getProtocol(memoryBuffer);

        if (protocol instanceof TJSONProtocol) {
            memoryBuffer.write(COLON, 0, 1);
        }
        userData.write(protocol);

        return Arrays.copyOf(memoryBuffer.getArray(), memoryBuffer.length());
    }

    private TProtocol createProtocol(byte[] thriftBody) {
        return protocolFactory.getProtocol(new TMemoryInputTransport(thriftBody));
    }

    private TBase extractAuthenticationData(TProtocol protocol, Class<TBase> authenticationDataClass) throws TException, IllegalAccessException, InstantiationException {
        TBase authData = authenticationDataClass.newInstance();
        authData.read(protocol);
        return authData;
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
