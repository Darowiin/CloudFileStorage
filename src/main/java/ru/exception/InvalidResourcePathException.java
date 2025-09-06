package ru.exception;

public class InvalidResourcePathException extends RuntimeException {
    public InvalidResourcePathException(String message) {
        super(message);
    }
}
