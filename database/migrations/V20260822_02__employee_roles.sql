-- 员工角色与权限治理增量脚本。
-- 已确认当前 Yida.employee 中主管理员账号 admin 唯一存在。

use yida;

delimiter $$
drop procedure if exists assert_primary_admin_unique$$
create procedure assert_primary_admin_unique()
begin
    declare primary_admin_count int default 0;
    select count(*) into primary_admin_count from employee where username = 'admin';
    if primary_admin_count <> 1 then
        signal sqlstate '45000' set message_text = '主管理员无法唯一确定，员工角色迁移已停止';
    end if;
end$$
call assert_primary_admin_unique()$$
drop procedure assert_primary_admin_unique$$
delimiter ;

set @role_column_exists = (
    select count(*)
    from information_schema.columns
    where table_schema = database() and table_name = 'employee' and column_name = 'role'
);
set @role_column_ddl = if(
    @role_column_exists = 0,
    'alter table employee add column role varchar(20) not null default ''EMPLOYEE'' comment ''员工角色'' after status',
    'select 1'
);
prepare role_column_stmt from @role_column_ddl;
execute role_column_stmt;
deallocate prepare role_column_stmt;

start transaction;
update employee set role = 'EMPLOYEE'
where role is null or role not in ('ADMIN', 'EMPLOYEE');
update employee set role = 'ADMIN'
where username = 'admin' and role <> 'ADMIN';
commit;

set @role_status_index_exists = (
    select count(*)
    from information_schema.statistics
    where table_schema = database() and table_name = 'employee' and index_name = 'idx_employee_role_status'
);
set @role_status_index_ddl = if(
    @role_status_index_exists = 0,
    'create index idx_employee_role_status on employee(role, status)',
    'select 1'
);
prepare role_status_index_stmt from @role_status_index_ddl;
execute role_status_index_stmt;
deallocate prepare role_status_index_stmt;
