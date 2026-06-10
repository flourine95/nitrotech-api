package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.order.dto.*;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.infrastructure.persistence.entity.OrderEntity;
import com.nitrotech.api.infrastructure.persistence.entity.OrderItemEntity;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpa;
    private final OrderItemJpaRepository itemJpa;

    @Override
    @Transactional
    public OrderData place(PlaceOrderData data) {
        OrderEntity entity = new OrderEntity();
        entity.setUserId(data.userId());
        entity.setShippingAddress(snapshotToMap(data.shippingAddress()));
        entity.setPaymentMethod(data.paymentMethod());
        entity.setPromotionCode(data.promotionCode());
        entity.setNote(data.note());
        entity.setTotalAmount(data.totalAmount());
        entity.setDiscountAmount(data.discountAmount());
        entity.setShippingFee(data.shippingFee());
        entity.setFinalAmount(data.finalAmount());
        OrderEntity saved = orderJpa.save(entity);

        data.items().forEach(item -> {
            OrderItemEntity itemEntity = new OrderItemEntity();
            itemEntity.setOrder(saved);
            itemEntity.setVariantId(item.variantId());
            itemEntity.setName(item.name());
            itemEntity.setSku(item.sku());
            itemEntity.setQuantity(item.quantity());
            itemEntity.setUnitPrice(item.unitPrice());
            itemEntity.setSubtotal(item.subtotal());
            itemJpa.save(itemEntity);
        });

        return toData(saved);
    }

    @Override
    public Optional<OrderData> findByIdAndUserId(Long id, Long userId) {
        return orderJpa.findByIdAndUserId(id, userId).map(this::toData);
    }

    @Override
    public Optional<OrderData> findById(Long id) {
        return orderJpa.findActiveById(id).map(this::toData);
    }

    @Override
    public Page<OrderData> findAll(OrderListQuery query) {
        return orderJpa.findAllFiltered(query.userId(), query.status(),
                        PageRequest.of(query.page(), query.size()))
                .map(this::toData);
    }

    @Override
    @Transactional
    public OrderData updateStatus(Long id, String status) {
        OrderEntity entity = orderJpa.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        entity.setStatus(status);
        return toData(orderJpa.save(entity));
    }

    @Override
    public boolean existsByIdAndUserId(Long id, Long userId) {
        return orderJpa.existsByIdAndUserId(id, userId);
    }

    @Override
    @Transactional
    public int expirePendingCreatedAtOrBefore(Instant cutoff, Instant expiredAt) {
        return orderJpa.expirePendingCreatedAtOrBefore(cutoff, expiredAt);
    }

    private Map<String, Object> snapshotToMap(ShippingAddressSnapshot s) {
        return Map.of(
                "receiver", s.receiver(), "phone", s.phone(),
                "province", s.province(), "provinceCode", s.provinceCode(),
                "district", s.district(), "districtCode", s.districtCode(),
                "ward", s.ward(), "wardCode", s.wardCode(),
                "street", s.street()
        );
    }

    @SuppressWarnings("unchecked")
    private ShippingAddressSnapshot mapToSnapshot(Map<String, Object> m) {
        return new ShippingAddressSnapshot(
                (String) m.get("receiver"), (String) m.get("phone"),
                (String) m.get("province"), (String) m.get("provinceCode"),
                (String) m.get("district"), (String) m.get("districtCode"),
                (String) m.get("ward"), (String) m.get("wardCode"),
                (String) m.get("street")
        );
    }

    private OrderData toData(OrderEntity e) {
        List<OrderItemData> items = itemJpa.findByOrder_Id(e.getId()).stream()
                .map(i -> new OrderItemData(i.getId(), i.getVariantId(), i.getName(),
                        i.getSku(), i.getQuantity(), i.getUnitPrice(), i.getSubtotal()))
                .toList();
        return new OrderData(
                e.getId(), e.getUserId(), mapToSnapshot(e.getShippingAddress()),
                e.getStatus(), e.getPaymentMethod(),
                e.getTotalAmount(), e.getDiscountAmount(), e.getShippingFee(), e.getFinalAmount(),
                e.getPromotionCode(), e.getNote(), items,
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
