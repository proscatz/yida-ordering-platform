package com.yida.integration;

import com.yida.entity.Orders;
import com.yida.mapper.OrderMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class InfrastructureContainerTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.36"))
            .withDatabaseName("yida_test")
            .withUsername("yida")
            .withPassword("yida_test_password");

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @Test
    void mysqlContainerAcceptsJdbcQueries() throws Exception {
        try (Connection connection = mysqlConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            assertTrue(resultSet.next());
            assertEquals(1, resultSet.getInt(1));
        }
    }

    @Test
    void redisContainerRespondsToPing() throws Exception {
        try (Socket socket = new Socket(REDIS.getHost(), REDIS.getMappedPort(6379));
             OutputStream output = socket.getOutputStream();
             BufferedReader input = new BufferedReader(new InputStreamReader(
                     socket.getInputStream(), StandardCharsets.UTF_8))) {
            output.write("*1\r\n$4\r\nPING\r\n".getBytes(StandardCharsets.UTF_8));
            output.flush();
            assertEquals("+PONG", input.readLine());
        }
    }

    @Test
    void userRequestUniqueConstraintAllowsOnlyOneConcurrentOrder() throws Exception {
        execute("drop table if exists order_idempotency_test");
        execute("create table order_idempotency_test ("
                + "id bigint primary key auto_increment, "
                + "number varchar(64) not null, user_id bigint not null, request_id varchar(64) not null, "
                + "unique key uk_number (number), unique key uk_user_request (user_id, request_id))");

        List<Integer> results = runConcurrently(
                () -> insertIdempotentOrder("order-a"),
                () -> insertIdempotentOrder("order-b"));

        assertEquals(1, results.stream().mapToInt(Integer::intValue).sum());
        try (Connection connection = mysqlConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select count(*) from order_idempotency_test")) {
            assertTrue(resultSet.next());
            assertEquals(1, resultSet.getInt(1));
        }
    }

    @Test
    void conditionalStateUpdateAllowsOnlyOneConcurrentWinner() throws Exception {
        execute("drop table if exists order_state_test");
        execute("create table order_state_test (id bigint primary key, status int not null)");
        execute("insert into order_state_test(id, status) values (1, 2)");

        List<Integer> results = runConcurrently(
                () -> updateExpectedState(3),
                () -> updateExpectedState(6));

        assertEquals(1, results.stream().mapToInt(Integer::intValue).sum());
        try (Connection connection = mysqlConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select status from order_state_test where id = 1")) {
            assertTrue(resultSet.next());
            assertTrue(resultSet.getInt(1) == 3 || resultSet.getInt(1) == 6);
        }
    }

    @Test
    void migrationScriptCreatesOrderUniquenessConstraints() throws Exception {
        execute("drop table if exists orders");
        execute("create table orders (id bigint primary key auto_increment, number varchar(64) not null, user_id bigint not null)");
        execute("insert into orders(number, user_id) values ('legacy-order-1', 42)");

        Path migration = Paths.get("sql", "V20260820_01__order_idempotency_and_uniqueness.sql");
        if (!Files.exists(migration)) {
            migration = Paths.get("..", "sql", "V20260820_01__order_idempotency_and_uniqueness.sql");
        }
        assertTrue(Files.exists(migration));
        String migrationSql = new String(Files.readAllBytes(migration), StandardCharsets.UTF_8)
                .replaceAll("(?m)^--.*$", "");
        for (String statementSql : migrationSql.split(";")) {
            String statement = statementSql.trim();
            if (!statement.isEmpty()) {
                execute(statement);
            }
        }

        try (Connection connection = mysqlConnection();
             Statement statement = connection.createStatement();
             ResultSet indexes = statement.executeQuery(
                     "select count(distinct index_name) from information_schema.statistics "
                             + "where table_schema = database() and table_name = 'orders' "
                             + "and index_name in ('uk_orders_number', 'uk_orders_user_request')")) {
            assertTrue(indexes.next());
            assertEquals(2, indexes.getInt(1));
        }
        try (Connection connection = mysqlConnection();
             Statement statement = connection.createStatement();
             ResultSet legacy = statement.executeQuery("select request_id from orders where id = 1")) {
            assertTrue(legacy.next());
            assertEquals("legacy-1", legacy.getString(1));
        }
    }
    @Test
    void authenticationAndPaymentMigrationExecutesOnMySql() throws Exception {
        execute("drop table if exists payment_refund_record");
        execute("drop table if exists payment_callback_record");
        execute("drop table if exists employee");
        execute("drop table if exists user");
        execute("create table employee (id bigint primary key, password varchar(32) not null)");
        execute("create table user (id bigint primary key auto_increment, openid varchar(64), name varchar(64), phone varchar(32), sex varchar(2), id_number varchar(32), avatar varchar(255), create_time datetime)");
        Path migration=Paths.get("sql","V20260820_02__authentication_and_payment_security.sql");
        if(!Files.exists(migration)) migration=Paths.get("..","sql","V20260820_02__authentication_and_payment_security.sql");
        assertTrue(Files.exists(migration));
        String sql=new String(Files.readAllBytes(migration),StandardCharsets.UTF_8).replaceAll("(?m)^--.*$","");
        for(String part:sql.split(";")){String statement=part.trim();if(!statement.isEmpty())execute(statement);}
        try(Connection connection=mysqlConnection();Statement statement=connection.createStatement();
            ResultSet result=statement.executeQuery("select count(*) from information_schema.tables where table_schema=database() and table_name in ('payment_callback_record','payment_refund_record')")){
            assertTrue(result.next());assertEquals(2,result.getInt(1));
        }
        try(Connection connection=mysqlConnection();Statement statement=connection.createStatement();
            ResultSet result=statement.executeQuery("select character_maximum_length from information_schema.columns where table_schema=database() and table_name='employee' and column_name='password'")){
            assertTrue(result.next());assertEquals(100,result.getInt(1));
        }
    }

    @Test
    void reliableMessagingMigrationCreatesOutboxAndIdempotencyTables() throws Exception {
        execute("drop table if exists mq_dead_letter_record");
        execute("drop table if exists message_consume_record");
        execute("drop table if exists order_outbox");
        Path migration = Paths.get("sql", "V20260820_03__redis_and_reliable_order_messaging.sql");
        if (!Files.exists(migration)) {
            migration = Paths.get("..", "sql", "V20260820_03__redis_and_reliable_order_messaging.sql");
        }
        assertTrue(Files.exists(migration));
        String sql = new String(Files.readAllBytes(migration), StandardCharsets.UTF_8)
                .replaceAll("(?m)^--.*$", "");
        for (String part : sql.split(";")) {
            String statement = part.trim();
            if (!statement.isEmpty()) execute(statement);
        }
        try (Connection connection = mysqlConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "select count(*) from information_schema.tables where table_schema=database() "
                             + "and table_name in ('order_outbox','message_consume_record','mq_dead_letter_record')")) {
            assertTrue(result.next());
            assertEquals(3, result.getInt(1));
        }
    }
    @Test
    void orderMapperXmlExecutesConditionalStatusUpdate() throws Exception {
        execute("drop table if exists orders");
        execute("create table orders ("
                + "id bigint primary key auto_increment, number varchar(64) not null, request_id varchar(64) not null, "
                + "status int not null, user_id bigint not null, address_book_id bigint, order_time datetime, "
                + "checkout_time datetime, pay_method int, pay_status int, amount decimal(10,2), remark varchar(255), "
                + "user_name varchar(64), phone varchar(32), address varchar(255), consignee varchar(64), "
                + "cancel_reason varchar(255), rejection_reason varchar(255), cancel_time datetime, "
                + "estimated_delivery_time datetime, delivery_status int, delivery_time datetime, "
                + "pack_amount int not null default 0, tableware_number int not null default 0, tableware_status int, "
                + "unique key uk_orders_number(number), unique key uk_orders_user_request(user_id, request_id))");

        try (SqlSession session = orderMapperSession()) {
            OrderMapper mapper = session.getMapper(OrderMapper.class);
            Orders order = Orders.builder()
                    .number("mapper-order-1")
                    .requestId("mapper-request-1")
                    .status(Orders.TO_BE_CONFIRMED)
                    .userId(42L)
                    .amount(new java.math.BigDecimal("10.00"))
                    .build();
            assertEquals(1, mapper.insert(order));
            session.commit();

            Orders update = Orders.builder().id(order.getId()).status(Orders.CONFIRMED).build();
            assertEquals(1, mapper.updateByIdAndStatus(update, Orders.TO_BE_CONFIRMED));
            assertEquals(0, mapper.updateByIdAndStatus(update, Orders.TO_BE_CONFIRMED));
            session.commit();
        }
    }
    private int insertIdempotentOrder(String orderNumber) throws Exception {
        try (Connection connection = mysqlConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "insert into order_idempotency_test(number, user_id, request_id) values (?, 42, 'request-1')")) {
            statement.setString(1, orderNumber);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState())) {
                return 0;
            }
            throw ex;
        }
    }

    private int updateExpectedState(int targetStatus) throws Exception {
        try (Connection connection = mysqlConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "update order_state_test set status = ? where id = 1 and status = 2")) {
            statement.setInt(1, targetStatus);
            return statement.executeUpdate();
        }
    }

    @SafeVarargs
    private final List<Integer> runConcurrently(Callable<Integer>... operations) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(operations.length);
        CountDownLatch ready = new CountDownLatch(operations.length);
        CountDownLatch start = new CountDownLatch(1);
        try {
            @SuppressWarnings("unchecked")
            Future<Integer>[] futures = new Future[operations.length];
            for (int index = 0; index < operations.length; index++) {
                Callable<Integer> operation = operations[index];
                futures[index] = executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return operation.call();
                });
            }
            ready.await();
            start.countDown();
            Integer[] results = new Integer[futures.length];
            for (int index = 0; index < futures.length; index++) {
                results[index] = futures[index].get();
            }
            return Arrays.asList(results);
        } finally {
            executor.shutdownNow();
        }
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = mysqlConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private SqlSession orderMapperSession() throws Exception {
        PooledDataSource dataSource = new PooledDataSource(
                "com.mysql.cj.jdbc.Driver", MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        Environment environment = new Environment("test", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.getTypeAliasRegistry().registerAlias("Orders", Orders.class);
        try (InputStream input = Resources.getResourceAsStream("mapper/OrderMapper.xml")) {
            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(
                    input, configuration, "mapper/OrderMapper.xml", configuration.getSqlFragments());
            mapperBuilder.parse();
        }
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(configuration);
        return factory.openSession(false);
    }
    private Connection mysqlConnection() throws SQLException {
        return DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }
}