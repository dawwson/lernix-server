package com.lxp.aplus.order.application.service;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.OrderErrorCode;
import com.lxp.aplus.order.application.port.in.OrderUseCase;
import com.lxp.aplus.order.application.port.in.model.command.OrderCreateCommand;
import com.lxp.aplus.order.application.port.in.model.result.OrderCreateResult;
import com.lxp.aplus.order.application.port.out.course.OrderCourseQueryPort;
import com.lxp.aplus.order.application.port.out.course.model.PurchasableCourse;
import com.lxp.aplus.order.application.port.out.event.OrderEventPublisherPort;
import com.lxp.aplus.order.application.port.out.event.model.OrderCompletedEvent;
import com.lxp.aplus.order.application.port.out.repository.OrderRepositoryPort;
import com.lxp.aplus.order.domain.Order;
import com.lxp.aplus.order.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService implements OrderUseCase {

    private final OrderRepositoryPort orderRepository;
    private final OrderCourseQueryPort courseQueryPort;
    private final OrderEventPublisherPort eventPublisher;

    @Override
    public OrderCreateResult createOrder(OrderCreateCommand command) {
        List<PurchasableCourse> coursePrices = courseQueryPort.getPurchasableCourses(command.courseIds());

        List<OrderItem> orderItems = coursePrices.stream()
                .map(coursePrice -> OrderItem.createCourseItem(
                        coursePrice.courseId(),
                        BigDecimal.valueOf(coursePrice.price())
                ))
                .toList();

        Order order = Order.create(command.userId(), orderItems);
        orderRepository.save(order);

        return OrderCreateResult.of(order.getOrderId(), order.getAmount());
    }

    @Override
    public void completeOrder(String orderId, String approvedPaymentId, BigDecimal approvedAmount) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        Order.CompletionResult completionResult =
                order.completeWithApprovedPayment(approvedPaymentId, approvedAmount);

        if (completionResult == Order.CompletionResult.ALREADY_COMPLETED) {
            return;
        }

        // Enrollment가 Order를 조회하지 않도록 수강권 생성에 필요한 항목만 이벤트로 전달합니다.
        List<OrderCompletedEvent.Item> items = order.getOrderItems().stream()
                .map(orderItem -> new OrderCompletedEvent.Item(
                        orderItem.getItemId(),
                        orderItem.getOrderItemId()
                ))
                .toList();

        eventPublisher.publish(
                new OrderCompletedEvent(
                        order.getOrderId(),
                        order.getUserId(),
                        items
                )
        );
    }
}
