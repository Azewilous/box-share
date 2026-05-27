package org.azelabs.boxshare.application.interfaces;

public interface ICommand<T, R> {
    R handle(T request);
}
