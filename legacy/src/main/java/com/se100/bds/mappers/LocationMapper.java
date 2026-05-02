package com.se100.bds.mappers;

import com.se100.bds.dtos.requests.location.CreateLocationRequest;
import com.se100.bds.dtos.requests.location.UpdateLocationRequest;
import com.se100.bds.dtos.responses.location.LocationCardResponse;
import com.se100.bds.dtos.responses.location.LocationDetailsResponse;
import com.se100.bds.models.entities.location.City;
import com.se100.bds.models.entities.location.District;
import com.se100.bds.models.entities.location.Ward;
import com.se100.bds.services.domains.customer.CustomerFavoriteService;
import com.se100.bds.utils.Constants;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LocationMapper extends BaseMapper {

    private final CustomerFavoriteService customerFavoriteService;

    @Autowired
    public LocationMapper(ModelMapper modelMapper, CustomerFavoriteService customerFavoriteService) {
        super(modelMapper);
        this.customerFavoriteService = customerFavoriteService;
    }

    @Override
    protected void configureCustomMappings() {
        modelMapper.typeMap(City.class, LocationCardResponse.class)
                .addMappings(mapper -> {
                    mapper.map(City::getCityName, LocationCardResponse::setName);
                    mapper.map(src -> Constants.LocationEnum.CITY, LocationCardResponse::setLocationTypeEnum);
                    mapper.using(ctx -> {
                        City city = (City) ctx.getSource();
                        return customerFavoriteService.isLikeByMe(city.getId(), Constants.LikeTypeEnum.CITY);
                    }).map(src -> src, LocationCardResponse::setIsFavorite);
                });

        modelMapper.typeMap(District.class, LocationCardResponse.class)
                .addMappings(mapper -> {
                    mapper.map(District::getDistrictName, LocationCardResponse::setName);
                    mapper.map(src -> Constants.LocationEnum.DISTRICT, LocationCardResponse::setLocationTypeEnum);
                    mapper.using(ctx -> {
                        District district = (District) ctx.getSource();
                        return customerFavoriteService.isLikeByMe(district.getId(), Constants.LikeTypeEnum.DISTRICT);
                    }).map(src -> src, LocationCardResponse::setIsFavorite);
                });

        modelMapper.typeMap(Ward.class, LocationCardResponse.class)
                .addMappings(mapper -> {
                    mapper.map(Ward::getWardName, LocationCardResponse::setName);
                    mapper.map(src -> Constants.LocationEnum.WARD, LocationCardResponse::setLocationTypeEnum);
                    mapper.using(ctx -> {
                        Ward ward = (Ward) ctx.getSource();
                        return customerFavoriteService.isLikeByMe(ward.getId(), Constants.LikeTypeEnum.WARD);
                    }).map(src -> src, LocationCardResponse::setIsFavorite);
                });

        modelMapper.typeMap(City.class, LocationDetailsResponse.class)
                .addMappings(mapper -> {
                    mapper.map(City::getCityName, LocationDetailsResponse::setName);
                    mapper.map(src -> Constants.LocationEnum.CITY, LocationDetailsResponse::setLocationTypeEnum);
                    mapper.using(ctx -> {
                        City city = (City) ctx.getSource();
                        return customerFavoriteService.isLikeByMe(city.getId(), Constants.LikeTypeEnum.CITY);
                    }).map(src -> src, LocationDetailsResponse::setIsFavorite);
                });

        modelMapper.typeMap(District.class, LocationDetailsResponse.class)
                .addMappings(mapper -> {
                    mapper.map(District::getDistrictName, LocationDetailsResponse::setName);
                    mapper.map(src -> Constants.LocationEnum.DISTRICT, LocationDetailsResponse::setLocationTypeEnum);
                    mapper.using(ctx -> {
                        District district = (District) ctx.getSource();
                        return customerFavoriteService.isLikeByMe(district.getId(), Constants.LikeTypeEnum.DISTRICT);
                    }).map(src -> src, LocationDetailsResponse::setIsFavorite);
                });

        modelMapper.typeMap(Ward.class, LocationDetailsResponse.class)
                .addMappings(mapper -> {
                    mapper.map(Ward::getWardName, LocationDetailsResponse::setName);
                    mapper.map(src -> Constants.LocationEnum.WARD, LocationDetailsResponse::setLocationTypeEnum);
                    mapper.using(ctx -> {
                        Ward ward = (Ward) ctx.getSource();
                        return customerFavoriteService.isLikeByMe(ward.getId(), Constants.LikeTypeEnum.WARD);
                    }).map(src -> src, LocationDetailsResponse::setIsFavorite);
                });

        // CreateLocationRequest -> City
        modelMapper.typeMap(CreateLocationRequest.class, City.class)
                .addMappings(mapper -> {
                    mapper.map(CreateLocationRequest::getName, City::setCityName);
                    mapper.map(CreateLocationRequest::getAvg_land_price, City::setAvgLandPrice);
                    mapper.skip(City::setId);
                });

        // CreateLocationRequest -> District
        modelMapper.typeMap(CreateLocationRequest.class, District.class)
                .addMappings(mapper -> {
                    mapper.map(CreateLocationRequest::getName, District::setDistrictName);
                    mapper.map(CreateLocationRequest::getAvg_land_price, District::setAvgLandPrice);
                    mapper.skip(District::setId);
                });

        // CreateLocationRequest -> Ward
        modelMapper.typeMap(CreateLocationRequest.class, Ward.class)
                .addMappings(mapper -> {
                    mapper.map(CreateLocationRequest::getName, Ward::setWardName);
                    mapper.map(CreateLocationRequest::getAvg_land_price, Ward::setAvgLandPrice);
                    mapper.skip(Ward::setId);
                });

        // UpdateLocationRequest -> City
        modelMapper.typeMap(UpdateLocationRequest.class, City.class)
                .addMappings(mapper -> {
                    mapper.map(UpdateLocationRequest::getName, City::setCityName);
                    mapper.map(UpdateLocationRequest::getAvg_land_price, City::setAvgLandPrice);
                });

        // UpdateLocationRequest -> District
        modelMapper.typeMap(UpdateLocationRequest.class, District.class)
                .addMappings(mapper -> {
                    mapper.map(UpdateLocationRequest::getName, District::setDistrictName);
                    mapper.map(UpdateLocationRequest::getAvg_land_price, District::setAvgLandPrice);
                });

        // UpdateLocationRequest -> Ward
        modelMapper.typeMap(UpdateLocationRequest.class, Ward.class)
                .addMappings(mapper -> {
                    mapper.map(UpdateLocationRequest::getName, Ward::setWardName);
                    mapper.map(UpdateLocationRequest::getAvg_land_price, Ward::setAvgLandPrice);
                });
    }
}

