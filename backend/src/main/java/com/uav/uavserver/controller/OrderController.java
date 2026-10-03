package com.uav.uavserver.controller;

import com.uav.uavserver.dto.OrderDto;
import com.uav.uavserver.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单接口（对应 front/src/api 的 orderAPI）。
 *   GET    /api/orders                     列表
 *   GET    /api/orders/{id}                详情
 *   GET    /api/orders/status/{status}     按状态
 *   GET    /api/orders/customer/{id}       某客户的订单
 *   GET    /api/orders/no/{orderNo}        按订单号查询（订单跟踪）
 *   GET    /api/orders/{id}/route          航线预检（距离/时长/高度/途经禁飞区）
 *   POST   /api/orders                     下单
 *   PUT    /api/orders/{id}                修改
 *   POST   /api/orders/{id}/accept         平台接单
 *   POST   /api/orders/{id}/dispatch       调度（指派飞行器/机长）
 *   POST   /api/orders/{id}/start          开始配送
 *   POST   /api/orders/{id}/complete       完成
 *   POST   /api/orders/{id}/cancel         取消
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderDto.Response>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<OrderDto.Response> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDto.Response>> getOrdersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status.trim().toUpperCase()));
    }

    @GetMapping("/customer/{id:\\d+}")
    public ResponseEntity<List<OrderDto.Response>> getOrdersByCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(id));
    }

    @GetMapping("/no/{orderNo}")
    public ResponseEntity<OrderDto.Response> getOrderByNumber(@PathVariable String orderNo) {
        return ResponseEntity.ok(orderService.getOrderByNumber(orderNo));
    }

    @GetMapping("/{id:\\d+}/route")
    public ResponseEntity<OrderDto.RouteInfo> getRouteInfo(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getRouteInfo(id));
    }

    @PostMapping
    public ResponseEntity<OrderDto.Response> createOrder(
            @Valid @RequestBody OrderDto.CreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(orderService.createOrder(request, authentication.getName()));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<OrderDto.Response> updateOrder(
            @PathVariable Long id,
            @RequestBody OrderDto.UpdateRequest request) {
        return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    @PostMapping("/{id:\\d+}/accept")
    public ResponseEntity<OrderDto.Response> acceptOrder(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(orderService.acceptOrder(id, authentication.getName()));
    }

    @PostMapping("/{id:\\d+}/dispatch")
    public ResponseEntity<OrderDto.Response> dispatchOrder(
            @PathVariable Long id,
            @RequestBody OrderDto.DispatchRequest request) {
        return ResponseEntity.ok(orderService.dispatchOrder(id, request));
    }

    @PostMapping("/{id:\\d+}/start")
    public ResponseEntity<OrderDto.Response> startOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.startOrder(id));
    }

    @PostMapping("/{id:\\d+}/complete")
    public ResponseEntity<OrderDto.Response> completeOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.completeOrder(id));
    }

    @PostMapping("/{id:\\d+}/cancel")
    public ResponseEntity<OrderDto.Response> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }
}
