package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.restaurant.dto.request.menu.MenuCategoryRequest;
import com.example.restaurant.dto.request.menu.MenuItemRequest;
import com.example.restaurant.exception.types.conflict.MenuCategoryIsUsedException;
import com.example.restaurant.exception.types.notfound.MenuCategoryNotFoundException;
import com.example.restaurant.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MenuIntegrationTest extends AbstractIntegrationTest {
    @Test
    void categoryLifecycleThroughPublicReadsAndAdminWrites() throws Exception {
        var parent = request("POST", "/menu-categories", new MenuCategoryRequest("Seasonal", null),
                adminCookie());
        assertThat(parent.status()).isEqualTo(200);
        long parentId = parent.id();
        var child = request("POST", "/menu-categories/parents/" + parentId + "/children",
                new MenuCategoryRequest(null, "Specials"), adminCookie());
        assertThat(child.status()).isEqualTo(200);
        long childId = child.id();
        assertThat(child.data().path("parentCategoryId").asLong()).isEqualTo(parentId);
        assertThat(request("GET", "/menu-categories/" + childId, null, null).status()).isEqualTo(200);
        assertThat(request("GET", "/menu-categories/children/" + parentId, null, null)
                .data().path("items").size()).isEqualTo(1);
        assertThat(request("GET", "/menu-categories/parents?page=0&size=1", null, null)
                .data().path("items").size()).isEqualTo(1);
        assertThat(request("PUT", "/menu-categories/parents/" + parentId,
                new MenuCategoryRequest("Renamed", null), adminCookie()).status()).isEqualTo(200);
        assertThat(request("PUT", "/menu-categories/children/" + childId,
                new MenuCategoryRequest("Child", "Other"), adminCookie()).status()).isEqualTo(200);
        assertThat(request("DELETE", "/menu-categories/parents/" + parentId, null, adminCookie())
                .status()).isEqualTo(409);
        assertThat(request("DELETE", "/menu-categories/children/" + childId, null, adminCookie())
                .status()).isEqualTo(200);
        assertThat(request("DELETE", "/menu-categories/parents/" + parentId, null, adminCookie())
                .status()).isEqualTo(200);
        assertThat(request("GET", "/menu-categories/" + parentId, null, null).status()).isEqualTo(404);
    }

    @Test
    void childWithMenuItemsCannotBeDeletedAndParentCannotBeUsedAsChild() {
        var parent = categories.createParentCategory(new MenuCategoryRequest("Root", null));
        var child = categories.createChildCategory(parent.id(), new MenuCategoryRequest("Child", null));
        menuService.createMenuItem(menuRequest(child.id()), null);
        assertThatThrownBy(() -> categories.deleteChildCategoryById(child.id()))
                .isInstanceOf(MenuCategoryIsUsedException.class);
        assertThatThrownBy(() -> categories.createChildCategory(child.id(),
                new MenuCategoryRequest("Invalid", null)))
                .isInstanceOf(MenuCategoryNotFoundException.class);
        assertThatThrownBy(() -> categories.updateChildCategory(parent.id(),
                new MenuCategoryRequest("Invalid", null)))
                .isInstanceOf(MenuCategoryNotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/menu-categories/9000000000", "/menu-categories/children/9000000000",
            "/menu-items/9000000000", "/menu-items/category/9000000000"})
    void unknownMenuResourcesReturnNotFound(String path) throws Exception {
        assertThat(request("GET", path, null, null).status()).isEqualTo(404);
    }

    @Test
    void blankCategoryNamesAreRejected() throws Exception {
        assertThat(request("POST", "/menu-categories", Map.of("nameDefaultLanguage", " "),
                adminCookie()).status()).isEqualTo(400);
    }

    @Test
    void menuItemLifecycleIncludesLocalizationIngredientsCurrencyAndImageReplacement() throws Exception {
        var parent = categories.createParentCategory(new MenuCategoryRequest("Root", null));
        var child = categories.createChildCategory(parent.id(), new MenuCategoryRequest("Child", null));
        var created = multipart("POST", "/menu-items", Map.of("menuItem", menuRequest(child.id())),
                true, adminCookie());
        assertThat(created.status()).isEqualTo(200);
        long id = created.id();
        long imageId = created.data().path("imageId").asLong();
        assertThat(created.data().path("currency").asText()).isEqualTo("UAH");
        assertThat(created.data().path("ingredientsDefaultLanguage").size()).isEqualTo(2);
        assertThat(request("GET", "/images/" + imageId, null, null).bytes()).isEqualTo(png().getBytes());
        var updated = multipart("PUT", "/menu-items/" + id, Map.of("menuItem", Map.ofEntries(
                Map.entry("nameDefaultLanguage", "New name"), Map.entry("nameSecondLanguage", "Other"),
                Map.entry("descriptionDefaultLanguage", "New description"),
                Map.entry("descriptionSecondLanguage", "Other description"),
                Map.entry("ingredientsDefaultLanguage", List.of("Water")),
                Map.entry("ingredientsSecondLanguage", List.of("Water")),
                Map.entry("isAllergenic", true), Map.entry("isVegetarian", true),
                Map.entry("isVegan", true), Map.entry("maxQuantityPerPerson", 3),
                Map.entry("weightGrams", 250), Map.entry("price", 12.50))), true, adminCookie());
        assertThat(updated.status()).isEqualTo(200);
        assertThat(updated.data().path("price").decimalValue()).isEqualByComparingTo("12.50");
        assertThat(updated.data().path("isVegan").asBoolean()).isTrue();
        assertThat(updated.data().path("imageId").asLong()).isNotEqualTo(imageId);
        assertThat(request("GET", "/images/" + imageId, null, null).status()).isEqualTo(404);
        assertThat(request("GET", "/menu-items/" + id, null, null).data().path("nameDefaultLanguage")
                .asText()).isEqualTo("New name");
        assertThat(request("GET", "/menu-items/category/" + child.id(), null, null)
                .data().path("items").size()).isEqualTo(1);
        assertThat(request("DELETE", "/menu-items/" + id, null, adminCookie()).status()).isEqualTo(200);
        assertThat(request("GET", "/menu-items/" + id, null, null).status()).isEqualTo(404);
        assertThat(menu.existsById(id)).isTrue();
        assertThat(request("GET", "/menu-items/category/" + child.id(), null, null)
                .data().path("items").size()).isZero();
        assertThat(multipart("PUT", "/menu-items/" + id,
                Map.of("menuItem", Map.of("isAvailable", true)), false, adminCookie()).status())
                .isEqualTo(200);
        assertThat(request("GET", "/menu-items/" + id, null, null).status()).isEqualTo(200);
    }

    @Test
    void partialUpdateWithoutImagePreservesUnspecifiedFieldsAndCanChangeCategory() throws Exception {
        var parent = categories.createParentCategory(new MenuCategoryRequest("Root", null));
        var child = categories.createChildCategory(parent.id(), new MenuCategoryRequest("Child", null));
        var created = menuService.createMenuItem(menuRequest(child.id()), null);
        var other = categories.createChildCategory(parent.id(), new MenuCategoryRequest("Other", null));
        var response = multipart("PUT", "/menu-items/" + created.id(),
                Map.of("menuItem", Map.of("childCategoryId", other.id())), false, adminCookie());
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.data().path("childCategoryId").asLong()).isEqualTo(other.id());
        assertThat(response.data().path("nameDefaultLanguage").asText()).isEqualTo("Test dish");
        assertThat(response.data().path("price").decimalValue()).isEqualByComparingTo("10.00");
        assertThat(multipart("PUT", "/menu-items/" + created.id(), Map.of(), false,
                adminCookie()).status()).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing-name", "zero-price", "negative-quantity", "parent-category"})
    void invalidMenuItemsCannotBeCreated(String problem) throws Exception {
        long parent = categories.createParentCategory(new MenuCategoryRequest("Root", null)).id();
        long child = categories.createChildCategory(parent, new MenuCategoryRequest("Child", null)).id();
        Map<String, Object> body = new HashMap<>();
        body.put("nameDefaultLanguage", problem.equals("missing-name") ? " " : "Dish");
        body.put("price", problem.equals("zero-price") ? 0 : 10);
        body.put("childCategoryId", problem.equals("parent-category") ? parent : child);
        body.put("maxQuantityPerPerson", problem.equals("negative-quantity") ? -1 : 2);
        body.put("isAllergenic", false);
        body.put("isVegetarian", false);
        body.put("isVegan", false);
        var result = multipart("POST", "/menu-items", Map.of("menuItem", body), false, adminCookie());
        assertThat(result.status()).isEqualTo(problem.equals("parent-category") ? 404 : 400);
    }

    @Test
    void listsHaveConsistentCurrencyAndPagination() throws Exception {
        var result = request("GET", "/menu-items?page=0&size=2&sort=id,asc", null, null);
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.data().path("items").size()).isEqualTo(2);
        assertThat(result.data().path("meta").path("total").asLong()).isGreaterThan(2);
        for (var item : result.data().path("items")) {
            assertThat(item.path("currency").asText()).isEqualTo("UAH");
        }
    }

    private MenuItemRequest menuRequest(Long categoryId) {
        return new MenuItemRequest("Test dish", "Second name", "Description", "Other description",
                List.of("Salt", "Water"), List.of("Salt", "Water"), false, false, false,
                5, 100, new BigDecimal("10.00"), categoryId);
    }
}
