package com.example.restaurant.service.order.diningtable;

import com.example.restaurant.dto.response.list.order.DiningTableListResponse;
import com.example.restaurant.dto.response.order.DiningTableResponse;
import com.example.restaurant.entity.order.DiningTable;
import com.example.restaurant.entity.order.Order;
import com.example.restaurant.exception.types.conflict.OrderIllegalStateException;
import com.example.restaurant.exception.types.notfound.DiningTableNotFoundException;
import com.example.restaurant.mapper.order.DiningTableMapper;
import com.example.restaurant.repository.order.DiningTableRepository;
import com.example.restaurant.repository.order.OrderRepository;
import com.example.restaurant.service.utils.QrCodeBuilder;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class DiningTableAdminService {
    private final OrderRepository orderRepository;
    private final DiningTableRepository diningTableRepository;
    private final DiningTableMapper diningTableMapper;
    @Value("${front.base.url}")
    private String frontBaseUrl;

    public DiningTableResponse createDiningTable() {
        DiningTable diningTable = new DiningTable();
        String qrUuid = UUID.randomUUID().toString();
        diningTable.setQrUuid(qrUuid);
        return diningTableMapper.toResponse(diningTableRepository.save(diningTable));
    }

    public DiningTableResponse updateDiningTableById(Long id, boolean isActive) {
        if (orderRepository.existsByDiningTableIdAndStatus(id, Order.OrderStatus.OPENED)) {
            throw new OrderIllegalStateException("Cannot change table status for table with id "
                    + id + " because there is an order already opened");
        }
        DiningTable diningTable = diningTableRepository.findById(id).orElseThrow(
                () -> new DiningTableNotFoundException(
                        "Dining table with id " + id + " Not Found"));
        diningTable.setActive(isActive);
        return diningTableMapper.toResponse(diningTableRepository.save(diningTable));
    }

    public DiningTableResponse getDiningTableById(Long id) {
        return diningTableMapper.toResponse(
                diningTableRepository.findById(id).orElseThrow(
                        () -> new DiningTableNotFoundException(
                                "Dining table with id " + id + " not found")));
    }

    public DiningTableResponse getActiveDiningTableById(Long id) {
        return diningTableMapper.toResponse(
                diningTableRepository.findByIdAndIsActiveTrue(id).orElseThrow(
                        () -> new DiningTableNotFoundException(
                                "Active dining table with id " + id + " not found")));
    }

    public DiningTableListResponse getAllDiningTables(Pageable pageable) {
        Page<DiningTable> tables = diningTableRepository.findAll(pageable);
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        long totalElements = tables.getTotalElements();

        return new DiningTableListResponse(
                diningTableMapper.toDiningTableResponseList(tables.toList()),
                pageNumber,
                pageSize,
                totalElements
        );
    }

    public DiningTableListResponse getAllActiveDiningTables(Pageable pageable) {
        Page<DiningTable> tables = diningTableRepository.findAllByIsActiveTrue(pageable);
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        long totalElements = tables.getTotalElements();

        return new DiningTableListResponse(
                diningTableMapper.toDiningTableResponseList(tables.toList()),
                pageNumber,
                pageSize,
                totalElements
        );
    }

    public byte[] generateDiningTableQrByUuid(String uuid) {
        if (!diningTableRepository.existsByQrUuidAndIsActiveTrue(uuid)) {
            throw new DiningTableNotFoundException(
                    "Active dining table with uuid " + uuid + " not found");
        }

        String resultUrl = frontBaseUrl + "/dining-tables/guest?uuid=" + uuid;

        return QrCodeBuilder.generatePng(resultUrl);
    }
}
