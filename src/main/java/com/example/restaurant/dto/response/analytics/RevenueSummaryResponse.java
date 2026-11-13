package com.example.restaurant.dto.response.analytics;

import com.example.restaurant.entity.management.Restaurant;
import java.math.BigDecimal;
import java.util.List;

public record RevenueSummaryResponse(int year, int month, List<CurrencyRevenue> currencies,
                                     long unresolvedHistoricalPaymentsCount,
                                     long reconciliationRequiredPaymentsCount,
                                     String timeZone, List<CurrencyRefunds> refunds) {
    public record CurrencyRefunds(Restaurant.Currency currency, BigDecimal refundedRevenue,
                                  BigDecimal refundedTips, long refundCount) {}

    public record CurrencyRevenue(Restaurant.Currency currency, BigDecimal cashRevenue,
                                  BigDecimal onlineRevenue, BigDecimal totalRevenue,
                                  BigDecimal onlineTips, long paymentCount, long orderCount) {}
}
