package com.example.restaurant.controller.openapi;

public final class AuthOpenApiExamples {
    public static final String EMPLOYEE_CREATED = """
            {
              "success": true,
              "message": "Employee created and saved. Account confirmation email sent.",
              "data": {
                "id": 12,
                "firstName": "Olena",
                "lastName": "Kovalenko",
                "email": "olena.kovalenko@example.com",
                "emailConfirmed": false,
                "profilePictureId": 7,
                "phone": "+380501112233",
                "isDeleted": false
              },
              "errors": null
            }
            """;
    public static final String EMPLOYEE_CONFIRMED = """
            {
              "success": true,
              "message": "Email confirmed, password set successfully.",
              "data": {
                "id": 12,
                "firstName": "Olena",
                "lastName": "Kovalenko",
                "email": "olena.kovalenko@example.com",
                "emailConfirmed": true,
                "profilePictureId": 7,
                "phone": "+380501112233",
                "isDeleted": false
              },
              "errors": null
            }
            """;
    public static final String LOGIN_SUCCESS = """
            {
              "success": true,
              "message": "Login performed successfully.",
              "data": null,
              "errors": null
            }
            """;
    public static final String LOGOUT_SUCCESS = """
            {
              "success": true,
              "message": "Logout performed successfully.",
              "data": null,
              "errors": null
            }
            """;
    public static final String RESET_LINK_SENT = """
            {
              "success": true,
              "message": "Reset link has been sent successfully.",
              "data": null,
              "errors": null
            }
            """;
    public static final String PASSWORD_SET = """
            {
              "success": true,
              "message": "Password set successfully.",
              "data": null,
              "errors": null
            }
            """;
    public static final String PASSWORD_CHANGED = """
            {
              "success": true,
              "message": "New password set successfully.",
              "data": null,
              "errors": null
            }
            """;
    public static final String VALIDATION_ERROR = """
            {
              "success": false,
              "message": null,
              "data": null,
              "errors": [
                "email: must be a well-formed email address",
                "password: must not be blank"
              ]
            }
            """;
    public static final String BAD_REQUEST_ERROR = """
            {
              "success": false,
              "message": null,
              "data": null,
              "errors": [
                "Can't process this image. Please use jpg, jpeg or png."
              ]
            }
            """;
    public static final String UNAUTHORIZED_ERROR = """
            {
              "success": false,
              "message": null,
              "data": null,
              "errors": [
                "Either login or password is invalid"
              ]
            }
            """;
    public static final String INVALID_TOKEN_ERROR = """
            {
              "success": false,
              "message": null,
              "data": null,
              "errors": [
                "This token is not of the type expected."
              ]
            }
            """;
    public static final String FORBIDDEN_ERROR = """
            {
              "success": false,
              "message": null,
              "data": null,
              "errors": [
                "Access denied"
              ]
            }
            """;
    public static final String NOT_FOUND_ERROR = """
            {
              "success": false,
              "message": null,
              "data": null,
              "errors": [
                "Employee not found"
              ]
            }
            """;
    public static final String CONFLICT_ERROR = """
            {
              "success": false,
              "message": null,
              "data": null,
              "errors": [
                "Email already used"
              ]
            }
            """;
    public static final String GONE_ERROR = """
            {
              "success": false,
              "message": null,
              "data": null,
              "errors": [
                "This token is already expired."
              ]
            }
            """;
    public static final String INTERNAL_ERROR = """
            {
              "success": false,
              "message": null,
              "data": null,
              "errors": [
                "Internal server error"
              ]
            }
            """;

    private AuthOpenApiExamples() {
    }
}
