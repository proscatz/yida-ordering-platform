package com.yida.catalog;

import com.alibaba.druid.pool.DruidDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CatalogDataIntegrityTest.ExternalDataSourceConfiguration.class)
@EnabledIfSystemProperty(named = "catalog.integrity", matches = "true")
class CatalogDataIntegrityTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TestConfiguration
    static class ExternalDataSourceConfiguration {
        @Bean
        DataSource dataSource(Environment environment) {
            DruidDataSource dataSource = new DruidDataSource();
            dataSource.setDriverClassName(environment.getRequiredProperty(
                    "spring.datasource.druid.driver-class-name"));
            dataSource.setUrl(environment.getRequiredProperty("spring.datasource.druid.url"));
            dataSource.setUsername(environment.getRequiredProperty("spring.datasource.druid.username"));
            dataSource.setPassword(environment.getRequiredProperty("spring.datasource.druid.password"));
            return dataSource;
        }
    }

    @Test
    void currentDatabaseMustBeYida() {
        String database = jdbcTemplate.queryForObject("select database()", String.class);
        assertThat(database).isEqualToIgnoringCase("Yida");
    }

    @Test
    void everyEnabledDishCategoryMustExposeAtLeastTenDishes() {
        List<String> deficient = jdbcTemplate.queryForList("""
                select concat(c.name, ':', count(d.id))
                from category c
                left join dish d on d.category_id = c.id and d.status = 1
                where c.type = 1 and c.status = 1
                group by c.id, c.name
                having count(d.id) < 10
                """, String.class);
        assertThat(deficient).as("启用菜品分类的可见菜品数不足 10").isEmpty();
    }

    @Test
    void everyEnabledSetmealCategoryMustExposeAtLeastThreeSetmeals() {
        List<String> deficient = jdbcTemplate.queryForList("""
                select concat(c.name, ':', count(s.id))
                from category c
                left join setmeal s on s.category_id = c.id and s.status = 1
                where c.type = 2 and c.status = 1
                group by c.id, c.name
                having count(s.id) < 3
                """, String.class);
        assertThat(deficient).as("启用套餐分类的可见套餐数不足 3").isEmpty();
    }

    @Test
    void enabledCatalogItemsMustHavePermanentHttpsImages() {
        Integer invalidDishes = jdbcTemplate.queryForObject("""
                select count(*) from dish d
                join category c on c.id = d.category_id
                where d.status = 1 and c.status = 1 and c.type = 1
                  and (d.image is null or trim(d.image) = '' or d.image not like 'https://%')
                """, Integer.class);
        Integer invalidSetmeals = jdbcTemplate.queryForObject("""
                select count(*) from setmeal s
                join category c on c.id = s.category_id
                where s.status = 1 and c.status = 1 and c.type = 2
                  and (s.image is null or trim(s.image) = '' or s.image not like 'https://%')
                """, Integer.class);
        assertThat(invalidDishes).isZero();
        assertThat(invalidSetmeals).isZero();
    }

    @Test
    void dishNamesMustRemainUnique() {
        List<String> duplicates = jdbcTemplate.queryForList("""
                select name from dish group by name having count(*) > 1
                """, String.class);
        assertThat(duplicates).as("存在重复菜品名称").isEmpty();
    }

    @Test
    void flavorsAndSetmealLinksMustNotBeOrphaned() {
        Integer orphanFlavors = jdbcTemplate.queryForObject("""
                select count(*) from dish_flavor f left join dish d on d.id = f.dish_id
                where d.id is null
                """, Integer.class);
        Integer orphanSetmealDishes = jdbcTemplate.queryForObject("""
                select count(*) from setmeal_dish sd
                left join setmeal s on s.id = sd.setmeal_id
                left join dish d on d.id = sd.dish_id
                where s.id is null or d.id is null
                """, Integer.class);
        assertThat(orphanFlavors).isZero();
        assertThat(orphanSetmealDishes).isZero();
    }

    @Test
    void enabledSetmealsMustHaveConsistentDishSnapshots() {
        Integer emptySetmeals = jdbcTemplate.queryForObject("""
                select count(*) from (
                    select s.id from setmeal s
                    left join setmeal_dish sd on sd.setmeal_id = s.id
                    where s.status = 1
                    group by s.id
                    having count(sd.id) = 0
                ) empty_setmeals
                """, Integer.class);
        Integer inconsistentLinks = jdbcTemplate.queryForObject("""
                select count(*) from setmeal_dish sd
                join dish d on d.id = sd.dish_id
                where sd.name <> d.name or sd.price <> d.price or sd.copies < 1
                """, Integer.class);
        assertThat(emptySetmeals == null ? 0 : emptySetmeals).isZero();
        assertThat(inconsistentLinks).isZero();
    }
}
