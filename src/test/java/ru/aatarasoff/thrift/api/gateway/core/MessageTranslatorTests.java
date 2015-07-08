package ru.aatarasoff.thrift.api.gateway.core;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TMemoryBuffer;
import org.junit.Assert;
import org.junit.Test;
import ru.aatarasoff.thrift.*;

import java.util.Arrays;

/**
 * Created by aleksandr on 08.07.15.
 */
public class MessageTranslatorTests {

    @Test
    public void binary_test() throws TException, IllegalAccessException, InstantiationException {
        translator_test_base(new TBinaryProtocol.Factory());
    }

    @Test
    public void compact_test() throws TException, IllegalAccessException, InstantiationException {
        translator_test_base(new TCompactProtocol.Factory());
    }

    @Test
    public void json_test() throws TException, IllegalAccessException, InstantiationException {
        translator_test_base(new TJSONProtocol.Factory());
    }

    private void translator_test_base(TProtocolFactory protocolFactory) throws TException, InstantiationException, IllegalAccessException {
        TMemoryBuffer externalServiceBuffer = new TMemoryBufferWithLength(1024);

        ExternalTestService.Client externalServiceClient
                = new ExternalTestService.Client(protocolFactory.getProtocol(externalServiceBuffer));

        externalServiceClient.send_getSomeData(
                new AuthToken().setToken("sometoken").setChecksum(128),
                new RequestData().setSomeStringField("somevalue").setSomeIntField(8)
        );

        TMemoryBuffer internalServiceBuffer = new TMemoryBufferWithLength(1024);

        InternalTestService.Client internalServiceClient
                = new InternalTestService.Client(protocolFactory.getProtocol(internalServiceBuffer));

        internalServiceClient.send_getSomeData(
                new UserData().setId("user1"),
                new RequestData().setSomeStringField("somevalue").setSomeIntField(8)
        );

        byte[] externalServiceMessage = Arrays.copyOf(externalServiceBuffer.getArray(), externalServiceBuffer.length());
        byte[] internalServiceMessage = Arrays.copyOf(internalServiceBuffer.getArray(), internalServiceBuffer.length());

        Assert.assertTrue(
                "Translated external message must be the same as internal message",
                Arrays.equals(
                        new MessageTransalator(protocolFactory, new AuthTokenExchanger<AuthToken, UserData>() {
                            @Override
                            public AuthToken createEmptyAuthToken() {
                                return new AuthToken();
                            }

                            @Override
                            public UserData process(AuthToken authToken) {
                                if ("sometoken".equals(authToken.getToken())) {
                                    return new UserData().setId("user1");
                                }

                                throw new RuntimeException("token is invalid");
                            }
                        }).process(externalServiceMessage),
                        internalServiceMessage
                )
        );
    }
}
