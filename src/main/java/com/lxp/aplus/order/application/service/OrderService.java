package com.lxp.aplus.order.application.service;

import com.lxp.aplus.order.application.command.OrderCreateCommand;
import com.lxp.aplus.order.application.port.in.OrderUseCase;
import com.lxp.aplus.order.application.port.out.CoursePrice;
import com.lxp.aplus.order.application.port.out.CourseQueryPort;
import com.lxp.aplus.order.application.result.OrderCreateResult;
import com.lxp.aplus.order.domain.Order;
import com.lxp.aplus.order.domain.OrderItem;
import com.lxp.aplus.order.domain.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService implements OrderUseCase {

    private final OrderRepository orderRepository;
    private final CourseQueryPort courseQueryPort;

    @Override
    public OrderCreateResult createOrder(OrderCreateCommand command) {
        List<CoursePrice> coursePrices = courseQueryPort.getCoursePriceByIds(command.courseIds());

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
}
