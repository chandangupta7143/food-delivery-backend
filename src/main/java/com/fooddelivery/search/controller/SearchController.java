package com.fooddelivery.search.controller;

import com.fooddelivery.common.response.ApiResponse;
import com.fooddelivery.common.response.PaginatedResponse;
import com.fooddelivery.search.dto.AdvancedSearchRequest;
import com.fooddelivery.search.dto.AdvancedSearchResponse;
import com.fooddelivery.search.service.SearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@Validated
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<AdvancedSearchResponse>>> search(
            @Valid @RequestBody AdvancedSearchRequest request) {
        PaginatedResponse<AdvancedSearchResponse> response = searchService.search(request);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", response));
    }
}
