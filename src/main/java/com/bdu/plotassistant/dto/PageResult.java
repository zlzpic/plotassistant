package com.bdu.plotassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {
    private int code;
    private String msg;
    private List<T> data;
    private long total;
    private int page;
    private int size;
    private int pages;

    public static <T> PageResult<T> success(List<T> data, long total, int page, int size) {
        int pages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PageResult<>(200, "OK", data, total, page, size, pages);
    }
}
