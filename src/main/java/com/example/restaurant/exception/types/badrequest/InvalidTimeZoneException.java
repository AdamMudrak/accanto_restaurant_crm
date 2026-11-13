package com.example.restaurant.exception.types.badrequest;

public class InvalidTimeZoneException extends RuntimeException {
    public InvalidTimeZoneException() {
        super("Select a valid IANA time zone, for example Europe/Kyiv.");
    }
}
