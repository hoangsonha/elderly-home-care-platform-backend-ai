package com.capstone_project.elderly_platform.utils;

import java.security.SecureRandom;

public class StringUtils {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String ALPHABETIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String NUMERIC = "0123456789";
    private static final SecureRandom random = new SecureRandom();

    /**
     * Tạo chuỗi random với độ dài cho trước
     * Sử dụng ký tự chữ và số (alphanumeric)
     *
     * @param length Độ dài của chuỗi cần tạo
     * @return Chuỗi random
     */
    public static String generateRandomString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }
        return generateRandomString(length, ALPHANUMERIC);
    }

    /**
     * Tạo chuỗi random chỉ chứa chữ cái
     *
     * @param length Độ dài của chuỗi cần tạo
     * @return Chuỗi random chỉ chứa chữ cái
     */
    public static String generateRandomAlphabetic(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }
        return generateRandomString(length, ALPHABETIC);
    }

    /**
     * Tạo chuỗi random chỉ chứa số
     *
     * @param length Độ dài của chuỗi cần tạo
     * @return Chuỗi random chỉ chứa số
     */
    public static String generateRandomNumeric(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }
        return generateRandomString(length, NUMERIC);
    }

    /**
     * Tạo chuỗi random từ một tập ký tự tùy chỉnh
     *
     * @param length Độ dài của chuỗi cần tạo
     * @param characters Tập ký tự để tạo chuỗi
     * @return Chuỗi random
     */
    public static String generateRandomString(int length, String characters) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }
        if (characters == null || characters.isEmpty()) {
            throw new IllegalArgumentException("Characters must not be null or empty");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }
}

