package com.se100.bds.data.domains;

import com.se100.bds.models.schemas.customer.CustomerFavoriteProperty;
import com.se100.bds.models.schemas.customer.CustomerPreferredCity;
import com.se100.bds.models.schemas.customer.CustomerPreferredDistrict;
import com.se100.bds.models.schemas.customer.CustomerPreferredPropertyType;
import com.se100.bds.models.schemas.customer.CustomerPreferredWard;
import com.se100.bds.models.entities.location.City;
import com.se100.bds.models.entities.location.District;
import com.se100.bds.models.entities.location.Ward;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.property.PropertyType;
import com.se100.bds.models.entities.user.Customer;
import com.se100.bds.repositories.domains.location.CityRepository;
import com.se100.bds.repositories.domains.location.DistrictRepository;
import com.se100.bds.repositories.domains.location.WardRepository;
import com.se100.bds.repositories.domains.mongo.customer.*;
import com.se100.bds.repositories.domains.property.PropertyRepository;
import com.se100.bds.repositories.domains.property.PropertyTypeRepository;
import com.se100.bds.repositories.domains.user.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomerPreferencesDummyData {

    private final CustomerRepository customerRepository;
    private final PropertyRepository propertyRepository;
    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final PropertyTypeRepository propertyTypeRepository;
    private final CustomerFavoritePropertyRepository favoritePropertyRepository;
    private final CustomerPreferredCityRepository preferredCityRepository;
    private final CustomerPreferredDistrictRepository preferredDistrictRepository;
    private final CustomerPreferredWardRepository preferredWardRepository;
    private final CustomerPreferredPropertyTypeRepository preferredPropertyTypeRepository;

    private final Random random = new Random();

    public void createDummy() {
        createDummyPreferences();
    }

    private void createDummyPreferences() {
        log.info("Creating dummy customer preferences");

        List<Customer> customers = customerRepository.findAll();
        if (customers.isEmpty()) {
            log.warn("Cannot create customer preferences - no customers found");
            return;
        }

        List<Property> properties = propertyRepository.findAll();
        if (properties.isEmpty()) {
            log.warn("Cannot create customer preferences - no properties found");
            return;
        }

        List<City> cities = cityRepository.findAll();
        List<District> districts = districtRepository.findAll();
        List<Ward> wards = wardRepository.findAll();
        List<PropertyType> propertyTypes = propertyTypeRepository.findAll();


        List<CustomerFavoriteProperty> favorites = new ArrayList<>();
        List<CustomerPreferredCity> preferredCities = new ArrayList<>();
        List<CustomerPreferredDistrict> preferredDistricts = new ArrayList<>();
        List<CustomerPreferredWard> preferredWards = new ArrayList<>();
        List<CustomerPreferredPropertyType> preferredTypes = new ArrayList<>();

        for (Customer customer : customers) {
            // Each customer favorites 2-5 properties
            int favoriteCount = 2 + random.nextInt(4);
            for (int i = 0; i < favoriteCount && i < properties.size(); i++) {
                Property property = properties.get(random.nextInt(properties.size()));
                CustomerFavoriteProperty favorite = new CustomerFavoriteProperty(customer.getId(), property.getId());
                favorites.add(favorite);
            }

            // Each customer has 1-2 preferred cities
            int cityCount = 1 + random.nextInt(2);
            for (int i = 0; i < cityCount && i < cities.size(); i++) {
                City city = cities.get(random.nextInt(cities.size()));
                CustomerPreferredCity preferredCity = new CustomerPreferredCity(customer.getId(), city.getId());
                preferredCities.add(preferredCity);
            }

            // Each customer has 1-3 preferred districts
            int districtCount = 1 + random.nextInt(3);
            for (int i = 0; i < districtCount && i < districts.size(); i++) {
                District district = districts.get(random.nextInt(districts.size()));
                CustomerPreferredDistrict preferredDistrict = new CustomerPreferredDistrict(customer.getId(), district.getId());
                preferredDistricts.add(preferredDistrict);
            }

            // Each customer has 1-3 preferred wards
            int wardCount = 1 + random.nextInt(3);
            for (int i = 0; i < wardCount && i < wards.size(); i++) {
                Ward ward = wards.get(random.nextInt(wards.size()));
                CustomerPreferredWard preferredWard = new CustomerPreferredWard(customer.getId(), ward.getId());
                preferredWards.add(preferredWard);
            }

            // Each customer has 1-3 preferred property types
            int typeCount = 1 + random.nextInt(3);
            for (int i = 0; i < typeCount && i < propertyTypes.size(); i++) {
                PropertyType type = propertyTypes.get(random.nextInt(propertyTypes.size()));
                CustomerPreferredPropertyType preferredType = new CustomerPreferredPropertyType(customer.getId(), type.getId());
                preferredTypes.add(preferredType);
            }
        }

        favoritePropertyRepository.saveAll(favorites);
        preferredCityRepository.saveAll(preferredCities);
        preferredDistrictRepository.saveAll(preferredDistricts);
        preferredWardRepository.saveAll(preferredWards);
        preferredPropertyTypeRepository.saveAll(preferredTypes);

        log.info("Saved customer preferences: {} favorites, {} cities, {} districts, {} wards, {} types",
                favorites.size(), preferredCities.size(), preferredDistricts.size(),
                preferredWards.size(), preferredTypes.size());
    }
}
