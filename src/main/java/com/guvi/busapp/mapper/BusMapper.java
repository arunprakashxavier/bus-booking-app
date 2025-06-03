package com.guvi.busapp.mapper;

import com.guvi.busapp.dto.BusDto;
import com.guvi.busapp.model.Bus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BusMapper {

    /**
     * Maps Bus entity to BusDto.
     * @param bus the Bus entity
     * @return the BusDto
     */
    BusDto busToBusDto(Bus bus);

    /**
     * Maps BusDto to Bus entity.
     * @param busDto the Bus data transfer object
     * @return the Bus entity
     */
    @Mapping(target = "id", ignore = true) // ID is usually generated or set from path variable
    Bus busDtoToBus(BusDto busDto);

    /**
     * Maps a list of Bus entities to a list of BusDto objects.
     * @param buses List of Bus entities
     * @return List of BusDto objects
     */
    List<BusDto> busesToBusDtos(List<Bus> buses);
}
