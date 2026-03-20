package com.tripdeva.testapp.dto;

public record ErrorResponse(int status, String code, String message) {
}
