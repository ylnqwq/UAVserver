package com.uav.uavserver.controller;

import com.uav.uavserver.domain.FlightPlanStatus;
import com.uav.uavserver.dto.FlightPlanDto;
import com.uav.uavserver.service.FlightPlanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flight-plans")
public class FlightPlanController {

    @Autowired
    private FlightPlanService flightPlanService;

    @PostMapping
    public ResponseEntity<FlightPlanDto.Response> createFlightPlan(
            @Valid @RequestBody FlightPlanDto.CreateRequest request,
            Authentication authentication) {
        FlightPlanDto.Response response = flightPlanService.createFlightPlan(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlightPlanDto.Response> updateFlightPlan(
            @PathVariable Long id,
            @Valid @RequestBody FlightPlanDto.UpdateRequest request) {
        FlightPlanDto.Response response = flightPlanService.updateFlightPlan(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<FlightPlanDto.Response> submitFlightPlan(@PathVariable Long id) {
        FlightPlanDto.Response response = flightPlanService.submitFlightPlan(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<FlightPlanDto.Response> approveFlightPlan(
            @PathVariable Long id,
            @Valid @RequestBody FlightPlanDto.ApprovalRequest request,
            Authentication authentication) {
        FlightPlanDto.Response response = flightPlanService.approveFlightPlan(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightPlanDto.Response> getFlightPlan(@PathVariable Long id) {
        FlightPlanDto.Response response = flightPlanService.getFlightPlan(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<FlightPlanDto.Response>> getAllFlightPlans() {
        List<FlightPlanDto.Response> response = flightPlanService.getAllFlightPlans();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<FlightPlanDto.Response>> getFlightPlansByStatus(@PathVariable FlightPlanStatus status) {
        List<FlightPlanDto.Response> response = flightPlanService.getFlightPlansByStatus(status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelFlightPlan(@PathVariable Long id) {
        flightPlanService.cancelFlightPlan(id);
        return ResponseEntity.ok().build();
    }
}
