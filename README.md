# Thrift API Gateway Core

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
    ) throws (1: SomeException e);
}
```

As you see, difference beetween two services only in the first argument.

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
compile 'ru.aatarasoff.thrift:api-gateway-core:0.1.1'
```

## How use this

You need to create MessageTransalator and call process method. 
Also you need implement AuthTokenExchanger interface.

See tests for better understanding.

## Enjoy!