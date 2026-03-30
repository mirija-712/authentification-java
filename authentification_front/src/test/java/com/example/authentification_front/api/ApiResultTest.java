package com.example.authentification_front.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiResultTest {

	@Test
	void ok_recordHoldsValue() {
		ApiResult.Ok<String> ok = new ApiResult.Ok<>("x");
		assertEquals("x", ok.value());
	}

	@Test
	void err_recordHoldsMessageAndStatus() {
		ApiResult.Err<String> err = new ApiResult.Err<>("oops", 401);
		assertEquals("oops", err.message());
		assertEquals(401, err.httpStatus());
	}
}
