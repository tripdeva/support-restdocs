package com.tripdeva.testapp.controller;

import com.tripdeva.testapp.dto.ApiResponse;
import com.tripdeva.testapp.dto.ProductRequest;
import com.tripdeva.testapp.dto.ProductResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	@GetMapping
	public ApiResponse<List<ProductResponse>> listProducts(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		List<ProductResponse> products = List.of(
				new ProductResponse(1L, "Laptop", 1200),
				new ProductResponse(2L, "Phone", 800)
		);
		return ApiResponse.ok(products);
	}

	@PostMapping
	public ApiResponse<ProductResponse> createProduct(@RequestBody ProductRequest request) {
		ProductResponse product = new ProductResponse(1L, request.name(), request.price());
		return ApiResponse.ok(product);
	}
}
