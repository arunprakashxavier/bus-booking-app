package com.guvi.busapp.mapper;

import com.guvi.busapp.dto.RouteDto;
import com.guvi.busapp.model.Route;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RouteMapper {

    /**
     * Maps Route entity to RouteDto.
     * @param route the Route entity
     * @return the RouteDto
     */
    RouteDto routeToRouteDto(Route route);

    /**
     * Maps RouteDto to Route entity.
     * @param routeDto the Route data transfer object
     * @return the Route entity
     */
    @Mapping(target = "id", ignore = true) // ID is usually generated or set from path variable
    Route routeDtoToRoute(RouteDto routeDto);

    /**
     * Maps a list of Route entities to a list of RouteDto objects.
     * @param routes List of Route entities
     * @return List of RouteDto objects
     */
    List<RouteDto> routesToRouteDtos(List<Route> routes);
}
