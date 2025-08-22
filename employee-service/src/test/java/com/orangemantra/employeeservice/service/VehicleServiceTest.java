package com.orangemantra.employeeservice.service;

import com.orangemantra.employeeservice.dto.VehicleRequest;
import com.orangemantra.employeeservice.dto.VehicleResponse;
import com.orangemantra.employeeservice.dto.VehicleVerifyRequest;
import com.orangemantra.employeeservice.model.Vehicle;
import com.orangemantra.employeeservice.repository.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    private VehicleService service;

    @BeforeEach
    void setUp() {
        service = new VehicleService(vehicleRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuth(String empId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(empId, null)
        );
    }

    @Test
    void submitOrUpdate_createsNewPendingAndTrimsLargeImage() {
        setAuth("E123");
        when(vehicleRepository.findByEmpId("E123")).thenReturn(Optional.empty());

        ArgumentCaptor<Vehicle> vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);
        when(vehicleRepository.save(vehicleCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleRequest req = new VehicleRequest();
        req.setMake("Tesla");
        req.setModel("Model 3");
        req.setColor("Blue");
        req.setRegistrationNumber("AB12CD3456");
        req.setCapacity(4);
        req.setProofImageName("rc.png");
        req.setProofImageUrl("a".repeat(2_000_100));

        VehicleResponse res = service.submitOrUpdate(req);

        Vehicle saved = vehicleCaptor.getValue();
        assertEquals("E123", saved.getEmpId());
        assertEquals("PENDING", saved.getStatus());
        assertNull(saved.getRejectionReason());
        assertNotNull(saved.getUpdatedAt());
        assertTrue(saved.getProofImageUrl().length() <= 2_000_000);
        assertEquals(2_000_000, saved.getProofImageUrl().length());

        assertEquals("Tesla", res.getMake());
        assertEquals("Model 3", res.getModel());
        assertEquals("Blue", res.getColor());
        assertEquals("AB12CD3456", res.getRegistrationNumber());
        assertEquals(4, res.getCapacity());
        assertEquals("PENDING", res.getStatus());
        assertNotNull(res.getProofImageUrl());
        assertEquals(2_000_000, res.getProofImageUrl().length());
    }

    @Test
    void submitOrUpdate_existingApproved_resetsToPendingAndClearsVerifiedAt() {
        setAuth("E1");
        Vehicle existing = Vehicle.builder()
                .id(10L)
                .empId("E1")
                .status("APPROVED")
                .verifiedAt(LocalDateTime.now())
                .build();
        when(vehicleRepository.findByEmpId("E1")).thenReturn(Optional.of(existing));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleRequest req = new VehicleRequest();
        req.setMake("Hyundai");
        req.setModel("i20");
        req.setColor("White");
        req.setRegistrationNumber("MH12AB1234");
        req.setCapacity(4);

        VehicleResponse res = service.submitOrUpdate(req);

        assertEquals("PENDING", res.getStatus());
        // ensure the entity passed to save had verifiedAt cleared
        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(captor.capture());
        assertNull(captor.getValue().getVerifiedAt());
        assertEquals("Hyundai", captor.getValue().getMake());
        assertEquals("i20", captor.getValue().getModel());
    }

    @Test
    void myVehicle_returnsWhenExists_else404() {
        setAuth("E9");
        Vehicle v = Vehicle.builder().id(1L).empId("E9").make("T").model("3").status("PENDING").build();
        when(vehicleRepository.findByEmpId("E9")).thenReturn(Optional.of(v));

        VehicleResponse ok = service.myVehicle();
        assertEquals("E9", ok.getEmpId());

        setAuth("E10");
        when(vehicleRepository.findByEmpId("E10")).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.myVehicle());
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void getByEmpId_behaviour() {
        Vehicle v = Vehicle.builder().id(2L).empId("E77").status("PENDING").build();
        when(vehicleRepository.findByEmpId("E77")).thenReturn(Optional.of(v));
        VehicleResponse res = service.getByEmpId("E77");
        assertEquals("E77", res.getEmpId());

        when(vehicleRepository.findByEmpId("E78")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.getByEmpId("E78"));
    }

    @Test
    void verify_invalidStatus_throws400() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(Vehicle.builder().id(1L).build()));
        VehicleVerifyRequest req = new VehicleVerifyRequest();
        req.setStatus("MAYBE");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.verify(1L, req));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void verify_approve_setsVerifiedAtAndClearsReason() {
        Vehicle v = Vehicle.builder().id(5L).status("PENDING").rejectionReason("bad").build();
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(v));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleVerifyRequest req = new VehicleVerifyRequest();
        req.setStatus("APPROVED");
        VehicleResponse res = service.verify(5L, req);

        assertEquals("APPROVED", res.getStatus());
        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(captor.capture());
        assertNull(captor.getValue().getRejectionReason());
        assertNotNull(captor.getValue().getVerifiedAt());
    }

    @Test
    void verify_reject_setsReasonAndClearsVerifiedAt() {
        Vehicle v = Vehicle.builder().id(6L).status("PENDING").verifiedAt(LocalDateTime.now()).build();
        when(vehicleRepository.findById(6L)).thenReturn(Optional.of(v));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleVerifyRequest req = new VehicleVerifyRequest();
        req.setStatus("REJECTED");
        req.setRejectionReason("invalid RC");
        VehicleResponse res = service.verify(6L, req);

        assertEquals("REJECTED", res.getStatus());
        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(captor.capture());
        assertEquals("invalid RC", captor.getValue().getRejectionReason());
        assertNull(captor.getValue().getVerifiedAt());
    }

    @Test
    void allVehicles_mapsEntitiesToResponses() {
        List<Vehicle> list = List.of(
                Vehicle.builder().id(1L).empId("E1").make("M1").status("PENDING").build(),
                Vehicle.builder().id(2L).empId("E2").make("M2").status("APPROVED").build()
        );
        when(vehicleRepository.findAll()).thenReturn(list);

        List<VehicleResponse> res = service.allVehicles();
        assertEquals(2, res.size());
        assertEquals("E1", res.get(0).getEmpId());
        assertEquals("E2", res.get(1).getEmpId());
    }
}

