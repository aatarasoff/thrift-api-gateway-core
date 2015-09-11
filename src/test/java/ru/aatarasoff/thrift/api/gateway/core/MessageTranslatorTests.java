package ru.aatarasoff.thrift.api.gateway.core;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TMemoryBuffer;
import org.apache.thrift.transport.TMemoryInputTransport;
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
        final TBinaryProtocol.Factory protocolFactory = new TBinaryProtocol.Factory();
        translator_test_base(protocolFactory, "sometoken", createTranslator(protocolFactory));
    }

    @Test
    public void compact_test() throws TException, IllegalAccessException, InstantiationException {
        final TCompactProtocol.Factory protocolFactory = new TCompactProtocol.Factory();
        translator_test_base(protocolFactory, "sometoken", createTranslator(protocolFactory));
    }

    @Test
    public void json_test() throws TException, IllegalAccessException, InstantiationException {
        final TJSONProtocol.Factory protocolFactory = new TJSONProtocol.Factory();
        translator_test_base(protocolFactory, "sometoken", createTranslator(protocolFactory));
    }

    @Test(expected = UnauthorizedException.class)
    public void binary_wrong_token_test() throws Exception {
        wrong_token_test(new TBinaryProtocol.Factory());
    }

    @Test(expected = UnauthorizedException.class)
    public void compact_wrong_token_test() throws Exception {
        wrong_token_test(new TBinaryProtocol.Factory());
    }

    @Test(expected = UnauthorizedException.class)
    public void json_wrong_token_test() throws Exception {
        wrong_token_test(new TBinaryProtocol.Factory());
    }

    @Test(expected = TApplicationException.class)
    public void binary_fatal_token_test() throws Exception {
        fatal_token_test(new TBinaryProtocol.Factory());
    }

    @Test(expected = TApplicationException.class)
    public void compact_fatal_token_test() throws Exception {
        fatal_token_test(new TBinaryProtocol.Factory());
    }

    @Test(expected = TApplicationException.class)
    public void json_fatal_token_test() throws Exception {
        fatal_token_test(new TBinaryProtocol.Factory());
    }

    public void wrong_token_test(TProtocolFactory protocolFactory) throws Exception {
        MessageTransalator translator = createTranslator(protocolFactory);

        try {
            translator_test_base(protocolFactory, "wrongtoken", translator);
        } catch (UnauthorizedException e) {
            processError(protocolFactory, translator, e);
        }
    }

    public void fatal_token_test(TProtocolFactory protocolFactory) throws Exception {
        MessageTransalator translator = createTranslator(protocolFactory);

        try {
            translator_test_base(protocolFactory, "fataltoken", translator);
        } catch (TException e) {
            processError(protocolFactory, translator, e);
        }
    }

    private void translator_test_base(TProtocolFactory protocolFactory, final String token, MessageTransalator translator) throws TException, InstantiationException, IllegalAccessException {
        TMemoryBuffer externalServiceBuffer = new TMemoryBufferWithLength(1024);

        ExternalTestService.Client externalServiceClient
                = new ExternalTestService.Client(protocolFactory.getProtocol(externalServiceBuffer));

        externalServiceClient.send_getSomeData(
                new AuthToken().setToken(token).setChecksum(128),
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
                        translator.process(externalServiceMessage),
                        internalServiceMessage
                )
        );
    }

    private void processError(TProtocolFactory protocolFactory, MessageTransalator translator, TException e) throws Exception {
        byte[] thriftBody = translator.processError(e);

        ExternalTestService.Client externalServiceClient = new ExternalTestService.Client(
                protocolFactory.getProtocol(new TMemoryInputTransport(thriftBody)),
                protocolFactory.getProtocol(new TMemoryBufferWithLength(1024))
        );

        externalServiceClient.send_getSomeData(
                new AuthToken().setToken("token").setChecksum(128),
                new RequestData().setSomeStringField("somevalue").setSomeIntField(8)
        );

        externalServiceClient.recv_getSomeData();
    }

    private MessageTransalator createTranslator(TProtocolFactory protocolFactory) {
        return new MessageTransalator(protocolFactory, new AuthTokenExchanger<AuthToken, UserData>() {
            @Override
            public AuthToken createEmptyAuthToken() {
                return new AuthToken();
            }

            @Override
            public UserData process(AuthToken authToken) throws TException {
                if ("sometoken".equals(authToken.getToken())) {
                    return new UserData().setId("user1");
                }

                if ("fataltoken".equals(authToken.getToken())) {
                    throw new TApplicationException(TApplicationException.INTERNAL_ERROR, "fatal!!!");
                }

                throw new UnauthorizedException("token is invalid");
            }
        });
    }
}
