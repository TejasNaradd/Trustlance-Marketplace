package com.trustlance.marketplaceservice.service;

import com.trustlance.marketplaceservice.dto.*;
import com.trustlance.marketplaceservice.exception.AppException;
import com.trustlance.marketplaceservice.model.Category;
import com.trustlance.marketplaceservice.model.ServiceEntity;
import com.trustlance.marketplaceservice.model.ServiceStatus;
import com.trustlance.marketplaceservice.repository.CategoryRepository;
import com.trustlance.marketplaceservice.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private final ServiceRepository serviceRepository;
    private final CategoryRepository categoryRepository;

    // --- Services CRUD ---

    @Transactional
    public ServiceResponse createService(UUID sellerId, CreateServiceRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException("Category not found with id: " + request.getCategoryId(), HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND"));

        ServiceEntity service = ServiceEntity.builder()
                .sellerId(sellerId)
                .categoryId(category.getCategoryId())
                .title(request.getTitle())
                .description(request.getDescription())
                .skills(request.getSkills())
                .price(request.getPrice())
                .deliveryDays(request.getDeliveryDays())
                .status(ServiceStatus.ACTIVE)
                .build();

        return mapToServiceResponse(serviceRepository.save(service), category.getName());
    }

    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(UUID serviceId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new AppException("Service not found with id: " + serviceId, HttpStatus.NOT_FOUND, "SERVICE_NOT_FOUND"));
        String categoryName = categoryRepository.findById(service.getCategoryId())
                .map(Category::getName)
                .orElse("Unknown");
        return mapToServiceResponse(service, categoryName);
    }

    @Transactional(readOnly = true)
    public Page<ServiceResponse> searchServices(String keyword, UUID categoryId, Pageable pageable) {
        return serviceRepository.searchServices(keyword, categoryId, ServiceStatus.ACTIVE, pageable)
                .map(s -> {
                    String catName = categoryRepository.findById(s.getCategoryId()).map(Category::getName).orElse("Unknown");
                    return mapToServiceResponse(s, catName);
                });
    }

    @Transactional(readOnly = true)
    public Page<ServiceResponse> getServicesBySeller(UUID sellerId, Pageable pageable) {
        return serviceRepository.findBySellerId(sellerId, pageable)
                .map(s -> {
                    String catName = categoryRepository.findById(s.getCategoryId()).map(Category::getName).orElse("Unknown");
                    return mapToServiceResponse(s, catName);
                });
    }

    @Transactional
    public ServiceResponse updateService(UUID serviceId, UUID callerId, String callerRole, UpdateServiceRequest request) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new AppException("Service not found with id: " + serviceId, HttpStatus.NOT_FOUND, "SERVICE_NOT_FOUND"));

        if (!service.getSellerId().equals(callerId) && !"ADMIN".equalsIgnoreCase(callerRole)) {
            throw new AppException("You do not have permission to modify this service", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        if (request.getCategoryId() != null) {
            categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException("Category not found", HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND"));
            service.setCategoryId(request.getCategoryId());
        }
        if (request.getTitle() != null) service.setTitle(request.getTitle());
        if (request.getDescription() != null) service.setDescription(request.getDescription());
        if (request.getSkills() != null) service.setSkills(request.getSkills());
        if (request.getPrice() != null) service.setPrice(request.getPrice());
        if (request.getDeliveryDays() != null) service.setDeliveryDays(request.getDeliveryDays());
        if (request.getStatus() != null) service.setStatus(request.getStatus());

        String categoryName = categoryRepository.findById(service.getCategoryId()).map(Category::getName).orElse("Unknown");
        return mapToServiceResponse(serviceRepository.save(service), categoryName);
    }

    @Transactional
    public void deleteService(UUID serviceId, UUID callerId, String callerRole) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new AppException("Service not found with id: " + serviceId, HttpStatus.NOT_FOUND, "SERVICE_NOT_FOUND"));

        if (!service.getSellerId().equals(callerId) && !"ADMIN".equalsIgnoreCase(callerRole)) {
            throw new AppException("You do not have permission to delete this service", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        service.setStatus(ServiceStatus.INACTIVE);
        serviceRepository.save(service);
    }

    // --- Category Management ---

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new AppException("Category already exists: " + request.getName(), HttpStatus.CONFLICT, "CATEGORY_ALREADY_EXISTS");
        }
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .build();
        return mapToCategoryResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException("Category not found with id: " + categoryId, HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND"));
        return mapToCategoryResponse(category);
    }

    private ServiceResponse mapToServiceResponse(ServiceEntity entity, String categoryName) {
        return ServiceResponse.builder()
                .serviceId(entity.getServiceId())
                .sellerId(entity.getSellerId())
                .categoryId(entity.getCategoryId())
                .categoryName(categoryName)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .skills(entity.getSkills())
                .price(entity.getPrice())
                .deliveryDays(entity.getDeliveryDays())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private CategoryResponse mapToCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
