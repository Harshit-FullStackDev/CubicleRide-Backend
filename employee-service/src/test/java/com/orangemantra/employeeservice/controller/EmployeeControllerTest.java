package com.orangemantra.employeeservice.controller;

import com.orangemantra.employeeservice.dto.EmployeeRegisterRequest;
import com.orangemantra.employeeservice.dto.RouteRequest;
import com.orangemantra.employeeservice.model.Employee;
import com.orangemantra.employeeservice.repository.EmployeeRepository;
import com.orangemantra.employeeservice.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;
    @Mock
    private EmployeeRepository repository;

    private EmployeeController controller;

    @BeforeEach
    void setup() {
        controller = new EmployeeController(employeeService, repository);
    }

    @Test
    void health_returnsUpStatus() {
        var map = controller.health();
        assertEquals("UP", map.get("status"));
        assertEquals("employee-service", map.get("service"));
        assertTrue((Long) map.get("timestamp") > 0);
    }

    @Test
    void assignRoute_delegatesToService() {
        RouteRequest req = new RouteRequest();
        req.setEmpId("E1");
        when(employeeService.assignRoute(req)).thenReturn("ok");
        assertEquals("ok", controller.assignRoute(req));
    }

    @Test
    void getProfile_delegates() {
        Employee e = Employee.builder().empId("E1").name("Alice").build();
        when(employeeService.getProfile("E1")).thenReturn(e);
        assertEquals(e, controller.getProfile("E1"));
    }

    @Test
    void saveEmployee_conflictOnExistingEmpId() {
        EmployeeRegisterRequest req = new EmployeeRegisterRequest("E1", "A", "a@b.com", null, null, null, null, null, null);
        when(repository.findAllByEmpId("E1")).thenReturn(List.of(new Employee()));
        ResponseEntity<String> res = controller.saveEmployee(req);
        assertEquals(409, res.getStatusCode().value());
        assertTrue(res.getBody().contains("empId"));
    }

    @Test
    void saveEmployee_conflictOnExistingEmail() {
        EmployeeRegisterRequest req = new EmployeeRegisterRequest("E2", "A", "a@b.com", null, null, null, null, null, null);
        when(repository.findAllByEmpId("E2")).thenReturn(List.of());
        when(repository.existsByEmailHash(anyString())).thenReturn(true);
        ResponseEntity<String> res = controller.saveEmployee(req);
        assertEquals(409, res.getStatusCode().value());
        assertTrue(res.getBody().contains("email"));
    }

    @Test
    void saveEmployee_success() {
        EmployeeRegisterRequest req = new EmployeeRegisterRequest("E3", "A", "a@b.com", "p", "d", "des", "loc", "g", "bio");
        when(repository.findAllByEmpId("E3")).thenReturn(List.of());
        when(repository.existsByEmailHash(anyString())).thenReturn(false);
        when(repository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ResponseEntity<String> res = controller.saveEmployee(req);
        assertEquals(200, res.getStatusCode().value());
        assertEquals("Employee saved successfully", res.getBody());
    }

    @Test
    void getAllEmployees_delegates() {
        List<Employee> list = List.of(Employee.builder().empId("E1").build());
        when(employeeService.getAllEmployees()).thenReturn(list);
        assertEquals(list, controller.getAllEmployees());
    }

    @Test
    void deleteEmployee_delegates() {
        controller.deleteEmployee("E1");
        verify(employeeService).deleteEmployee("E1");
    }

    @Test
    void updateEmployee_delegates() {
        Employee body = Employee.builder().name("A").build();
        Employee saved = Employee.builder().name("A").empId("E1").build();
        when(employeeService.updateEmployee("E1", body)).thenReturn(saved);
        assertEquals(saved, controller.updateEmployee("E1", body));
    }

    @Test
    void getEmployeeName_returnsOk() {
        Employee e = Employee.builder().empId("E1").name("Alice").build();
        when(employeeService.getProfile("E1")).thenReturn(e);
        ResponseEntity<String> res = controller.getEmployeeName("E1");
        assertEquals(200, res.getStatusCode().value());
        assertEquals("Alice", res.getBody());
    }
}

