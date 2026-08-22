package com.yida.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CatalogDataIntegrityTest.ExternalDataSourceConfiguration.class)
@EnabledIfSystemProperty(named = "catalog.migrate", matches = "true")
class CatalogDataMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void appliesStageFourCatalogMigrationOnlyToYida() throws Exception {
        String database = jdbcTemplate.queryForObject("select database()", String.class);
        assertThat(database).isEqualToIgnoringCase("Yida");

        Path script = Path.of(System.getProperty("user.dir"))
                .getParent()
                .resolve("sql/V20260822_03__complete_demo_catalog.sql")
                .normalize();
        assertThat(script).isRegularFile();

        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource(script));
        }

        Integer deficientDishCategories = jdbcTemplate.queryForObject("""
                select count(*) from (
                    select c.id from category c
                    left join dish d on d.category_id=c.id and d.status=1
                    where c.type=1 and c.status=1
                    group by c.id
                    having count(d.id)<10
                ) deficient
                """, Integer.class);
        Integer deficientSetmealCategories = jdbcTemplate.queryForObject("""
                select count(*) from (
                    select c.id from category c
                    left join setmeal s on s.category_id=c.id and s.status=1
                    where c.type=2 and c.status=1
                    group by c.id
                    having count(s.id)<3
                ) deficient
                """, Integer.class);
        assertThat(deficientDishCategories).isZero();
        assertThat(deficientSetmealCategories).isZero();
    }
}
