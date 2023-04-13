package org.jenkins.plugins.yc.exception;

public class LoginFailed extends RuntimeException{
    public LoginFailed(String cause){
        super(cause);
    }
}
