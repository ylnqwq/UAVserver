package com.uav.uavserver.service;

import com.uav.uavserver.domain.Airspace;
import com.uav.uavserver.domain.AirspaceType;
import com.uav.uavserver.dto.AirspaceDto;
import com.uav.uavserver.repository.AirspaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AirspaceService {

    @Autowired
    private AirspaceRepository airspaceRepository;

    @Transactional
    public AirspaceDto.Response createAirspace(AirspaceDto.CreateRequest request) {
        if (airspaceRepository.findByAirspaceCode(request.getAirspaceCode()).isPresent()) {
            throw new RuntimeException("空域编码已存在");
        }

        Airspace airspace = new Airspace();
        airspace.setAirspaceCode(request.getAirspaceCode());
        airspace.setName(request.getName());
        airspace.setType(request.getType());
        airspace.setBoundaries(request.getBoundaries());
        airspace.setMinAltitude(request.getMinAltitude());
        airspace.setMaxAltitude(request.getMaxAltitude());
        airspace.setEffectiveFrom(request.getEffectiveFrom());
        airspace.setEffectiveTo(request.getEffectiveTo());
        airspace.setRestrictions(request.getRestrictions());
        airspace.setManagingAuthority(request.getManagingAuthority());
        airspace.setContactPhone(request.getContactPhone());
        airspace.setDescription(request.getDescription());
        airspace.setIsActive(true);

        airspace = airspaceRepository.save(airspace);
        return toResponse(airspace);
    }

    @Transactional
    public AirspaceDto.Response updateAirspace(Long id, AirspaceDto.UpdateRequest request) {
        Airspace airspace = airspaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("空域不存在"));

        if (request.getName() != null) airspace.setName(request.getName());
        if (request.getType() != null) airspace.setType(request.getType());
        if (request.getBoundaries() != null) airspace.setBoundaries(request.getBoundaries());
        if (request.getMinAltitude() != null) airspace.setMinAltitude(request.getMinAltitude());
        if (request.getMaxAltitude() != null) airspace.setMaxAltitude(request.getMaxAltitude());
        if (request.getIsActive() != null) airspace.setIsActive(request.getIsActive());
        if (request.getEffectiveFrom() != null) airspace.setEffectiveFrom(request.getEffectiveFrom());
        if (request.getEffectiveTo() != null) airspace.setEffectiveTo(request.getEffectiveTo());
        if (request.getRestrictions() != null) airspace.setRestrictions(request.getRestrictions());
        if (request.getManagingAuthority() != null) airspace.setManagingAuthority(request.getManagingAuthority());
        if (request.getContactPhone() != null) airspace.setContactPhone(request.getContactPhone());
        if (request.getDescription() != null) airspace.setDescription(request.getDescription());

        airspace = airspaceRepository.save(airspace);
        return toResponse(airspace);
    }

    public AirspaceDto.Response getAirspace(Long id) {
        Airspace airspace = airspaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("空域不存在"));
        return toResponse(airspace);
    }

    public List<AirspaceDto.Response> getAllAirspaces() {
        return airspaceRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AirspaceDto.Response> getActiveAirspaces() {
        return airspaceRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AirspaceDto.Response> getAirspacesByType(AirspaceType type) {
        return airspaceRepository.findByType(type).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAirspace(Long id) {
        if (!airspaceRepository.existsById(id)) {
            throw new RuntimeException("空域不存在");
        }
        airspaceRepository.deleteById(id);
    }

    private AirspaceDto.Response toResponse(Airspace airspace) {
        AirspaceDto.Response response = new AirspaceDto.Response();
        response.setId(airspace.getId());
        response.setAirspaceCode(airspace.getAirspaceCode());
        response.setName(airspace.getName());
        response.setType(airspace.getType());
        response.setBoundaries(airspace.getBoundaries());
        response.setMinAltitude(airspace.getMinAltitude());
        response.setMaxAltitude(airspace.getMaxAltitude());
        response.setIsActive(airspace.getIsActive());
        response.setEffectiveFrom(airspace.getEffectiveFrom());
        response.setEffectiveTo(airspace.getEffectiveTo());
        response.setRestrictions(airspace.getRestrictions());
        response.setManagingAuthority(airspace.getManagingAuthority());
        response.setContactPhone(airspace.getContactPhone());
        response.setDescription(airspace.getDescription());
        response.setCreatedAt(airspace.getCreatedAt());
        response.setUpdatedAt(airspace.getUpdatedAt());
        return response;
    }
}
