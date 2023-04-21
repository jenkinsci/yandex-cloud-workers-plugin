package org.jenkins.plugins.yc.exception;

public class YandexClientException extends RuntimeException{

    public YandexClientException(String message){
        super(message);
    }

    public YandexClientException(String message, Throwable e){
        super(message, e);
    }
}
