package com.yida.service.impl;

import com.yida.constant.EmployeeRoleConstant;
import com.yida.constant.StatusConstant;
import com.yida.context.AuthenticatedPrincipal;
import com.yida.context.AuthenticationContext;
import com.yida.dto.EmployeeDTO;
import com.yida.dto.EmployeeLoginDTO;
import com.yida.entity.Employee;
import com.yida.exception.EmployeeStatusConflictException;
import com.yida.exception.ForbiddenOperationException;
import com.yida.exception.PasswordErrorException;
import com.yida.mapper.EmployeeMapper;
import com.yida.security.PasswordHasher;
import com.yida.vo.EmployeeProfileVO;
import com.yida.vo.EmployeeVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmployeeServiceImplTest {
    private EmployeeMapper mapper;
    private PasswordHasher hasher;
    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(EmployeeMapper.class);
        hasher = new PasswordHasher();
        service = new EmployeeServiceImpl(mapper, hasher);
    }

    @AfterEach
    void tearDown() {
        AuthenticationContext.clear();
    }

    @Test
    void acceptsBcryptPassword() {
        Employee employee = employee(1L, EmployeeRoleConstant.ADMIN, StatusConstant.ENABLE, hasher.encode("secret"));
        when(mapper.getByUsername("admin")).thenReturn(employee);
        assertSame(employee, service.login(login("admin", "secret")));
        verify(mapper, never()).upgradePassword(anyLong(), anyString(), anyString());
    }

    @Test
    void rejectsWrongPassword() {
        when(mapper.getByUsername("admin")).thenReturn(
                employee(1L, EmployeeRoleConstant.ADMIN, StatusConstant.ENABLE, hasher.encode("secret")));
        assertThrows(PasswordErrorException.class, () -> service.login(login("admin", "wrong")));
    }

    @Test
    void upgradesLegacyMd5AfterSuccessfulLogin() {
        String legacy = DigestUtils.md5DigestAsHex("secret".getBytes(StandardCharsets.UTF_8));
        Employee employee = employee(1L, EmployeeRoleConstant.ADMIN, StatusConstant.ENABLE, legacy);
        when(mapper.getByUsername("admin")).thenReturn(employee);
        when(mapper.upgradePassword(eq(1L), eq(legacy), anyString())).thenReturn(1);
        service.login(login("admin", "secret"));
        assertTrue(employee.getPassword().startsWith("$2"));
        assertTrue(hasher.matchesBcrypt("secret", employee.getPassword()));
    }

    @Test
    void administratorCanViewOwnSafeProfile() {
        authenticate(1L);
        when(mapper.getById(1L)).thenReturn(employee(1L, EmployeeRoleConstant.ADMIN, StatusConstant.ENABLE, "hidden"));
        EmployeeProfileVO profile = service.getCurrentEmployee();
        assertEquals(EmployeeRoleConstant.ADMIN, profile.getRole());
        assertEquals(1L, profile.getId());
    }

    @Test
    void ordinaryEmployeeCanViewOwnSafeProfile() {
        authenticate(2L);
        when(mapper.getById(2L)).thenReturn(employee(2L, EmployeeRoleConstant.EMPLOYEE, StatusConstant.ENABLE, "hidden"));
        EmployeeProfileVO profile = service.getCurrentEmployee();
        assertEquals(EmployeeRoleConstant.EMPLOYEE, profile.getRole());
    }

    @Test
    void safeResponseTypesDoNotExposePasswordFields() {
        assertFalse(Arrays.stream(EmployeeVO.class.getDeclaredFields()).anyMatch(field -> field.getName().equals("password")));
        assertFalse(Arrays.stream(EmployeeProfileVO.class.getDeclaredFields()).anyMatch(field -> field.getName().equals("password")));
    }

    @Test
    void administratorCreatesOrdinaryEmployeeWithBcrypt() {
        authenticate(1L);
        when(mapper.getById(1L)).thenReturn(employee(1L, EmployeeRoleConstant.ADMIN, StatusConstant.ENABLE, "hidden"));
        service.save(new EmployeeDTO());
        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(mapper).insert(captor.capture());
        assertTrue(hasher.matchesBcrypt("123456", captor.getValue().getPassword()));
        assertEquals(EmployeeRoleConstant.EMPLOYEE, captor.getValue().getRole());
    }

    @Test
    void administratorCanDisableOrdinaryEmployee() {
        authenticate(1L);
        when(mapper.getById(1L)).thenReturn(employee(1L, EmployeeRoleConstant.ADMIN, StatusConstant.ENABLE, "hidden"));
        when(mapper.getById(2L)).thenReturn(employee(2L, EmployeeRoleConstant.EMPLOYEE, StatusConstant.ENABLE, "hidden"));
        when(mapper.updateStatusIfMatch(2L, 1, 0, 1L)).thenReturn(1);
        service.startOrStop(0, 2L);
        verify(mapper).updateStatusIfMatch(2L, 1, 0, 1L);
    }

    @Test
    void administratorCannotDisableSelf() {
        authenticate(1L);
        when(mapper.getById(1L)).thenReturn(employee(1L, EmployeeRoleConstant.ADMIN, StatusConstant.ENABLE, "hidden"));
        assertThrows(ForbiddenOperationException.class, () -> service.startOrStop(0, 1L));
        verify(mapper, never()).updateStatusIfMatch(anyLong(), anyInt(), anyInt(), anyLong());
    }

    @Test
    void ordinaryEmployeeCannotDisableAnotherEmployee() {
        authenticate(2L);
        when(mapper.getById(2L)).thenReturn(employee(2L, EmployeeRoleConstant.EMPLOYEE, StatusConstant.ENABLE, "hidden"));
        assertThrows(ForbiddenOperationException.class, () -> service.startOrStop(0, 3L));
    }

    @Test
    void ordinaryEmployeeCannotDisableAdministrator() {
        authenticate(2L);
        when(mapper.getById(2L)).thenReturn(employee(2L, EmployeeRoleConstant.EMPLOYEE, StatusConstant.ENABLE, "hidden"));
        assertThrows(ForbiddenOperationException.class, () -> service.startOrStop(0, 1L));
    }

    @Test
    void administratorCannotDisableAnotherAdministrator() {
        authenticate(1L);
        when(mapper.getById(1L)).thenReturn(employee(1L, EmployeeRoleConstant.ADMIN, StatusConstant.ENABLE, "hidden"));
        when(mapper.getById(3L)).thenReturn(employee(3L, EmployeeRoleConstant.ADMIN, StatusConstant.ENABLE, "hidden"));
        assertThrows(ForbiddenOperationException.class, () -> service.startOrStop(0, 3L));
    }

    @Test
    void repeatedStatusChangeIsReportedAsConflict() {
        authenticate(1L);
        when(mapper.getById(1L)).thenReturn(employee(1L, EmployeeRoleConstant.ADMIN, StatusConstant.ENABLE, "hidden"));
        when(mapper.getById(2L)).thenReturn(employee(2L, EmployeeRoleConstant.EMPLOYEE, StatusConstant.DISABLE, "hidden"));
        assertThrows(EmployeeStatusConflictException.class, () -> service.startOrStop(0, 2L));
    }

    @Test
    void concurrentStatusChangeIsReportedWhenConditionalUpdateLosesRace() {
        authenticate(1L);
        when(mapper.getById(1L)).thenReturn(employee(1L, EmployeeRoleConstant.ADMIN, StatusConstant.ENABLE, "hidden"));
        when(mapper.getById(2L)).thenReturn(employee(2L, EmployeeRoleConstant.EMPLOYEE, StatusConstant.ENABLE, "hidden"));
        when(mapper.updateStatusIfMatch(2L, 1, 0, 1L)).thenReturn(0);
        assertThrows(EmployeeStatusConflictException.class, () -> service.startOrStop(0, 2L));
    }

    private void authenticate(Long id) {
        AuthenticationContext.set(new AuthenticatedPrincipal(id, AuthenticatedPrincipal.ADMIN, "test-token-id"));
    }

    private Employee employee(Long id, String role, Integer status, String password) {
        return Employee.builder().id(id).username(id == 1L ? "admin" : "staff")
                .name(id == 1L ? "管理员" : "普通员工").phone("13800000000").sex("1")
                .password(password).status(status).role(role).build();
    }

    private EmployeeLoginDTO login(String username, String password) {
        EmployeeLoginDTO dto = new EmployeeLoginDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }
}
