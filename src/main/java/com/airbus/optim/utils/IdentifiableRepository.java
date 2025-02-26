package com.airbus.optim.utils;

public interface IdentifiableRepository<T> {
    Long findNextAvailableId();
}
