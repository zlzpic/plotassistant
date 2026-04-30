package com.bdu.plotassistant.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ServiceUtil {

    public static void requireNonNull(Object obj, String message) {
        if (obj == null) {
            throw new BizException(message);
        }
    }

    public static void requireNonNull(Object... objs) {
        for (Object obj : objs) {
            if (obj == null) {
                throw new BizException("参数不能为空");
            }
        }
    }

    public static int calculateOffset(int page, int size) {
        if (page < 1) {
            page = 1;
        }
        return (page - 1) * size;
    }

    public static int calculatePages(long total, int size) {
        if (size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / size);
    }

    public static String formatDateTime(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }
}
