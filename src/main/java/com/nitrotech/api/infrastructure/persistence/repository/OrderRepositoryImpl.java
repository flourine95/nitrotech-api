package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.order.OrderStatus;
import com.nitrotech.api.domain.order.dto.*;
import com.nitrotech.api.domain.order.exception.OrderNotFoundException;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.infrastructure.persistence.entity.OrderEntity;
import com.nitrotech.api.infrastructure.persistence.entity.OrderItemEntity;
import com.nitrotech.api.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.nitrotech.api.infrastructure.persistence.entity.ShipmentEntity;
import com.nitrotech.api.infrastructure.persistence.entity.UserEntity;
import com.nitrotech.api.infrastructure.persistence.spec.OrderSpecification;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpa;
    private final OrderItemJpaRepository itemJpa;
    private final UserJpaRepository userJpa;
    private final PaymentTransactionJpaRepository paymentJpa;
    private final ShipmentJpaRepository shipmentJpa;
    private final ProductVariantJpaRepository variantJpa;
    private final EntityManager em;

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
        entity.setOrderCode(java.util.UUID.randomUUID().toString());
        OrderEntity saved = orderJpa.save(entity);
        saved.setOrderCode("SO-" + String.format("%03d", saved.getId()));
        final OrderEntity finalSaved = orderJpa.saveAndFlush(saved);

        data.items().forEach(item -> {
            OrderItemEntity itemEntity = new OrderItemEntity();
            itemEntity.setOrder(finalSaved);
            itemEntity.setVariantId(item.variantId());
            itemEntity.setName(item.name());
            itemEntity.setSku(item.sku());
            itemEntity.setQuantity(item.quantity());
            itemEntity.setUnitPrice(item.unitPrice());
            itemEntity.setSubtotal(item.subtotal());
            itemEntity.setWeightGrams(item.weightGrams());
            itemEntity.setLengthCm(item.lengthCm());
            itemEntity.setWidthCm(item.widthCm());
            itemEntity.setHeightCm(item.heightCm());
            itemJpa.save(itemEntity);
        });

        return toData(finalSaved);
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
    public Page<OrderListItemData> findList(OrderFilter filter, Pageable pageable) {
        Specification<OrderEntity> spec = OrderSpecification.from(filter);
        Page<OrderEntity> page = orderJpa.findAll(spec, pageable);
        if (page.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> orderIds = page.getContent().stream().map(OrderEntity::getId).toList();
        List<Long> userIds = page.getContent().stream().map(OrderEntity::getUserId).distinct().toList();
        List<Object[]> countRows = orderJpa.countItemsForOrders(orderIds);
        Map<Long, Long> itemCountMap = countRows.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
        Map<Long, String> userEmailMap = userJpa.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getEmail));
        Map<Long, String> paymentStatusMap = paymentJpa.findByOrderIdInOrderByCreatedAtDesc(orderIds).stream()
                .collect(Collectors.toMap(
                        PaymentTransactionEntity::getOrderId,
                        PaymentTransactionEntity::getStatus,
                        (existing, ignored) -> existing
                ));
        Map<Long, ShipmentEntity> shipmentMap = shipmentJpa.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(ShipmentEntity::getOrderId, shipment -> shipment));

        return page.map(e -> toListItemData(
                e,
                itemCountMap.getOrDefault(e.getId(), 0L),
                userEmailMap.get(e.getUserId()),
                paymentStatusMap.get(e.getId()),
                shipmentMap.get(e.getId())
        ));
    }

    private OrderListItemData toListItemData(
            OrderEntity e,
            Long itemCount,
            String email,
            String paymentStatus,
            ShipmentEntity shipment
    ) {
        String receiver = null;
        String phone = null;
        if (e.getShippingAddress() != null) {
            Object rec = e.getShippingAddress().get("receiver");
            if (rec == null) {
                rec = e.getShippingAddress().get("name");
            }
            if (rec != null) receiver = rec.toString();
            Object ph = e.getShippingAddress().get("phone");
            if (ph != null) phone = ph.toString();
        }
        return new OrderListItemData(
                e.getId(),
                e.getUserId(),
                e.getOrderCode(),
                receiver,
                phone,
                email,
                e.getStatus(),
                e.getPaymentMethod(),
                paymentStatus,
                shipment != null,
                shipment == null ? null : shipment.getStatus(),
                shipment == null ? null : shipment.getTrackingCode(),
                availableActions(e.getStatus(), shipment != null),
                Duration.between(e.getCreatedAt(), Instant.now()).toMinutes(),
                slaDueAt(e, shipment != null),
                slaStatus(e, shipment != null),
                slaLabel(e.getStatus(), shipment != null),
                e.getFinalAmount(),
                itemCount,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    private List<String> availableActions(String status, boolean hasShipment) {
        OrderStatus orderStatus = OrderStatus.fromValue(status);
        if (orderStatus == null) {
            return List.of("view_detail");
        }
        return switch (orderStatus) {
            case PENDING -> List.of("view_detail", "confirm", "cancel");
            case CONFIRMED -> hasShipment
                    ? List.of("view_detail", "mark_processing", "cancel")
                    : List.of("view_detail", "create_shipment", "mark_processing", "cancel");
            case PROCESSING -> hasShipment
                    ? List.of("view_detail", "mark_shipped")
                    : List.of("view_detail", "create_shipment", "mark_shipped");
            case SHIPPED -> List.of("view_detail", "mark_delivered");
            case DELIVERED -> List.of("view_detail", "refund");
            default -> List.of("view_detail");
        };
    }

    private Instant slaDueAt(OrderEntity order, boolean hasShipment) {
        Instant base = order.getUpdatedAt() == null ? order.getCreatedAt() : order.getUpdatedAt();
        OrderStatus status = OrderStatus.fromValue(order.getStatus());
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PENDING -> order.getCreatedAt().plus(Duration.ofMinutes(60));
            case CONFIRMED -> hasShipment ? null : base.plus(Duration.ofHours(4));
            case PROCESSING -> base.plus(Duration.ofHours(24));
            case SHIPPED -> base.plus(Duration.ofDays(3));
            default -> null;
        };
    }

    private String slaStatus(OrderEntity order, boolean hasShipment) {
        Instant dueAt = slaDueAt(order, hasShipment);
        if (dueAt == null) {
            return "normal";
        }
        Instant now = Instant.now();
        if (!now.isBefore(dueAt)) {
            return "critical";
        }
        Instant base = OrderStatus.is(order.getStatus(), OrderStatus.PENDING)
                ? order.getCreatedAt()
                : (order.getUpdatedAt() == null ? order.getCreatedAt() : order.getUpdatedAt());
        long totalSeconds = Math.max(1, Duration.between(base, dueAt).getSeconds());
        long elapsedSeconds = Math.max(0, Duration.between(base, now).getSeconds());
        return elapsedSeconds * 100 / totalSeconds >= 75 ? "warning" : "normal";
    }

    private String slaLabel(String status, boolean hasShipment) {
        OrderStatus orderStatus = OrderStatus.fromValue(status);
        if (orderStatus == null) {
            return null;
        }
        return switch (orderStatus) {
            case PENDING -> "Chờ xác nhận";
            case CONFIRMED -> hasShipment ? null : "Chờ tạo vận đơn";
            case PROCESSING -> "Chờ bàn giao";
            case SHIPPED -> "Chờ giao thành công";
            default -> null;
        };
    }

    @Override
    public long countFacetsTotal(OrderFilter filter) {
        OrderFilter totalFilter = new OrderFilter(
                filter.userId(),
                filter.search(),
                null, // Exclude status from total facet query
                filter.paymentMethod(),
                filter.createdFrom(),
                filter.createdToExclusive(),
                filter.amountMin(),
                filter.amountMax()
        );
        Specification<OrderEntity> spec = OrderSpecification.from(totalFilter);
        return orderJpa.count(spec);
    }

    @Override
    public List<Object[]> countStatuses(OrderFilter filter) {
        OrderFilter statusFilter = new OrderFilter(
                filter.userId(),
                filter.search(),
                null, // Exclude status from status facet query
                filter.paymentMethod(),
                filter.createdFrom(),
                filter.createdToExclusive(),
                filter.amountMin(),
                filter.amountMax()
        );
        return countFacetsGroupedBy(statusFilter, "status");
    }

    @Override
    public List<Object[]> countPaymentMethods(OrderFilter filter) {
        OrderFilter pmFilter = new OrderFilter(
                filter.userId(),
                filter.search(),
                filter.status(),
                null, // Exclude paymentMethod from paymentMethod facet query
                filter.createdFrom(),
                filter.createdToExclusive(),
                filter.amountMin(),
                filter.amountMax()
        );
        return countFacetsGroupedBy(pmFilter, "paymentMethod");
    }

    private List<Object[]> countFacetsGroupedBy(OrderFilter filter, String attributeName) {
        Specification<OrderEntity> spec = OrderSpecification.from(filter);
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Object[].class);
        var root = query.from(OrderEntity.class);

        var predicate = spec.toPredicate(root, query, cb);

        query.multiselect(root.get(attributeName), cb.count(root.get("id")))
                .where(predicate)
                .groupBy(root.get(attributeName));

        return em.createQuery(query).getResultList();
    }

    @Override
    @Transactional
    public OrderData updateStatus(Long id, String status) {
        OrderEntity entity = orderJpa.findActiveById(id)
                .orElseThrow(OrderNotFoundException::new);
        entity.setStatus(status);
        return toData(orderJpa.save(entity));
    }

    @Override
    public boolean existsByIdAndUserId(Long id, Long userId) {
        return orderJpa.existsByIdAndUserId(id, userId);
    }

    @Override
    public List<OrderData> findPendingCreatedAtOrBefore(Instant cutoff) {
        return orderJpa.findPendingCreatedAtOrBefore(cutoff).stream()
                .map(this::toData)
                .toList();
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
        String receiver = (String) m.get("receiver");
        if (receiver == null) {
            receiver = (String) m.get("name");
        }
        return new ShippingAddressSnapshot(
                receiver, (String) m.get("phone"),
                (String) m.get("province"), (String) m.get("provinceCode"),
                (String) m.get("district"), (String) m.get("districtCode"),
                (String) m.get("ward"), (String) m.get("wardCode"),
                (String) m.get("street")
        );
    }

    private OrderData toData(OrderEntity e) {
        List<OrderItemEntity> orderItems = itemJpa.findByOrder_Id(e.getId());
        Map<Long, Long> productIds = variantJpa.findAllById(orderItems.stream()
                        .map(OrderItemEntity::getVariantId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(v -> v.getId(), v -> v.getProductId()));
        List<OrderItemData> items = orderItems.stream()
                .map(i -> new OrderItemData(
                        i.getId(), i.getVariantId(), productIds.get(i.getVariantId()), i.getName(),
                        i.getSku(), i.getQuantity(), i.getUnitPrice(), i.getSubtotal(),
                        null, // imageUrl: populated by variant→image join when needed
                        i.getWeightGrams(), i.getLengthCm(), i.getWidthCm(), i.getHeightCm()
                ))
                .toList();

        UserSummaryData userSummary = userJpa.findById(e.getUserId())
                .map(u -> new UserSummaryData(u.getName(), u.getEmail(), u.getPhone(), u.getAvatar()))
                .orElse(null);

        PaymentSummaryData paymentSummary = paymentJpa
                .findTopByOrderIdOrderByCreatedAtDesc(e.getId())
                .map(p -> new PaymentSummaryData(p.getProvider(), p.getStatus(), p.getAmount(), p.getPaidAt()))
                .orElse(null);

        return new OrderData(
                e.getId(), e.getUserId(), e.getOrderCode(), mapToSnapshot(e.getShippingAddress()),
                e.getStatus(), e.getPaymentMethod(),
                e.getTotalAmount(), e.getDiscountAmount(), e.getShippingFee(), e.getFinalAmount(),
                e.getPromotionCode(), e.getNote(), items,
                e.getCreatedAt(), e.getUpdatedAt(),
                userSummary, paymentSummary
        );
    }
}
