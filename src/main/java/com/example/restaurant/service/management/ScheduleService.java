package com.example.restaurant.service.management;

import com.example.restaurant.dto.request.management.UpdateWorkingDayRequest;
import com.example.restaurant.dto.response.list.management.WorkingDayListResponse;
import com.example.restaurant.dto.response.management.WorkingDayResponse;
import com.example.restaurant.entity.management.WorkingDay;
import com.example.restaurant.exception.types.conflict.TimeConflictException;
import com.example.restaurant.exception.types.notfound.WorkingDayNotFoundException;
import com.example.restaurant.mapper.management.WorkingDayMapper;
import com.example.restaurant.repository.management.WorkingDayRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {
    private static final int FIRST_PAGE = 0;
    private static final int DAYS_IN_WEEK = 7;
    private final WorkingDayRepository workingDayRepository;
    private final WorkingDayMapper workingDayMapper;

    public WorkingDayResponse getWorkingDayById(Long id) {
        return workingDayMapper.toResponse(findWorkingDayById(id));
    }

    public WorkingDayListResponse getWorkingDays() {
        List<WorkingDay> workingDays = workingDayRepository.findAll(
                Sort.by(Sort.Direction.ASC, "id"));
        List<WorkingDayResponse> workingDayResponseList = workingDayMapper.toResponse(workingDays);

        return new WorkingDayListResponse(
                workingDayResponseList, FIRST_PAGE, DAYS_IN_WEEK, DAYS_IN_WEEK);
    }

    public WorkingDayResponse updateWorkingDayById(
            Long id, UpdateWorkingDayRequest request) {
        WorkingDay workingDay = findWorkingDayById(id);

        if (request.isActive() != null) {
            workingDay.setActive(request.isActive());
        }
        assureTimeConsistencyThenSet(workingDay, request);

        return workingDayMapper.toResponse(
                workingDayRepository.save(workingDay));
    }

    private WorkingDay findWorkingDayById(Long id) {
        return workingDayRepository.findById(id).orElseThrow(
                () -> new WorkingDayNotFoundException(
                        "Couldn't find working day with id: " + id));
    }

    private void assureTimeConsistencyThenSet(
            WorkingDay workingDay, UpdateWorkingDayRequest request) {
        if (request.openAt() != null && request.closeAt() != null) {
            workingDay.setOpenAt(request.openAt());
            workingDay.setCloseAt(request.closeAt());
        } else if (request.openAt() != null) {
            if (request.openAt().isAfter(workingDay.getCloseAt())
                    || request.openAt().equals(workingDay.getCloseAt())) {
                throw new TimeConflictException("openAt must be before closeAt");
            }
            workingDay.setOpenAt(request.openAt());
        } else if (request.closeAt() != null) {
            if (request.closeAt().isBefore(workingDay.getOpenAt())
                    || request.closeAt().equals(workingDay.getOpenAt())) {
                throw new TimeConflictException("closeAt must be after openAt");
            }
            workingDay.setCloseAt(request.closeAt());
        }
    }
}
