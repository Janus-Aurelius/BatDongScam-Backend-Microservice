package com.se100.bds.mappers;

import com.se100.bds.dtos.responses.user.meprofile.CustomerResponse;
import com.se100.bds.dtos.responses.user.meprofile.PropertyOwnerResponse;
import com.se100.bds.dtos.responses.user.meprofile.SalesAgentResponse;
import com.se100.bds.dtos.responses.user.meprofile.MeResponse;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.utils.Constants;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserMapper extends BaseMapper {

    @Autowired
    public UserMapper(ModelMapper modelMapper) {
        super(modelMapper);
    }

    @SuppressWarnings("D")
    @Override
    protected void configureCustomMappings() {
        modelMapper.typeMap(User.class, MeResponse.class)
                .addMappings(mapper -> {
                    // Map ward information
                    mapper.using(ctx -> {
                        User user = (User) ctx.getSource();
                        return user.getWard() != null ? user.getWard().getId() : null;
                    }).map(src -> src, (dest, v) -> dest.setWardId((UUID) v));

                    mapper.using(ctx -> {
                        User user = (User) ctx.getSource();
                        return user.getWard() != null ? user.getWard().getWardName() : null;
                    }).map(src -> src, (dest, v) -> dest.setWardName((String) v));

                    mapper.using(ctx -> {
                        User user = (User) ctx.getSource();
                        return user.getWard() != null && user.getWard().getDistrict() != null ? user.getWard().getDistrict().getId() : null;
                    }).map(src -> src, (dest, v) -> dest.setDistrictId((UUID) v));

                    mapper.using(ctx -> {
                        User user = (User) ctx.getSource();
                        return user.getWard() != null && user.getWard().getDistrict() != null ? user.getWard().getDistrict().getDistrictName() : null;
                    }).map(src -> src, (dest, v) -> dest.setDistrictName((String) v));

                    mapper.using(ctx -> {
                        User user = (User) ctx.getSource();
                        return user.getWard() != null && user.getWard().getDistrict() != null && user.getWard().getDistrict().getCity() != null ? user.getWard().getDistrict().getCity().getId() : null;
                    }).map(src -> src, (dest, v) -> dest.setCityId((UUID) v));

                    mapper.using(ctx -> {
                        User user = (User) ctx.getSource();
                        return user.getWard() != null && user.getWard().getDistrict() != null && user.getWard().getDistrict().getCity() != null ? user.getWard().getDistrict().getCity().getCityName() : null;
                    }).map(src -> src, (dest, v) -> dest.setCityName((String) v));

                    // Map profile based on role
                    mapper.using(ctx -> {
                        User user = (User) ctx.getSource();
                        if (user.getRole() == Constants.RoleEnum.CUSTOMER && user.getCustomer() != null) {
                            return modelMapper.map(user.getCustomer(), CustomerResponse.class);
                        } else if (user.getRole() == Constants.RoleEnum.PROPERTY_OWNER && user.getPropertyOwner() != null) {
                            return modelMapper.map(user.getPropertyOwner(), PropertyOwnerResponse.class);
                        } else if (user.getRole() == Constants.RoleEnum.SALESAGENT && user.getSaleAgent() != null) {
                            return modelMapper.map(user.getSaleAgent(), SalesAgentResponse.class);
                        }
                        return null;
                    }).map(src -> src, (dest, v) -> dest.setProfile(v));
                });
    }
}
