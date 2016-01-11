# Thrift API Gateway Core

[![Join the chat at https://gitter.im/aatarasoff/thrift-api-gateway-core](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/aatarasoff/thrift-api-gateway-core?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Sometimes you need to do gateway for your API.
This project provides core functionality for this.

## What is it

Imagine that you have a thrift service like this:

```
service InternalTestService {
    SomeReturnData getSomeData(
        1: UserData userData,
        2: RequestData requestData
    ) throws (1: SomeException e);
}
```

So, you have some user data that identifies him, for example id.
But external client (mobile, web, other service) has only authentication token.
And you want to build gateway that would be proxy request and translate message into your internal service.

So, you need to publish to client other service: 

```
service ExternalTestService {
    SomeReturnData getSomeData(
        1: AuthToken authData,
        2: RequestData requestData
    ) throws (1: SomeException e, 99: UnauthorizedException ue);
}
```

As you see, difference beetween two services in the first argument and unauthorized exception as 99 field (I hope nobody needs more than 98 exceptions :) ).

MessageTranslator can get thrift message from external service and transform it into internal service message.

## How connect project

Its very simple:

```
repositories {
    maven {
        url  "http://dl.bintray.com/aatarasoff/maven" 
    }
}
```

```
compile 'ru.aatarasoff.thrift:api-gateway-core:0.2.1'
```

## How use this

You need to create MessageTransalator and call process method. 
Also you need implement AuthTokenExchanger interface.

Message Translator is not thread safe. Remember that.
If you have an exception while method process is called, you can process it with processError method.

See tests for better understanding.

## Enjoy!