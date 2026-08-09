package com.example.restaurant.service.utils;

import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

public class UrlBuilder {
    public static String buildUrl(String baseUrl,
                                  String path,
                                  MultiValueMap<String, String> params) {
        return UriComponentsBuilder
                    .fromUriString(baseUrl)
                    .path(path)
                    .queryParams(params)
                    .toUriString();
    }
}
