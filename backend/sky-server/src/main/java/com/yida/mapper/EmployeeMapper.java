package com.yida.mapper;

import com.github.pagehelper.Page;
import com.yida.annotation.Autofill;
import com.yida.dto.EmployeePageQueryDTO;
import com.yida.entity.Employee;
import com.yida.enumeration.OperationType;
import com.yida.vo.EmployeeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EmployeeMapper {
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);

    @Autofill(OperationType.INSERT)
    void insert(Employee employee);

    Page<EmployeeVO> page(EmployeePageQueryDTO employeePageQueryDTO);

    @Autofill(OperationType.UPDATE)
    void update(Employee employee);

    @Update("update employee set password = #{password}, update_time = now() where id = #{id} and password = #{legacyPassword}")
    int upgradePassword(@Param("id") Long id, @Param("legacyPassword") String legacyPassword,
                        @Param("password") String password);

    @Select("select * from employee where id = #{id}")
    Employee getById(Long id);

    @Update("update employee set status = #{status}, update_time = now(), update_user = #{operatorId} " +
            "where id = #{id} and status = #{expectedStatus} and role = 'EMPLOYEE' and id <> #{operatorId}")
    int updateStatusIfMatch(@Param("id") Long id,
                            @Param("expectedStatus") Integer expectedStatus,
                            @Param("status") Integer status,
                            @Param("operatorId") Long operatorId);
}
