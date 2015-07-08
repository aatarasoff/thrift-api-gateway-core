package ru.aatarasoff.thrift.api.gateway.core;

import org.apache.thrift.TBase;

/**
 * Created by aleksandr on 09.07.15.
 */
public interface AuthTokenExchanger<T extends TBase, U extends  TBase> {
    T createEmptyAuthToken();

    U process(T authToken);
}
