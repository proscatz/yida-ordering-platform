package com.yida.controller.admin;

import com.yida.dto.EmployeeDTO;
import com.yida.dto.EmployeeLoginDTO;
import com.yida.dto.EmployeePageQueryDTO;
import com.yida.entity.Employee;
import com.yida.properties.JwtProperties;
import com.yida.result.PageResult;
import com.yida.result.Result;
import com.yida.security.TokenService;
import com.yida.service.EmployeeService;
import com.yida.vo.EmployeeLoginVO;
import com.yida.vo.EmployeeProfileVO;
import com.yida.vo.EmployeeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin/employee")
@Slf4j
public class EmployeeController {
    private final EmployeeService employeeService;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    public EmployeeController(EmployeeService employeeService, TokenService tokenService, JwtProperties jwtProperties) {
        this.employeeService = employeeService;
        this.tokenService = tokenService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO request) {
        log.info("员工登录，username={}", request.getUsername());
        Employee employee = employeeService.login(request);
        return Result.success(EmployeeLoginVO.builder().id(employee.getId())
                .userName(employee.getUsername()).name(employee.getName())
                .role(employee.getRole())
                .token(tokenService.issueAdmin(employee.getId())).build());
    }

    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        tokenService.revokeAdmin(request.getHeader(jwtProperties.getAdminTokenName()));
        return Result.success();
    }

    @PostMapping
    public Result save(@RequestBody EmployeeDTO dto) { employeeService.save(dto); return Result.success(); }

    @GetMapping("/me")
    public Result<EmployeeProfileVO> me() {
        return Result.success(employeeService.getCurrentEmployee());
    }

    @GetMapping("/page")
    public Result page(EmployeePageQueryDTO dto) {
        PageResult pageResult = employeeService.page(dto);
        return Result.success(pageResult);
    }

    @PostMapping("/status/{status}")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        employeeService.startOrStop(status, id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<EmployeeVO> getById(@PathVariable Long id) { return Result.success(employeeService.getById(id)); }

    @PutMapping
    public Result update(@RequestBody EmployeeDTO dto) { employeeService.update(dto); return Result.success(); }
}
