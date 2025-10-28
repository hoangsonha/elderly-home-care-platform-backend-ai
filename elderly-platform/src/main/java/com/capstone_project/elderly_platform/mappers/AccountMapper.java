//package com.capstone_project.elderly_platform.mappers;
//
//import com.boeing.aircraftservice.dtos.response.AircraftResponseDTO;
//import com.boeing.aircraftservice.dtos.response.AircraftTypeResponseDTO;
//import com.boeing.aircraftservice.pojos.Aircraft;
//import com.boeing.aircraftservice.pojos.AircraftType;
//import org.mapstruct.Mapper;
//import org.mapstruct.Mapping;
//
//@Mapper(componentModel = "spring")
//public interface AccountMapper {
//
//    @Mapping(source = "id", target = "id")
//    @Mapping(source = "aircraftType", target = "aircraftType")
//    @Mapping(source = "deleted", target = "deleted")
//    AircraftResponseDTO aircrafttoAircraftResponseDTO(Aircraft aircraft);
//
//    @Mapping(source = "id", target = "id")
//    @Mapping(source = "seatMap", target = "seatMap")
//    @Mapping(source = "totalSeats", target = "totalSeats")
//    @Mapping(source = "deleted", target = "deleted")
//    AircraftTypeResponseDTO aircraftTypetoAircraftTypeResponseDTO(AircraftType aircraftType);
//
//}
