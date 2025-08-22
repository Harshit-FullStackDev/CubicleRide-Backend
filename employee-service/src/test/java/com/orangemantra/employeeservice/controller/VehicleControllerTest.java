package com.orangemantra.employeeservice.controller;

import com.orangemantra.employeeservice.dto.VehicleRequest;
import com.orangemantra.employeeservice.dto.VehicleResponse;
import com.orangemantra.employeeservice.dto.VehicleVerifyRequest;
import com.orangemantra.employeeservice.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTest {

    @Mock
    private VehicleService vehicleService;

    private VehicleController controller;

    @BeforeEach
    void setup() {
        controller = new VehicleController(vehicleService);
    }

    @Test
    void submitOrUpdate_delegates() {
        VehicleRequest req = new VehicleRequest();
        VehicleResponse res = VehicleResponse.builder().empId("E1").build();
        when(vehicleService.submitOrUpdate(req)).thenReturn(res);
        assertEquals(res, controller.submitOrUpdate(req));
    }

    @Test
    void myVehicle_delegates() {
        VehicleResponse res = VehicleResponse.builder().empId("E1").build();
        when(vehicleService.myVehicle()).thenReturn(res);
        assertEquals(res, controller.myVehicle());
    }

    @Test
    void getByEmpId_delegates() {
        VehicleResponse res = VehicleResponse.builder().empId("E1").build();
        when(vehicleService.getByEmpId("E1")).thenReturn(res);
        assertEquals(res, controller.getByEmpId("E1"));
    }

    @Test
    void verify_delegates() {
        VehicleVerifyRequest req = new VehicleVerifyRequest();
        req.setStatus("APPROVED");
        VehicleResponse res = VehicleResponse.builder().status("APPROVED").build();
        when(vehicleService.verify(1L, req)).thenReturn(res);
        assertEquals(res, controller.verify(1L, req));
    }

    @Test
    void allVehicles_delegates() {
        List<VehicleResponse> list = List.of(VehicleResponse.builder().empId("E1").build());
        when(vehicleService.allVehicles()).thenReturn(list);
        assertEquals(list, controller.allVehicles());
    }
}

