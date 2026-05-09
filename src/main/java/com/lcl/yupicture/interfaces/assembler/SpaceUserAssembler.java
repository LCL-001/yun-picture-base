package com.lcl.yupicture.interfaces.assembler;

import com.lcl.yupicture.domain.space.entity.SpaceUser;
import com.lcl.yupicture.interfaces.dto.space.spaceuser.SpaceUserAddRequest;
import com.lcl.yupicture.interfaces.dto.space.spaceuser.SpaceUserEditRequest;
import org.springframework.beans.BeanUtils;

public class SpaceUserAssembler {

    public static SpaceUser toSpaceUserEntity(SpaceUserAddRequest request) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(request, spaceUser);
        return spaceUser;
    }

    public static SpaceUser toSpaceUserEntity(SpaceUserEditRequest request) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(request, spaceUser);
        return spaceUser;
    }
}
