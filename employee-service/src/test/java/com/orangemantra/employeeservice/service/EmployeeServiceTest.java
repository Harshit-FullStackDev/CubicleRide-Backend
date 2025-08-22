package com.orangemantra.employeeservice.service;

import com.orangemantra.employeeservice.dto.RouteRequest;
import com.orangemantra.employeeservice.exception.EmployeeNotFoundException;
import com.orangemantra.employeeservice.model.Employee;
import com.orangemantra.employeeservice.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    private EmployeeService service;

    @Mock
    private RestTemplate restTemplate; // will be injected via ReflectionTestUtils

    @BeforeEach
    void setUp() {
        service = new EmployeeService(repository);
        // Replace internal RestTemplate with a mock to avoid real HTTP calls
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    @Test
    void assignRoute_savesEmployeeAndReturnsMessage() {
        // given
        String empId = "E001";
        Employee emp = Employee.builder().id(1L).empId(empId).name("Alice").build();
        when(repository.findAllByEmpId(empId)).thenReturn(List.of(emp));
        when(repository.save(emp)).thenReturn(emp);

        RouteRequest request = new RouteRequest();
        request.setEmpId(empId);

        // when
        String result = service.assignRoute(request);

        // then
        assertEquals("Route updated", result);
        verify(repository).save(emp);
    }

    @Test
    void getProfile_returnsFirstBySmallestId_whenDuplicatesExist() {
        String empId = "E001";
        Employee newer = Employee.builder().id(2L).empId(empId).name("Alice").build();
        Employee older = Employee.builder().id(1L).empId(empId).name("Alice").build();
        // repository returns unsorted list (newer first) - must be mutable for service sorting
        when(repository.findAllByEmpId(empId)).thenReturn(new ArrayList<>(List.of(newer, older)));

        Employee found = service.getProfile(empId);

        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void getProfile_throwsWhenNotFound() {
        when(repository.findAllByEmpId("X")).thenReturn(List.of());
        assertThrows(EmployeeNotFoundException.class, () -> service.getProfile("X"));
    }

    @Test
    void getAllEmployees_delegatesToRepository() {
        List<Employee> all = List.of(
                Employee.builder().id(1L).empId("E1").name("A").build(),
                Employee.builder().id(2L).empId("E2").name("B").build(),
                Employee.builder().id(3L).empId("E3").name("C").build()
        );
        when(repository.findAll()).thenReturn(all);

        List<Employee> result = service.getAllEmployees();
        assertEquals(all, result);
    }

    @Test
    void deleteEmployee_throwsWhenNotFound() {
        when(repository.findAllByEmpId("E404")).thenReturn(List.of());
        assertThrows(EmployeeNotFoundException.class, () -> service.deleteEmployee("E404"));
        verify(repository, never()).deleteAll(anyList());
    }

    @Test
    void deleteEmployee_deletesAllMatches() {
        String empId = "E777";
        List<Employee> matches = List.of(
                Employee.builder().id(10L).empId(empId).name("A").build(),
                Employee.builder().id(11L).empId(empId).name("B").build()
        );
        when(repository.findAllByEmpId(empId)).thenReturn(matches);

        service.deleteEmployee(empId);

        verify(repository).deleteAll(matches);
    }

    @Test
    void updateEmployee_updatesFields_withoutCallingAuthServiceWhenNameUnchanged() {
        String empId = "E123";
        Employee stored = Employee.builder()
                .id(1L).empId(empId).name("Alice")
                .phone("111").department("D1").designation("Des1")
                .officeLocation("Loc1").gender("F").bio("bio1")
                .build();
        when(repository.findAllByEmpId(empId)).thenReturn(List.of(stored));

        Employee updated = Employee.builder()
                .name("Alice") // unchanged name
                .phone("222").department("D2").designation("Des2")
                .officeLocation("Loc2").gender("F").bio("bio2")
                .build();

        // repository will return the saved entity
        when(repository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee result = service.updateEmployee(empId, updated);

        assertEquals("Alice", result.getName());
        assertEquals("222", result.getPhone());
        assertEquals("D2", result.getDepartment());
        assertEquals("Des2", result.getDesignation());
        assertEquals("Loc2", result.getOfficeLocation());
        assertEquals("bio2", result.getBio());

        // No external call when name unchanged
        verify(restTemplate, never()).put(anyString(), any());
    }

    @Test
    void updateEmployee_callsAuthServiceWhenNameChanged_andSwallowsExceptions() {
        String empId = "E123";
        Employee stored = Employee.builder().id(1L).empId(empId).name("Alice").build();
        when(repository.findAllByEmpId(empId)).thenReturn(List.of(stored));

        Employee updated = Employee.builder().name("Bob").build();

        when(repository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Prepare mock to throw exception to ensure it is swallowed
        doThrow(new RuntimeException("boom")).when(restTemplate).put(anyString(), any());

        Employee result = service.updateEmployee(empId, updated);

        assertEquals("Bob", result.getName());

        // Verify correct URL and payload structure
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).put(urlCaptor.capture(), entityCaptor.capture());

        String expectedUrl = "http://localhost:8081/auth/user/name/" + empId;
        assertEquals(expectedUrl, urlCaptor.getValue());

        HttpEntity sent = entityCaptor.getValue();
        assertNotNull(sent);
        assertEquals(MediaType.APPLICATION_JSON, sent.getHeaders().getContentType());
        assertEquals("{\"name\":\"Bob\"}", sent.getBody());
    }
}
