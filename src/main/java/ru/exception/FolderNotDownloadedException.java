package ru.exception;

public class FolderNotDownloadedException extends RuntimeException {
    public FolderNotDownloadedException(String message) {
        super(message);
    }
}
