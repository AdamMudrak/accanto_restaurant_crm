package com.example.restaurant.mapper.order;

import com.example.restaurant.dto.response.order.DiningTableResponse;
import com.example.restaurant.entity.order.DiningTable;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DiningTableMapper {
    public DiningTableResponse toResponse(DiningTable diningTable) {
        return new DiningTableResponse(
                diningTable.getId(),
                diningTable.getQrUuid(),
                diningTable.isActive()
        );
    }

    public List<DiningTableResponse> toDiningTableResponseList(
            List<DiningTable> diningTables) {
        List<DiningTableResponse> diningTableResponseList = new ArrayList<>();

        for (DiningTable diningTable : diningTables) {
            diningTableResponseList.add(toResponse(diningTable));
        }

        return diningTableResponseList;
    }

}
