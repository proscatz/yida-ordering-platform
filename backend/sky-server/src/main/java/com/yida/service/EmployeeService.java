package com.yida.service;

import com.yida.dto.EmployeeDTO;
import com.yida.dto.EmployeeLoginDTO;
import com.yida.dto.EmployeePageQueryDTO;
import com.yida.entity.Employee;
import com.yida.result.PageResult;
import com.yida.vo.EmployeeProfileVO;
import com.yida.vo.EmployeeVO;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

   

    PageResult page(EmployeePageQueryDTO employeePageQueryDTO);

    void startOrStop(Integer status, Long id);

    EmployeeVO getById(Long id);

    EmployeeProfileVO getCurrentEmployee();

    void update(EmployeeDTO employeedto);

    void save(EmployeeDTO employeedto);
}
