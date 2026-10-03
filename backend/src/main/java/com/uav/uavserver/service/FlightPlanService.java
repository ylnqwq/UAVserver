package com.uav.uavserver.service;

import com.uav.uavserver.domain.*;
import com.uav.uavserver.dto.FlightPlanDto;
import com.uav.uavserver.repository.AircraftRepository;
import com.uav.uavserver.repository.FlightPlanRepository;
import com.uav.uavserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FlightPlanService {

    @Autowired
    private FlightPlanRepository flightPlanRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public FlightPlanDto.Response createFlightPlan(FlightPlanDto.CreateRequest request, String username) {
        Aircraft aircraft = aircraftRepository.findById(request.getAircraftId())
                .orElseThrow(() -> new RuntimeException("飞行器不存在"));

        User pilot = userRepository.findById(request.getPilotId())
                .orElseThrow(() -> new RuntimeException("飞行员不存在"));

        User applicant = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("申请人不存在"));

        FlightPlan plan = new FlightPlan();
        plan.setPlanNumber(generatePlanNumber());
        plan.setPlanName(request.getPlanName());
        plan.setAircraft(aircraft);
        plan.setPilot(pilot);
        plan.setApplicant(applicant);
        plan.setPurpose(request.getPurpose());
        plan.setPlannedStartTime(request.getPlannedStartTime());
        plan.setPlannedEndTime(request.getPlannedEndTime());
        plan.setPlannedDuration((int) ChronoUnit.MINUTES.between(
                request.getPlannedStartTime(), request.getPlannedEndTime()));
        plan.setDepartureLocation(request.getDepartureLocation());
        plan.setArrivalLocation(request.getArrivalLocation());
        plan.setPlannedAltitude(request.getPlannedAltitude());
        plan.setRouteDescription(request.getRouteDescription());
        plan.setRouteCoordinates(request.getRouteCoordinates());
        plan.setRemarks(request.getRemarks());
        plan.setWeatherConditions(request.getWeatherConditions());
        plan.setEmergencyContact(request.getEmergencyContact());
        plan.setEmergencyPhone(request.getEmergencyPhone());
        plan.setStatus(FlightPlanStatus.DRAFT);

        plan = flightPlanRepository.save(plan);
        return toResponse(plan);
    }

    @Transactional
    public FlightPlanDto.Response updateFlightPlan(Long id, FlightPlanDto.UpdateRequest request) {
        FlightPlan plan = flightPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("飞行计划不存在"));

        if (plan.getStatus() != FlightPlanStatus.DRAFT) {
            throw new RuntimeException("只能修改草稿状态的飞行计划");
        }

        if (request.getPlanName() != null) plan.setPlanName(request.getPlanName());
        if (request.getPlannedStartTime() != null) plan.setPlannedStartTime(request.getPlannedStartTime());
        if (request.getPlannedEndTime() != null) plan.setPlannedEndTime(request.getPlannedEndTime());
        if (request.getPlannedStartTime() != null && request.getPlannedEndTime() != null) {
            plan.setPlannedDuration((int) ChronoUnit.MINUTES.between(
                    plan.getPlannedStartTime(), plan.getPlannedEndTime()));
        }
        if (request.getDepartureLocation() != null) plan.setDepartureLocation(request.getDepartureLocation());
        if (request.getArrivalLocation() != null) plan.setArrivalLocation(request.getArrivalLocation());
        if (request.getPlannedAltitude() != null) plan.setPlannedAltitude(request.getPlannedAltitude());
        if (request.getRouteDescription() != null) plan.setRouteDescription(request.getRouteDescription());
        if (request.getRouteCoordinates() != null) plan.setRouteCoordinates(request.getRouteCoordinates());
        if (request.getRemarks() != null) plan.setRemarks(request.getRemarks());
        if (request.getWeatherConditions() != null) plan.setWeatherConditions(request.getWeatherConditions());

        plan = flightPlanRepository.save(plan);
        return toResponse(plan);
    }

    @Transactional
    public FlightPlanDto.Response submitFlightPlan(Long id) {
        FlightPlan plan = flightPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("飞行计划不存在"));

        if (plan.getStatus() != FlightPlanStatus.DRAFT) {
            throw new RuntimeException("只能提交草稿状态的飞行计划");
        }

        plan.setStatus(FlightPlanStatus.SUBMITTED);
        plan.setSubmittedAt(LocalDateTime.now());

        plan = flightPlanRepository.save(plan);
        return toResponse(plan);
    }

    @Transactional
    public FlightPlanDto.Response approveFlightPlan(Long id, FlightPlanDto.ApprovalRequest request, String username) {
        FlightPlan plan = flightPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("飞行计划不存在"));

        if (plan.getStatus() != FlightPlanStatus.SUBMITTED && plan.getStatus() != FlightPlanStatus.UNDER_REVIEW) {
            throw new RuntimeException("只能审批已提交或审核中的飞行计划");
        }

        User approver = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("审批人不存在"));

        plan.setStatus(request.getApproved() ? FlightPlanStatus.APPROVED : FlightPlanStatus.REJECTED);
        plan.setApprovedAt(LocalDateTime.now());
        plan.setApprovedBy(approver);
        plan.setApprovalComments(request.getComments());

        plan = flightPlanRepository.save(plan);
        return toResponse(plan);
    }

    public FlightPlanDto.Response getFlightPlan(Long id) {
        FlightPlan plan = flightPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("飞行计划不存在"));
        return toResponse(plan);
    }

    public List<FlightPlanDto.Response> getAllFlightPlans() {
        return flightPlanRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<FlightPlanDto.Response> getFlightPlansByStatus(FlightPlanStatus status) {
        return flightPlanRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelFlightPlan(Long id) {
        FlightPlan plan = flightPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("飞行计划不存在"));

        if (plan.getStatus() == FlightPlanStatus.COMPLETED || plan.getStatus() == FlightPlanStatus.CANCELLED) {
            throw new RuntimeException("无法取消已完成或已取消的飞行计划");
        }

        plan.setStatus(FlightPlanStatus.CANCELLED);
        flightPlanRepository.save(plan);
    }

    private String generatePlanNumber() {
        return "FP" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private FlightPlanDto.Response toResponse(FlightPlan plan) {
        FlightPlanDto.Response response = new FlightPlanDto.Response();
        response.setId(plan.getId());
        response.setPlanNumber(plan.getPlanNumber());
        response.setPlanName(plan.getPlanName());
        response.setAircraftName(plan.getAircraft().getName());
        response.setAircraftRegistration(plan.getAircraft().getRegistrationNumber());
        response.setPilotName(plan.getPilot().getRealName() != null ? plan.getPilot().getRealName() : plan.getPilot().getUsername());
        response.setApplicantName(plan.getApplicant().getRealName() != null ? plan.getApplicant().getRealName() : plan.getApplicant().getUsername());
        response.setPurpose(plan.getPurpose());
        response.setPlannedStartTime(plan.getPlannedStartTime());
        response.setPlannedEndTime(plan.getPlannedEndTime());
        response.setPlannedDuration(plan.getPlannedDuration());
        response.setDepartureLocation(plan.getDepartureLocation());
        response.setArrivalLocation(plan.getArrivalLocation());
        response.setPlannedAltitude(plan.getPlannedAltitude());
        response.setStatus(plan.getStatus());
        response.setSubmittedAt(plan.getSubmittedAt());
        response.setApprovedAt(plan.getApprovedAt());
        if (plan.getApprovedBy() != null) {
            response.setApprovedByName(plan.getApprovedBy().getRealName() != null ?
                    plan.getApprovedBy().getRealName() : plan.getApprovedBy().getUsername());
        }
        response.setApprovalComments(plan.getApprovalComments());
        response.setCreatedAt(plan.getCreatedAt());
        return response;
    }
}
