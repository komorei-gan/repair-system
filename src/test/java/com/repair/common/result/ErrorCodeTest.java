package com.repair.common.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 错误码与统一返回结构的基础断言。
 */
class ErrorCodeTest {

    @Test
    void codeMatchesHttpStatus() {
        assertEquals(200, ErrorCode.SUCCESS.getCode());
        assertEquals(400, ErrorCode.PARAM_ERROR.getCode());
        assertEquals(401, ErrorCode.UNAUTHORIZED.getCode());
        assertEquals(403, ErrorCode.FORBIDDEN.getCode());
        assertEquals(404, ErrorCode.NOT_FOUND.getCode());
        assertEquals(500, ErrorCode.SERVER_ERROR.getCode());
    }

    @Test
    void resultFactories() {
        Result<String> ok = Result.success("data");
        assertEquals(200, ok.getCode());
        assertEquals("操作成功", ok.getMsg());
        assertEquals("data", ok.getData());

        Result<Void> err = Result.error(ErrorCode.LOGIN_FAILED);
        assertEquals(401, err.getCode());
        assertEquals("用户名或密码错误", err.getMsg());
        assertNull(err.getData());
    }
}
