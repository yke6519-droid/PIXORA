package com.example.picturebackend;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.session.SessionRepository;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PictureBackendApplicationTests {

    @Autowired
    private MybatisPlusInterceptor mybatisPlusInterceptor;

    @Autowired
    private SessionRepository<?> sessionRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        // Spring Session Redis 必须完成自动装配，继续保持现有 Session 登录机制。
        assertNotNull(sessionRepository);
    }

    @Test
    void shouldKeepMybatisPlusPaginationConfiguration() {
        // 升级后仍需保留 MySQL 分页拦截器及每页最多 1000 条的业务限制。
        assertEquals(1, mybatisPlusInterceptor.getInterceptors().size());
        assertTrue(mybatisPlusInterceptor.getInterceptors().get(0) instanceof PaginationInnerInterceptor);

        PaginationInnerInterceptor paginationInterceptor =
                (PaginationInnerInterceptor) mybatisPlusInterceptor.getInterceptors().get(0);
        assertEquals(1000L, paginationInterceptor.getMaxLimit());
    }

    @Test
    void shouldExposeOpenApiAndKnife4jDocumentation() throws Exception {
        // 同时访问 JSON 与 UI，避免只启动成功却在真正生成文档时发生版本冲突。
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("PIXORA API"))
                .andExpect(jsonPath("$.info.version").value("v2.0"));
        mockMvc.perform(get("/doc.html"))
                .andExpect(status().isOk());
    }

}
