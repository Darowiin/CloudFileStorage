package ru.util;

import ru.exception.InvalidResourcePathException;

public class PathUtils {
    public static boolean isDirectory(String path) {
        if (path == null || path.isEmpty() || path.endsWith("/")) {
            return true;
        }

        String fileName = getFileName(path);

        return !fileName.contains(".");
    }

    public static String getParentPath(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "";
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        int lastSlash = path.lastIndexOf('/');
        if (lastSlash == -1) {
            return "";
        }

        return lastSlash == 0 ? "" : path.substring(0, lastSlash + 1);
    }

    public static String getFileName(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "";
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        int lastSlash = path.lastIndexOf('/');
        return lastSlash > -1 ? path.substring(lastSlash + 1) : path;
    }

    public static String getUserDirectory(int userId) {
        return String.format("user-%d-files/", userId);
    }

    public static String getFullPath(int userId, String path) {
        String userDir = getUserDirectory(userId);

        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }

        return userDir + path;
    }

    public static void checkPath(String path) {
        if (path == null) {
            throw new InvalidResourcePathException("Path cannot be null");
        }

        String invalidChars = "\\:*?\"<>|";
        for (char c : invalidChars.toCharArray()) {
            if (path.contains(String.valueOf(c))) {
                throw new InvalidResourcePathException("The path contains invalid characters: " + c);
            }
        }

        if (path.contains("..") || path.contains("//")) {
            throw new InvalidResourcePathException("The path contains invalid sequence of characters: " + path);
        }
    }
}
