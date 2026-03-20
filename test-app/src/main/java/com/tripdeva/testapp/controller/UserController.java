package com.tripdeva.testapp.controller;

import com.tripdeva.testapp.dto.ApiResponse;
import com.tripdeva.testapp.dto.ErrorResponse;
import com.tripdeva.testapp.dto.UserRequest;
import com.tripdeva.testapp.dto.UserResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

	@PostMapping
	public ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody UserRequest request) {
		UserResponse user = new UserResponse(1L, request.name(), request.email());
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Request-Id", "req-abc-123");
		return ResponseEntity.status(HttpStatus.CREATED).headers(headers).body(ApiResponse.ok(user));
	}

	@GetMapping("/{id}")
	public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
		UserResponse user = new UserResponse(id, "John", "john@test.com");
		return ApiResponse.ok(user);
	}

	@PutMapping("/{id}")
	public ApiResponse<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
		UserResponse user = new UserResponse(id, request.name(), request.email());
		return ApiResponse.ok(user);
	}

	@PatchMapping("/{id}")
	public ApiResponse<UserResponse> patchUser(@PathVariable Long id, @RequestBody UserRequest request) {
		UserResponse user = new UserResponse(id, request.name() != null ? request.name() : "John", request.email());
		return ApiResponse.ok(user);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteUser(@PathVariable Long id) {
		// no-op
	}

	@GetMapping("/error-example")
	public ResponseEntity<ErrorResponse> errorExample() {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ErrorResponse(404, "NOT_FOUND", "사용자를 찾을 수 없습니다"));
	}
}
