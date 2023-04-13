package org.jenkins.plugins.yc.exception;

public class LaunchScriptException extends RuntimeException{
    public LaunchScriptException(Throwable cause){
        super("An error occurred while executing the script\n", cause);
    }
}
