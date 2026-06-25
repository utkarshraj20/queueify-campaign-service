package com.queueify.campaignservice.common.util;

import java.security.SecureRandom;

public class OtpUtil {
    private static final SecureRandom random = new SecureRandom();

    int randomNumber = random.nextInt(900000) + 100000;

    public static String generateOtp() {
        StringBuilder randomNumber = new StringBuilder();
        randomNumber.append(random.nextInt(900000) + 100000);
        return randomNumber.toString();
    }

}
