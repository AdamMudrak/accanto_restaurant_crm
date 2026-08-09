package com.example.restaurant.controller.openapi;

public final class ManagementOpenApiExamples {
    public static final String EMPLOYEE_RESPONSE = """
            {
              "success": true,
              "message": "Employee with id 12 found.",
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
    public static final String EMPLOYEES_PAGE = """
            {
              "success": true,
              "message": "Employees retrieved successfully.",
              "data": {
                "items": [
                  {
                    "id": 12,
                    "firstName": "Olena",
                    "lastName": "Kovalenko",
                    "email": "olena.kovalenko@example.com",
                    "emailConfirmed": true,
                    "profilePictureId": 7,
                    "phone": "+380501112233",
                    "isDeleted": false
                  }
                ],
                "meta": {
                  "page": 0,
                  "size": 20,
                  "total": 1
                }
              },
              "errors": null
            }
            """;
    public static final String RESTAURANT_RESPONSE = """
            {
              "success": true,
              "message": "Successfully retrieved restaurant.",
              "data": {
                "id": 1,
                "name": "Ukrainian Restaurant",
                "logoId": 1,
                "backgroundImageId": 2,
                "defaultRegion": "UKRAINE",
                "language": "UA",
                "currency": "UAH",
                "paymentProvider": "MONOBANK"
              },
              "errors": null
            }
            """;
    public static final String WORKING_DAY_RESPONSE = """
            {
              "success": true,
              "message": "Retrieved working day successfully.",
              "data": {
                "id": 1,
                "day": "MONDAY",
                "isActive": true,
                "openAt": "09:00:00",
                "closeAt": "18:00:00"
              },
              "errors": null
            }
            """;
    public static final String WORKING_DAYS_RESPONSE = """
            {
              "success": true,
              "message": "Retrieved working days successfully.",
              "data": {
                "items": [
                  {
                    "id": 1,
                    "day": "MONDAY",
                    "isActive": true,
                    "openAt": "09:00:00",
                    "closeAt": "18:00:00"
                  }
                ],
                "meta": {
                  "page": 0,
                  "size": 7,
                  "total": 7
                }
              },
              "errors": null
            }
            """;
    public static final String RESTAURANT_TABLE_RESPONSE = """
            {
              "success": true,
              "message": "Successfully retrieved dining table.",
              "data": {
                "id": 1,
                "qrUuid": "550e8400-e29b-41d4-a716-446655440000",
                "isActive": true
              },
              "errors": null
            }
            """;
    public static final String RESTAURANT_TABLES_RESPONSE = """
            {
              "success": true,
              "message": "Successfully retrieved dining tables.",
              "data": {
                "items": [
                  {
                    "id": 1,
                    "qrUuid": "550e8400-e29b-41d4-a716-446655440000",
                    "isActive": true
                  }
                ],
                "meta": {
                  "page": 0,
                  "size": 20,
                  "total": 1
                }
              },
              "errors": null
            }
            """;
    public static final String ORDER_RESPONSE = """
            {
              "success": true,
              "message": "Successfully retrieved order.",
              "data": {
                "id": 15,
                "uuid": "550e8400-e29b-41d4-a716-446655440000",
                "orderStatus": "OPENED",
                "diningTableId": 1,
                "employeeId": 2,
                "createdAt": "2026-08-08T19:30:00",
                "closedAt": null,
                "orderItemsByGuests": [
                  {
                    "guestId": 7,
                    "orderItems": [
                      {
                        "id": 21,
                        "guestId": 7,
                        "orderId": 15,
                        "menuItemId": 3,
                        "itemNameSnapshot": "Latte",
                        "itemPriceSnapshot": 105.00,
                        "quantity": 2,
                        "status": "ORDERED",
                        "clientComment": "No sugar",
                        "employeeComment": null
                      }
                    ]
                  }
                ]
              },
              "errors": null
            }
            """;
    public static final String ORDERS_RESPONSE = """
            {
              "success": true,
              "message": "Successfully retrieved orders.",
              "data": {
                "items": [
                  {
                    "id": 15,
                    "uuid": "550e8400-e29b-41d4-a716-446655440000",
                    "orderStatus": "OPENED",
                    "diningTableId": 1,
                    "employeeId": 2,
                    "createdAt": "2026-08-08T19:30:00",
                    "closedAt": null,
                    "orderItemsByGuests": null
                  }
                ],
                "meta": {
                  "page": 0,
                  "size": 20,
                  "total": 1
                }
              },
              "errors": null
            }
            """;
    public static final String CURRENT_GUEST_ORDER_RESPONSE = """
            {
              "success": true,
              "message": "Successfully retrieved current order.",
              "data": {
                "thisGuestId": 7,
                "order": {
                  "id": 15,
                  "uuid": "550e8400-e29b-41d4-a716-446655440000",
                  "orderStatus": "OPENED",
                  "diningTableId": 1,
                  "employeeId": 2,
                  "createdAt": "2026-08-08T19:30:00",
                  "closedAt": null,
                  "orderItemsByGuests": [
                    {
                      "guestId": 7,
                      "orderItems": [
                        {
                          "id": 21,
                          "guestId": 7,
                          "orderId": 15,
                          "menuItemId": 3,
                          "itemNameSnapshot": "Latte",
                          "itemPriceSnapshot": 105.00,
                          "quantity": 2,
                          "status": "ORDERED",
                          "clientComment": "No sugar",
                          "employeeComment": null
                        }
                      ]
                    }
                  ]
                }
              },
              "errors": null
            }
            """;
    public static final String ORDER_ITEMS_BY_GUEST_RESPONSE = """
            {
              "success": true,
              "message": "Successfully updated order items.",
              "data": [
                {
                  "guestId": 7,
                  "orderItems": [
                    {
                      "id": 21,
                      "guestId": 7,
                      "orderId": 15,
                      "menuItemId": 3,
                      "itemNameSnapshot": "Latte",
                      "itemPriceSnapshot": 105.00,
                      "quantity": 2,
                      "status": "ORDERED",
                      "clientComment": "No sugar",
                      "employeeComment": null
                    }
                  ]
                }
              ],
              "errors": null
            }
            """;
    public static final String ORDER_CONFLICT_ERROR = """
            {
              "success": false,
              "message": null,
              "data": null,
              "errors": [
                "Cannot close order with unpaid order items."
              ]
            }
            """;
    public static final String PAYMENT_RESPONSE = """
            {
              "success": true,
              "message": "Successfully created payment.",
              "data": {
                "paymentId": 33,
                "invoiceId": "cs_test_a1B2c3D4",
                "pageUrl": "https://checkout.stripe.com/c/pay/cs_test_a1B2c3D4",
                "status": "PENDING",
                "orderAmount": 210.00,
                "tipAmount": 20.00
              },
              "errors": null
            }
            """;
    public static final String PAYMENT_VALIDATION_ERROR = """
            {
              "success": false,
              "message": null,
              "data": null,
              "errors": [
                "orderItemsIds: must not be empty"
              ]
            }
            """;
    public static final String PAYMENT_CONFLICT_ERROR = """
            {
              "success": false,
              "message": null,
              "data": null,
              "errors": [
                "Order item with id 21 must be CONFIRMED or SERVED before payment."
              ]
            }
            """;
    public static final String MENU_CATEGORY_RESPONSE = """
            {
              "success": true,
              "message": "Retrieved menu category successfully.",
              "data": {
                "id": 3,
                "nameDefaultLanguage": "Coffee",
                "nameSecondLanguage": "Кава",
                "parentCategoryId": 1
              },
              "errors": null
            }
            """;
    public static final String PARENT_MENU_CATEGORY_RESPONSE = """
            {
              "success": true,
              "message": "Created parent menu category successfully.",
              "data": {
                "id": 1,
                "nameDefaultLanguage": "Drinks",
                "nameSecondLanguage": "Напої",
                "parentCategoryId": null
              },
              "errors": null
            }
            """;
    public static final String PARENT_MENU_CATEGORIES_RESPONSE = """
            {
              "success": true,
              "message": "Retrieved parent menu categories successfully.",
              "data": {
                "items": [
                  {
                    "id": 1,
                    "nameDefaultLanguage": "Drinks",
                    "nameSecondLanguage": "Напої",
                    "parentCategoryId": null
                  }
                ],
                "meta": {
                  "page": 0,
                  "size": 20,
                  "total": 1
                }
              },
              "errors": null
            }
            """;
    public static final String CHILD_MENU_CATEGORIES_RESPONSE = """
            {
              "success": true,
              "message": "Retrieved children menu categories successfully.",
              "data": {
                "items": [
                  {
                    "id": 3,
                    "nameDefaultLanguage": "Coffee",
                    "nameSecondLanguage": "Кава",
                    "parentCategoryId": 1
                  }
                ],
                "meta": {
                  "page": 0,
                  "size": 20,
                  "total": 1
                }
              },
              "errors": null
            }
            """;
    public static final String MENU_CATEGORY_VALIDATION_ERROR = """
            {
              "success": false,
              "message": null,
              "data": null,
              "errors": [
                "nameDefaultLanguage or nameSecondLanguage required"
              ]
            }
            """;
    public static final String MENU_CATEGORY_CONFLICT_ERROR = """
            {
              "success": false,
              "message": null,
              "data": null,
              "errors": [
                "Can't delete parent category with id 1 while it has children."
              ]
            }
            """;
    public static final String EMPTY_MENU_CATEGORY_SUCCESS = """
            {
              "success": true,
              "message": "Deleted menu category successfully.",
              "data": null,
              "errors": null
            }
            """;
    public static final String MENU_ITEM_RESPONSE = """
            {
              "success": true,
              "message": "Successfully retrieved menu item",
              "data": {
                "id": 2,
                "nameDefaultLanguage": "Latte",
                "nameSecondLanguage": "Лате",
                "descriptionDefaultLanguage": "Espresso with steamed milk and light foam.",
                "descriptionSecondLanguage": "Еспресо з пареним молоком та легкою пінкою.",
                "ingredientsDefaultLanguage": [
                  "Espresso",
                  "Milk",
                  "Milk foam"
                ],
                "ingredientsSecondLanguage": [
                  "Еспресо",
                  "Молоко",
                  "Молочна пінка"
                ],
                "isAllergenic": true,
                "isVegetarian": true,
                "isVegan": false,
                "maxQuantityPerPerson": 4,
                "price": 105.00,
                "imageId": 4,
                "childCategoryId": 3
              },
              "errors": null
            }
            """;
    public static final String MENU_ITEMS_RESPONSE = """
            {
              "success": true,
              "message": "Menu items retrieved successfully.",
              "data": {
                "items": [
                  {
                    "id": 2,
                    "nameDefaultLanguage": "Latte",
                    "nameSecondLanguage": "Лате",
                    "descriptionDefaultLanguage": "Espresso with steamed milk and light foam.",
                    "descriptionSecondLanguage": "Еспресо з пареним молоком та легкою пінкою.",
                    "ingredientsDefaultLanguage": [
                      "Espresso",
                      "Milk",
                      "Milk foam"
                    ],
                    "ingredientsSecondLanguage": [
                      "Еспресо",
                      "Молоко",
                      "Молочна пінка"
                    ],
                    "isAllergenic": true,
                    "isVegetarian": true,
                    "isVegan": false,
                    "maxQuantityPerPerson": 4,
                    "price": 105.00,
                    "imageId": 4,
                    "childCategoryId": 3
                  }
                ],
                "meta": {
                  "page": 0,
                  "size": 20,
                  "total": 1
                }
              },
              "errors": null
            }
            """;
    public static final String EMPTY_MENU_ITEM_SUCCESS = """
            {
              "success": true,
              "message": "Successfully deleted menu item.",
              "data": null,
              "errors": null
            }
            """;
    public static final String MENU_ITEM_VALIDATION_ERROR = """
            {
              "success": false,
              "message": null,
              "data": null,
              "errors": [
                "nameDefaultLanguage or nameSecondLanguage required",
                "price: must not be null",
                "childCategoryId: must not be null"
              ]
            }
            """;
    public static final String EMPTY_SUCCESS = """
            {
              "success": true,
              "message": "No auth test success",
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
                "id: must be greater than 0"
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
                "Resource not found"
              ]
            }
            """;
    public static final String CONFLICT_ERROR = """
            {
              "success": false,
              "message": null,
              "data": null,
              "errors": [
                "openAt must be before closeAt"
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

    private ManagementOpenApiExamples() {
    }
}
