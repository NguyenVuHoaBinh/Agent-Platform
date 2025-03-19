package viettel.dac.identityservice.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for consistent pagination across all controllers
 */
@Component
public class PaginationUtils {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_PROPERTY = "createdAt";
    private static final Sort.Direction DEFAULT_SORT_DIRECTION = Sort.Direction.DESC;

    /**
     * Create a standard pageable object with reasonable defaults
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Property to sort by
     * @param direction Sort direction
     * @return Configured Pageable object
     */
    public Pageable createPageable(Integer page, Integer size, String sortBy, String direction) {
        // Set default page if not provided
        int pageNumber = (page != null && page >= 0) ? page : 0;

        // Set default and max size if not provided
        int pageSize = (size != null && size > 0) ? size : DEFAULT_PAGE_SIZE;
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);

        // Set default sort property and direction if not provided
        String sortProperty = (sortBy != null && !sortBy.isEmpty()) ? sortBy : DEFAULT_SORT_PROPERTY;
        Sort.Direction sortDirection = (direction != null && direction.equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : DEFAULT_SORT_DIRECTION;

        // Create and return pageable
        return PageRequest.of(pageNumber, pageSize, Sort.by(sortDirection, sortProperty));
    }

    /**
     * Convert page to standard response format with metadata
     *
     * @param page Page of results
     * @return Map containing page metadata and content
     */
    public <T> Map<String, Object> createPageResponse(Page<T> page) {
        Map<String, Object> response = new HashMap<>();
        response.put("content", page.getContent());
        response.put("currentPage", page.getNumber());
        response.put("totalItems", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("size", page.getSize());
        response.put("hasNext", page.hasNext());
        response.put("hasPrevious", page.hasPrevious());
        return response;
    }
}