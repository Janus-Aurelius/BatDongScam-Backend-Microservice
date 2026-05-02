package com.se100.bds.services.domains.location.impl;

import com.se100.bds.dtos.requests.location.CreateLocationRequest;
import com.se100.bds.dtos.requests.location.UpdateLocationRequest;
import com.se100.bds.dtos.responses.location.LocationCardResponse;
import com.se100.bds.dtos.responses.location.LocationDetailsResponse;
import com.se100.bds.mappers.LocationMapper;
import com.se100.bds.models.entities.location.City;
import com.se100.bds.models.entities.location.District;
import com.se100.bds.models.entities.location.Ward;
import com.se100.bds.repositories.domains.location.CityRepository;
import com.se100.bds.repositories.domains.location.DistrictRepository;
import com.se100.bds.repositories.domains.location.WardRepository;
import com.se100.bds.repositories.domains.property.PropertyRepository;
import com.se100.bds.services.domains.location.LocationService;
import com.se100.bds.services.domains.search.SearchService;
import com.se100.bds.services.fileupload.CloudinaryService;
import com.se100.bds.utils.Constants;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {
    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final PropertyRepository propertyRepository;
    private final SearchService searchService;
    private final LocationMapper locationMapper;
    private final CloudinaryService cloudinaryService;


    @Override
    public Page<City> topMostSearchedCities(Pageable pageable) {
        // Lấy tháng và năm hiện tại
        int currentYear = java.time.LocalDateTime.now().getYear();
        int currentMonth = java.time.LocalDateTime.now().getMonthValue();

        // Lấy offset và limit từ Pageable
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        List<UUID> topCityIds = searchService.topMostSearchByUser(
                null,
                offset,
                limit,
                Constants.SearchTypeEnum.CITY,
                currentYear,
                currentMonth
        );
        return cityRepository.findAllByIdIn(topCityIds, pageable);
    }

    @Override
    public Map<UUID, String> findAllByParents(UUID parentId, Constants.SearchTypeEnum searchTypeEnum) {
        Map<UUID, String> results = new HashMap<>();

        switch (searchTypeEnum) {
            case CITY -> {
                List<City> cities = cityRepository.findAll();
                for  (City city : cities) {
                    results.put(city.getId(), city.getCityName());
                }
            }
            case DISTRICT -> {
                List<District> districts = districtRepository.findAllByCity_Id(parentId);
                for  (District district : districts) {
                    results.put(district.getId(), district.getDistrictName());
                }
            }
            case WARD -> {
                List<Ward> wards = wardRepository.findAllByDistrict_Id(parentId);
                for  (Ward ward : wards) {
                    results.put(ward.getId(), ward.getWardName());
                }
            }
            default -> throw new IllegalArgumentException("Invalid searchTypeEnum");
        }

        return results;
    }

    @Override
    public Page<LocationCardResponse> findAllLocationCardsWithFilter(
            Pageable pageable,
            String keyWord,
            List<UUID> cityIds, List<UUID> districtIds,
            Constants.LocationEnum locationTypeEnum,
            Boolean isActive,
            BigDecimal minAvgLandPrice, BigDecimal maxAvgLandPrice,
            BigDecimal minArea, BigDecimal maxArea,
            Integer minPopulation, Integer maxPopulation
    ) {
        switch (locationTypeEnum) {
            case CITY -> {
                Page<City> cities = cityRepository.findAllWithFilters(
                        pageable,
                        keyWord,
                        cityIds,
                        isActive,
                        minAvgLandPrice, maxAvgLandPrice,
                        minArea, maxArea,
                        minPopulation, maxPopulation
                );
                return locationMapper.mapToPage(
                        cities, LocationCardResponse.class
                );
            }
            case DISTRICT -> {
                Page<District> districts = districtRepository.findAllWithFilters(
                        pageable,
                        keyWord,
                        cityIds,
                        districtIds,
                        isActive,
                        minAvgLandPrice, maxAvgLandPrice,
                        minArea, maxArea,
                        minPopulation, maxPopulation
                );
                return locationMapper.mapToPage(
                        districts, LocationCardResponse.class
                );
            }
            case WARD -> {
                Page<Ward> wards = wardRepository.findAllWithFilters(
                        pageable,
                        keyWord,
                        cityIds,
                        districtIds,
                        isActive,
                        minAvgLandPrice, maxAvgLandPrice,
                        minArea, maxArea,
                        minPopulation, maxPopulation
                );
                return locationMapper.mapToPage(
                        wards, LocationCardResponse.class
                );
            }
            default -> throw new IllegalArgumentException("Invalid locationTypeEnum");
        }
    }

    @Override
    public LocationDetailsResponse getLocationDetails(UUID locationId, Constants.LocationEnum locationTypeEnum) {
        LocationDetailsResponse locationDetailsResponse;

        switch (locationTypeEnum) {
            case CITY -> {
                City city = cityRepository.findById(locationId)
                        .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + locationId));
                locationDetailsResponse = locationMapper.mapTo(city, LocationDetailsResponse.class);

                locationDetailsResponse.setActiveProperties(propertyRepository.countActivePropertiesByCityId(city.getId()));
                locationDetailsResponse.setDistrictCount(cityRepository.countDistrictsByCityId(city.getId()));
                locationDetailsResponse.setWardCount(cityRepository.countWardsByCityId(city.getId()));
            }
            case DISTRICT -> {
                District district = districtRepository.findById(locationId)
                        .orElseThrow(() -> new EntityNotFoundException("District not found with id: " + locationId));
                locationDetailsResponse = locationMapper.mapTo(district, LocationDetailsResponse.class);

                locationDetailsResponse.setActiveProperties(propertyRepository.countActivePropertiesByDistrictId(district.getId()));
                locationDetailsResponse.setDistrictCount(0);
                locationDetailsResponse.setWardCount(districtRepository.countWardsByDistrictId(district.getId()));
            }
            case WARD -> {
                Ward ward = wardRepository.findById(locationId)
                        .orElseThrow(() -> new EntityNotFoundException("Ward not found with id: " + locationId));
                locationDetailsResponse = locationMapper.mapTo(ward, LocationDetailsResponse.class);

                locationDetailsResponse.setActiveProperties(propertyRepository.countActivePropertiesByWardId(ward.getId()));
                locationDetailsResponse.setDistrictCount(0);
                locationDetailsResponse.setWardCount(0);
            }
            default -> throw new IllegalArgumentException("Invalid locationTypeEnum");
        }

        return locationDetailsResponse;
    }

    @Override
    @Transactional
    public LocationDetailsResponse create(CreateLocationRequest createLocationRequest) throws IOException {
        LocationDetailsResponse locationDetailsResponse;

        String imgUrl = null;
        if (createLocationRequest.getImage() != null) {
            imgUrl = cloudinaryService.uploadFile(createLocationRequest.getImage(), "locations");
        }

        switch (createLocationRequest.getLocationTypeEnum()) {
            case CITY -> {
                City city = locationMapper.mapTo(createLocationRequest, City.class);
                city.setImgUrl(imgUrl);
                locationDetailsResponse = locationMapper.mapTo(
                        cityRepository.save(city),
                        LocationDetailsResponse.class
                );
            }
            case DISTRICT -> {
                City city = cityRepository.findById(createLocationRequest.getParentId())
                        .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + createLocationRequest.getParentId()));
                District district = locationMapper.mapTo(createLocationRequest, District.class);
                district.setCity(city);
                district.setImgUrl(imgUrl);
                locationDetailsResponse = locationMapper.mapTo(
                        districtRepository.save(district),
                        LocationDetailsResponse.class
                );
            }
            case WARD -> {
                District district = districtRepository.findById(createLocationRequest.getParentId())
                        .orElseThrow(() -> new EntityNotFoundException("District not found with id: " + createLocationRequest.getParentId()));
                Ward ward = locationMapper.mapTo(createLocationRequest, Ward.class);
                ward.setDistrict(district);
                ward.setImgUrl(imgUrl);
                locationDetailsResponse = locationMapper.mapTo(
                        wardRepository.save(ward),
                        LocationDetailsResponse.class
                );
            }
            default -> throw new IllegalArgumentException("Invalid locationTypeEnum");
        }

        locationDetailsResponse.setImgUrl(imgUrl);
        locationDetailsResponse.setActiveProperties(0);
        locationDetailsResponse.setDistrictCount(0);
        locationDetailsResponse.setWardCount(0);

        return locationDetailsResponse;
    }

    @Override
    @Transactional
    public LocationDetailsResponse update(UpdateLocationRequest updateLocationRequest) throws IOException {
        LocationDetailsResponse locationDetailsResponse;

        String imgUrl = null;
        if (updateLocationRequest.getImage() != null) {
            imgUrl = cloudinaryService.uploadFile(updateLocationRequest.getImage(), "locations");
        }

        switch (updateLocationRequest.getLocationTypeEnum()) {
            case CITY -> {
                City city = cityRepository.findById(updateLocationRequest.getId())
                        .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + updateLocationRequest.getId()));

                // Delete old image if exists and new image is provided
                if (imgUrl != null && city.getImgUrl() != null) {
                    cloudinaryService.deleteFile(city.getImgUrl());
                }

                // Update fields only if they are not null
                if (updateLocationRequest.getName() != null) {
                    city.setCityName(updateLocationRequest.getName());
                }
                if (updateLocationRequest.getDescription() != null) {
                    city.setDescription(updateLocationRequest.getDescription());
                }
                if (imgUrl != null) {
                    city.setImgUrl(imgUrl);
                }
                if (updateLocationRequest.getTotalArea() != null) {
                    city.setTotalArea(updateLocationRequest.getTotalArea());
                }
                if (updateLocationRequest.getAvg_land_price() != null) {
                    city.setAvgLandPrice(updateLocationRequest.getAvg_land_price());
                }
                if (updateLocationRequest.getPopulation() != null) {
                    city.setPopulation(updateLocationRequest.getPopulation());
                }
                if (updateLocationRequest.getIsActive() != null) {
                    city.setIsActive(updateLocationRequest.getIsActive());
                }

                City savedCity = cityRepository.save(city);
                locationDetailsResponse = locationMapper.mapTo(savedCity, LocationDetailsResponse.class);
                locationDetailsResponse.setActiveProperties(propertyRepository.countActivePropertiesByCityId(savedCity.getId()));
                locationDetailsResponse.setDistrictCount(cityRepository.countDistrictsByCityId(savedCity.getId()));
                locationDetailsResponse.setWardCount(cityRepository.countWardsByCityId(savedCity.getId()));
            }
            case DISTRICT -> {
                District district = districtRepository.findById(updateLocationRequest.getId())
                        .orElseThrow(() -> new EntityNotFoundException("District not found with id: " + updateLocationRequest.getId()));

                // Delete old image if exists and new image is provided
                if (imgUrl != null && district.getImgUrl() != null) {
                    cloudinaryService.deleteFile(district.getImgUrl());
                }

                // Update parent city if provided
                if (updateLocationRequest.getParentId() != null) {
                    City city = cityRepository.findById(updateLocationRequest.getParentId())
                            .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + updateLocationRequest.getParentId()));
                    district.setCity(city);
                }

                // Update fields only if they are not null
                if (updateLocationRequest.getName() != null) {
                    district.setDistrictName(updateLocationRequest.getName());
                }
                if (updateLocationRequest.getDescription() != null) {
                    district.setDescription(updateLocationRequest.getDescription());
                }
                if (imgUrl != null) {
                    district.setImgUrl(imgUrl);
                }
                if (updateLocationRequest.getTotalArea() != null) {
                    district.setTotalArea(updateLocationRequest.getTotalArea());
                }
                if (updateLocationRequest.getAvg_land_price() != null) {
                    district.setAvgLandPrice(updateLocationRequest.getAvg_land_price());
                }
                if (updateLocationRequest.getPopulation() != null) {
                    district.setPopulation(updateLocationRequest.getPopulation());
                }
                if (updateLocationRequest.getIsActive() != null) {
                    district.setIsActive(updateLocationRequest.getIsActive());
                }

                District savedDistrict = districtRepository.save(district);
                locationDetailsResponse = locationMapper.mapTo(savedDistrict, LocationDetailsResponse.class);
                locationDetailsResponse.setActiveProperties(propertyRepository.countActivePropertiesByDistrictId(savedDistrict.getId()));
                locationDetailsResponse.setDistrictCount(0);
                locationDetailsResponse.setWardCount(districtRepository.countWardsByDistrictId(savedDistrict.getId()));
            }
            case WARD -> {
                Ward ward = wardRepository.findById(updateLocationRequest.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Ward not found with id: " + updateLocationRequest.getId()));

                // Delete old image if exists and new image is provided
                if (imgUrl != null && ward.getImgUrl() != null) {
                    cloudinaryService.deleteFile(ward.getImgUrl());
                }

                // Update parent district if provided
                if (updateLocationRequest.getParentId() != null) {
                    District district = districtRepository.findById(updateLocationRequest.getParentId())
                            .orElseThrow(() -> new EntityNotFoundException("District not found with id: " + updateLocationRequest.getParentId()));
                    ward.setDistrict(district);
                }

                // Update fields only if they are not null
                if (updateLocationRequest.getName() != null) {
                    ward.setWardName(updateLocationRequest.getName());
                }
                if (updateLocationRequest.getDescription() != null) {
                    ward.setDescription(updateLocationRequest.getDescription());
                }
                if (imgUrl != null) {
                    ward.setImgUrl(imgUrl);
                }
                if (updateLocationRequest.getTotalArea() != null) {
                    ward.setTotalArea(updateLocationRequest.getTotalArea());
                }
                if (updateLocationRequest.getAvg_land_price() != null) {
                    ward.setAvgLandPrice(updateLocationRequest.getAvg_land_price());
                }
                if (updateLocationRequest.getPopulation() != null) {
                    ward.setPopulation(updateLocationRequest.getPopulation());
                }
                if (updateLocationRequest.getIsActive() != null) {
                    ward.setIsActive(updateLocationRequest.getIsActive());
                }

                Ward savedWard = wardRepository.save(ward);
                locationDetailsResponse = locationMapper.mapTo(savedWard, LocationDetailsResponse.class);
                locationDetailsResponse.setActiveProperties(propertyRepository.countActivePropertiesByWardId(savedWard.getId()));
                locationDetailsResponse.setDistrictCount(0);
                locationDetailsResponse.setWardCount(0);
            }
            default -> throw new IllegalArgumentException("Invalid locationTypeEnum");
        }

        return locationDetailsResponse;
    }

    @Override
    @Transactional
    public boolean delete(UUID locationId, Constants.LocationEnum locationTypeEnum) {
        try {
            switch (locationTypeEnum) {
                case CITY -> {
                    City city = cityRepository.findById(locationId)
                            .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + locationId));

                    // Delete image from Cloudinary if exists
                    if (city.getImgUrl() != null) {
                        try {
                            cloudinaryService.deleteFile(city.getImgUrl());
                        } catch (IOException e) {
                            log.error("Failed to delete city image from Cloudinary: {}", e.getMessage());
                        }
                    }

                    cityRepository.delete(city);
                    log.info("Successfully deleted city with id: {}", locationId);
                }
                case DISTRICT -> {
                    District district = districtRepository.findById(locationId)
                            .orElseThrow(() -> new EntityNotFoundException("District not found with id: " + locationId));

                    // Delete image from Cloudinary if exists
                    if (district.getImgUrl() != null) {
                        try {
                            cloudinaryService.deleteFile(district.getImgUrl());
                        } catch (IOException e) {
                            log.error("Failed to delete district image from Cloudinary: {}", e.getMessage());
                        }
                    }

                    districtRepository.delete(district);
                    log.info("Successfully deleted district with id: {}", locationId);
                }
                case WARD -> {
                    Ward ward = wardRepository.findById(locationId)
                            .orElseThrow(() -> new EntityNotFoundException("Ward not found with id: " + locationId));

                    // Delete image from Cloudinary if exists
                    if (ward.getImgUrl() != null) {
                        try {
                            cloudinaryService.deleteFile(ward.getImgUrl());
                        } catch (IOException e) {
                            log.error("Failed to delete ward image from Cloudinary: {}", e.getMessage());
                        }
                    }

                    wardRepository.delete(ward);
                    log.info("Successfully deleted ward with id: {}", locationId);
                }
                default -> throw new IllegalArgumentException("Invalid locationTypeEnum: " + locationTypeEnum);
            }
            return true;
        } catch (EntityNotFoundException e) {
            log.error("Entity not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete location: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<UUID> getAllCityIds() {
        return cityRepository.getAllIds();
    }

    @Override
    public List<UUID> getAllDistrictIds() {
        return districtRepository.getAllIds();
    }

    @Override
    public List<UUID> getAllWardIds() {
        return wardRepository.getAllIds();
    }

    @Override
    public String getLocationName(UUID locationId, Constants.LocationEnum locationTypeEnum) {
        switch (locationTypeEnum) {
            case CITY -> {
                return cityRepository.getCityName(locationId);
            }
            case DISTRICT -> {
                String cityName = cityRepository.getCityNameByDistrictId(locationId);
                String districtName = districtRepository.getDistrictName(locationId);
                return districtName + ", " + cityName;
            }
            case WARD ->  {
                String cityName = cityRepository.getCityNameByWardId(locationId);
                String districtName = districtRepository.getDistrictNameByWardId(locationId);
                String wardName = wardRepository.getWardName(locationId);
                return wardName + ", " + districtName + ", " + cityName;
            }
        }

        return "";
    }
}
