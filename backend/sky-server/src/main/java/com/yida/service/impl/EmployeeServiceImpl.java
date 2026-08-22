package com.yida.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.yida.constant.EmployeeRoleConstant;
import com.yida.constant.MessageConstant;
import com.yida.constant.PasswordConstant;
import com.yida.constant.StatusConstant;
import com.yida.context.BaseContext;
import com.yida.dto.EmployeeDTO;
import com.yida.dto.EmployeeLoginDTO;
import com.yida.dto.EmployeePageQueryDTO;
import com.yida.entity.Employee;
import com.yida.exception.AccountLockedException;
import com.yida.exception.AccountNotFoundException;
import com.yida.exception.EmployeeStatusConflictException;
import com.yida.exception.ForbiddenOperationException;
import com.yida.exception.PasswordErrorException;
import com.yida.mapper.EmployeeMapper;
import com.yida.result.PageResult;
import com.yida.security.PasswordHasher;
import com.yida.service.EmployeeService;
import com.yida.vo.EmployeeProfileVO;
import com.yida.vo.EmployeeVO;
import org.springframework.stereotype.Service;

@Service
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeMapper employeeMapper;
    private final PasswordHasher passwordHasher;

    public EmployeeServiceImpl(EmployeeMapper employeeMapper, PasswordHasher passwordHasher) {
        this.employeeMapper = employeeMapper;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public Employee login(EmployeeLoginDTO request) {
        Employee employee = employeeMapper.getByUsername(request.getUsername());
        if (employee == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        String rawPassword = request.getPassword();
        boolean bcrypt = passwordHasher.matchesBcrypt(rawPassword, employee.getPassword());
        boolean legacy = !bcrypt && passwordHasher.matchesLegacyMd5(rawPassword, employee.getPassword());
        if (!bcrypt && !legacy) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        if (StatusConstant.DISABLE.equals(employee.getStatus())) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }
        if (legacy) {
            String upgraded = passwordHasher.encode(rawPassword);
            if (employeeMapper.upgradePassword(employee.getId(), employee.getPassword(), upgraded) == 1) {
                employee.setPassword(upgraded);
            }
        }
        return employee;
    }

    @Override
    public void save(EmployeeDTO dto) {
        requireAdmin();
        Employee employee = Employee.builder()
                .id(dto.getId()).username(dto.getUsername()).name(dto.getName())
                .password(passwordHasher.encode(PasswordConstant.DEFAULT_PASSWORD))
                .phone(dto.getPhone()).sex(dto.getSex()).idNumber(dto.getIdNumber())
                .status(StatusConstant.ENABLE).role(EmployeeRoleConstant.EMPLOYEE).build();
        employeeMapper.insert(employee);
    }

    @Override
    public PageResult page(EmployeePageQueryDTO dto) {
        requireAdmin();
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<EmployeeVO> page = employeeMapper.page(dto);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        if (!StatusConstant.ENABLE.equals(status) && !StatusConstant.DISABLE.equals(status)) {
            throw new EmployeeStatusConflictException(MessageConstant.EMPLOYEE_STATUS_INVALID);
        }
        Employee operator = requireAdmin();
        if (operator.getId().equals(id)) {
            throw new ForbiddenOperationException(MessageConstant.EMPLOYEE_SELF_STATUS_FORBIDDEN);
        }
        Employee target = requireEmployee(id);
        if (!EmployeeRoleConstant.EMPLOYEE.equals(target.getRole())) {
            throw new ForbiddenOperationException(MessageConstant.ADMIN_STATUS_FORBIDDEN);
        }
        if (status.equals(target.getStatus())) {
            throw new EmployeeStatusConflictException(MessageConstant.EMPLOYEE_STATUS_CONFLICT);
        }
        int updated = employeeMapper.updateStatusIfMatch(id, target.getStatus(), status, operator.getId());
        if (updated != 1) {
            throw new EmployeeStatusConflictException(MessageConstant.EMPLOYEE_STATUS_CONFLICT);
        }
    }

    @Override
    public EmployeeVO getById(Long id) {
        requireAdmin();
        return toVO(requireEmployee(id));
    }

    @Override
    public EmployeeProfileVO getCurrentEmployee() {
        Employee employee = requireCurrentEmployee();
        return EmployeeProfileVO.builder()
                .id(employee.getId()).username(employee.getUsername()).name(employee.getName())
                .phone(employee.getPhone()).sex(employee.getSex()).status(employee.getStatus())
                .role(employee.getRole()).build();
    }

    @Override
    public void update(EmployeeDTO dto) {
        requireAdmin();
        Employee target = requireEmployee(dto.getId());
        if (!EmployeeRoleConstant.EMPLOYEE.equals(target.getRole())) {
            throw new ForbiddenOperationException(MessageConstant.ADMIN_STATUS_FORBIDDEN);
        }
        employeeMapper.update(Employee.builder().id(dto.getId()).username(dto.getUsername()).name(dto.getName())
                .phone(dto.getPhone()).sex(dto.getSex()).idNumber(dto.getIdNumber()).build());
    }

    private Employee requireAdmin() {
        Employee current = requireCurrentEmployee();
        if (!EmployeeRoleConstant.ADMIN.equals(current.getRole())) {
            throw new ForbiddenOperationException(MessageConstant.EMPLOYEE_PERMISSION_DENIED);
        }
        return current;
    }

    private Employee requireCurrentEmployee() {
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            throw new ForbiddenOperationException(MessageConstant.EMPLOYEE_PERMISSION_DENIED);
        }
        Employee employee = employeeMapper.getById(currentId);
        if (employee == null || !StatusConstant.ENABLE.equals(employee.getStatus())) {
            throw new ForbiddenOperationException(MessageConstant.EMPLOYEE_PERMISSION_DENIED);
        }
        return employee;
    }

    private Employee requireEmployee(Long id) {
        Employee employee = id == null ? null : employeeMapper.getById(id);
        if (employee == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        return employee;
    }

    private EmployeeVO toVO(Employee employee) {
        return EmployeeVO.builder()
                .id(employee.getId()).username(employee.getUsername()).name(employee.getName())
                .phone(employee.getPhone()).sex(employee.getSex()).idNumber(employee.getIdNumber())
                .status(employee.getStatus()).role(employee.getRole())
                .createTime(employee.getCreateTime()).updateTime(employee.getUpdateTime())
                .createUser(employee.getCreateUser()).updateUser(employee.getUpdateUser()).build();
    }
}
