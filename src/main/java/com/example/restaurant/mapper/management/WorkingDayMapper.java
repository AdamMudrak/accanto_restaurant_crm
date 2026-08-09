package com.example.restaurant.mapper.management;

import com.example.restaurant.dto.response.management.WorkingDayResponse;
import com.example.restaurant.entity.management.WorkingDay;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WorkingDayMapper {
    public WorkingDayResponse toResponse(WorkingDay workingDay) {
        return new WorkingDayResponse(
                workingDay.getId(),
                workingDay.getDay().name(),
                workingDay.isActive(),
                workingDay.getOpenAt(),
                workingDay.getCloseAt()
        );
    }

    public List<WorkingDayResponse> toResponse(List<WorkingDay> workingDays) {
        List<WorkingDayResponse> responses = new ArrayList<>();
        for (WorkingDay workingDay : workingDays) {
            responses.add(toResponse(workingDay));
        }

        return responses;
    }
}
