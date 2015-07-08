package ru.aatarasoff.thrift.api.gateway.core;

import org.apache.thrift.TBase;

/**
 * Created by aleksandr on 09.07.15.
 */
public interface AuthenticationDataExchanger<T extends TBase, U extends  TBase> {
    Class<T> getAuthenticationDataClass();

    U process(T authenticationData);
}
