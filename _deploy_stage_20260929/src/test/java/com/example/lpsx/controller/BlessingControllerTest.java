package com.example.lpsx.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.ibatis.builder.MapperBuilderAssistant;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.Blessing;
import com.example.lpsx.mapper.BlessingMapper;
import com.example.lpsx.vo.BlessingListVO;

class BlessingControllerTest {

    private BlessingMapper blessingMapper;
    private BlessingController controller;
    private AtomicReference<Wrapper<Blessing>> capturedWrapper;

    @BeforeAll
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "BlessingControllerTest"),
                Blessing.class);
    }

    @BeforeEach
    void setUp() {
        capturedWrapper = new AtomicReference<>();
        blessingMapper = (BlessingMapper) Proxy.newProxyInstance(
                BlessingMapper.class.getClassLoader(), new Class<?>[] { BlessingMapper.class },
                (proxy, method, args) -> {
                    if ("selectPage".equals(method.getName())) {
                        capturedWrapper.set((Wrapper<Blessing>) args[1]);
                        return args[0];
                    }
                    if ("toString".equals(method.getName())) return "BlessingMapperTestProxy";
                    throw new UnsupportedOperationException(method.getName());
                });
        controller = new BlessingController();
        ReflectionTestUtils.setField(controller, "blessingMapper", blessingMapper);
    }

    @Test
    void globalListIncludesAllSourcesAndRequiresSchoolName() {
        Result<Page<BlessingListVO>> result = controller.list(1, 6, null, null);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getSize()).isEqualTo(6);

        String sql = capturedWrapper.get().getSqlSegment();
        assertThat(sql).contains("status", "school_name IS NOT NULL", "school_name <>",
                "create_time DESC", "id DESC");
        assertThat(sql).doesNotContain("school_source =", "school_id =", "school_id IS NULL");
    }

    @Test
    void scopedListStillFiltersBySchoolSourceAndId() {
        Result<Page<BlessingListVO>> result = controller.list(1, 20, "score", 52);

        assertThat(result.getCode()).isEqualTo(200);

        String sql = capturedWrapper.get().getSqlSegment();
        assertThat(sql).contains("school_source =", "school_id =", "create_time DESC", "id DESC");
        assertThat(sql).doesNotContain("school_name IS NOT NULL");
        LambdaQueryWrapper<Blessing> wrapper = (LambdaQueryWrapper<Blessing>) capturedWrapper.get();
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains("score", 52);
    }

    @Test
    void globalSecondPagePreservesRequestedPagination() {
        Result<Page<BlessingListVO>> result = controller.list(2, 6, null, null);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getCurrent()).isEqualTo(2);
        assertThat(result.getData().getSize()).isEqualTo(6);
    }
}
