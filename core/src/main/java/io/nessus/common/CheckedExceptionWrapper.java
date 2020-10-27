package io.nessus.common;

@SuppressWarnings("serial")
public class CheckedExceptionWrapper extends RuntimeException {

    public static RuntimeException create(Throwable cause) {
        
    	if (cause instanceof RuntimeException) 
        	return (RuntimeException) cause;
    	
        return new CheckedExceptionWrapper(cause);
    }
    
    public CheckedExceptionWrapper(Throwable cause) {
        super(cause);
    }
}
