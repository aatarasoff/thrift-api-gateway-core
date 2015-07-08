namespace java ru.aatarasoff.thrift

exception SomeException {
    1: string code
}

service ExternalTestService {
    SomeReturnData getSomeData(
        1: AuthenticationData authData,
        2: RequestData requestData
    ) throws (1: SomeException e);
}

service InternalTestService {
    SomeReturnData getSomeData(
        1: UserData userData,
        2: RequestData requestData
    ) throws (1: SomeException e);
}

struct SomeReturnData {
    1: string someStringField,
    2: i32 someIntField
}

struct RequestData {
    1: string someStringField,
    2: i32 someIntField
}

struct AuthenticationData {
    1: string token,
    2: i32 checksum
}

struct UserData {
    1: string id
}