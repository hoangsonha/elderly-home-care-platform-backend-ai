package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.AccountLoginRequest;
import com.capstone_project.elderly_platform.dtos.request.AccountRegisterRequest;
import com.capstone_project.elderly_platform.dtos.request.AccountVerificationRequest;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.dtos.response.TokenResponse;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementExistException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.services.AccountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/accounts")
@RestController
@RefreshScope
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account", description = "Operations related to authentication management")
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/register")
    public ResponseEntity<ObjectResponse> accountRegister(@Valid @RequestBody AccountRegisterRequest accountRegisterRequest) {
        try {
            boolean account = accountService.registerAccount(accountRegisterRequest);
            return account ? ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Đăng ký tài khoản thành công", account)) :
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Đăng ký tài khoản thất bại do không gửi được email", null));
        } catch (BadRequestException e) {
            log.error("Error found : {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (ElementExistException e) {
            log.error("Error found : {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error register user", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Đăng ký tài khoản thất bại", null));
        }
    }

    @PostMapping("/register/verification")
    public ResponseEntity<TokenResponse> userRegister(@Valid @RequestBody AccountVerificationRequest accountRegisterRequest) {
        try {
            TokenResponse tokenResponse = accountService.verificationUser(accountRegisterRequest);
            return ResponseEntity.status(tokenResponse.getCode().equals("Success") ? HttpStatus.OK : HttpStatus.UNAUTHORIZED).body(tokenResponse);
        } catch (BadRequestException e) {
            log.error("Error verification found : {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new TokenResponse("Failed", e.getMessage(), null, null, null, null));
        } catch (Exception e) {
            log.error("Cannot verification : {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    TokenResponse.builder()
                            .code("FAILED")
                            .message("Mã không trùng khớp")
                            .build()
            );
        }
    }

    @PostMapping("/refresh_token")
    public ResponseEntity<TokenResponse> refreshToken(HttpServletRequest request) {
        String refreshToken = request.getHeader("RefreshToken");
        TokenResponse tokenResponse = accountService.refreshToken(refreshToken);
        return ResponseEntity.status(tokenResponse.getCode().equals("Success") ? HttpStatus.OK : HttpStatus.UNAUTHORIZED).body(tokenResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginPage(@Valid @RequestBody AccountLoginRequest accountLoginRequest) {
        try {
            TokenResponse tokenResponse = accountService.login(accountLoginRequest.getEmail(), accountLoginRequest.getPassword());
            return ResponseEntity.status(tokenResponse.getCode().equals("Success") ? HttpStatus.OK : HttpStatus.UNAUTHORIZED).body(tokenResponse);
        } catch (Exception e) {
            log.error("Cannot login : {}", e.toString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    TokenResponse.builder()
                            .code("FAILED")
                            .message("Đăng nhập thất bại")
                            .build()
            );
        }
    }

    // logout
    @PostMapping("/logout")
    public ResponseEntity<ObjectResponse> getLogout(HttpServletRequest request) {
        try {
            boolean checkLogout = accountService.logout(request);
            return checkLogout ? ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Đăng xuất thành công", null)) :
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Failed", "Đăng xuất thất bại", null));
        } catch (ElementNotFoundException e) {
            log.error("Error logout not found : {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Failed", "Đăng xuất thất bại", null));
        } catch (Exception e) {
            log.error("Error logout : {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Failed", "Đăng xuất thất bại", null));
        }
    }


//    private final AircraftService aircraftService;
//
//    @Value("${application.default-current-page}")
//    private int defaultCurrentPage;
//
//    @Value("${application.default-page-size}")
//    private int defaultPageSize;
//
//    /**
//     * Method get all air-crafts
//     *
//     * @param currentPage currentOfThePage
//     * @param pageSize numberOfElement
//     * @return list or empty
//     */
//    @Operation(summary = "Get all air-crafts", description = "Retrieves all air-crafts, with optional pagination")
////    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') or hasRole('STAFF')")
//    @GetMapping("")
//    public ResponseEntity<PagingResponse> getAllAircraft(@RequestParam(value = "currentPage", required = false) Integer currentPage,
//                                                         @RequestParam(value = "pageSize", required = false) Integer pageSize) {
//        int resolvedCurrentPage = (currentPage != null) ? currentPage : defaultCurrentPage;
//        int resolvedPageSize = (pageSize != null) ? pageSize : defaultPageSize;
//        PagingResponse results = aircraftService.getAircraftsPaging(resolvedCurrentPage, resolvedPageSize);
//        List<?> data = (List<?>) results.getData();
//        return ResponseEntity.status(!data.isEmpty() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(results);
//    }
//
//    /**
//     * Method get all air-crafts non paging
//     *
//     * @return list or empty
//     */
//    @Operation(summary = "Get all air-crafts non paging", description = "Retrieves all air-crafts, without optional pagination")
////    @PreAuthorize("hasRole('ADMIN')")
//    @GetMapping("/non-paging")
//    public ResponseEntity<ObjectResponse> getAllAircraftsNonPaging() {
//        List<AircraftResponseDTO> results = aircraftService.getAircrafts();
//        return !results.isEmpty() ? ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "get all aircrafts non paging successfully", results)) :
//                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Failed", "get all aircrafts non paging failed", results));
//    }
//
//    /**
//     * Method search air-crafts with name and sortBy
//     *
//     * @param currentPage currentOfThePage
//     * @param pageSize    numberOfElement
//     * @param code code of aircraft to search
//     * @param model sortBy model with aircraftType
//     * @param manufacturer sortBy manufacturer with aircraftType
//     * @return list or empty
//     */
//    @Operation(summary = "Search air-crafts", description = "Retrieves all air-crafts are filtered by code, model and manufacturer")
////    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
//    @GetMapping("/search")
//    public ResponseEntity<PagingResponse> searchAircraft(@RequestParam(value = "currentPage", required = false) Integer currentPage,
//                                                         @RequestParam(value = "pageSize", required = false) Integer pageSize,
//                                                         @RequestParam(value = "code", required = false, defaultValue = "") String code,
//                                                         @RequestParam(value = "model", required = false, defaultValue = "") String model,
//                                                         @RequestParam(value = "manufacturer", required = false, defaultValue = "") String manufacturer) {
//        int resolvedCurrentPage = (currentPage != null) ? currentPage : defaultCurrentPage;
//        int resolvedPageSize = (pageSize != null) ? pageSize : defaultPageSize;
//
//        PagingResponse results = aircraftService.searchAircrafts(resolvedCurrentPage, resolvedPageSize, code, model, manufacturer);
//        List<?> data = (List<?>) results.getData();
//        return ResponseEntity.status(!data.isEmpty() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(results);
//    }
//
//    /**
//     * Method get aircraft by aircraft id
//     *
//     * @param id idOfAircraft
//     * @return list or empty
//     */
//    @Operation(summary = "Get aircraft by aircraft id", description = "Retrieves aircraft by aircraft id")
////    @PreAuthorize("hasRole('USER') or hasRole('STAFF') or hasRole('ADMIN')")
//    @GetMapping("/{id}")
//    public ResponseEntity<ObjectResponse> getAircraftByID(@PathVariable("id") UUID id) {
//        AircraftResponseDTO aircraft = aircraftService.findById(id);
//        return aircraft != null ?
//                ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Get aircraft by ID successfully", aircraft)) :
//                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Get aircraft by ID failed", null));
//    }
//
//    /**
//     * Method create aircraft
//     *
//     * @param createAircraftRequest param basic for aircraft
//     * @return aircraft or null
//     */
//    @Operation(summary = "Create aircraft", description = "Create aircraft")
////    @PreAuthorize("hasRole('ADMIN')")
//    @PostMapping("")
//    public ResponseEntity<ObjectResponse> createAircraft(@Valid @RequestBody CreateAircraftRequest createAircraftRequest) {
//        try {
//            AircraftResponseDTO aircraft = aircraftService.createAircraft(createAircraftRequest);
//            return ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Create aircraft successfully", aircraft));
//        } catch (BadRequestException e) {
//            log.error("Error creating aircraft", e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", e.getMessage(), null));
//        } catch (ElementExistException e) {
//            log.error("Error creating aircraft", e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", e.getMessage(), null));
//        } catch (Exception e) {
//            log.error("Error creating aircraft", e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Create aircraft failed", null));
//        }
//    }
//
//    /**
//     * Method update aircraft
//     *
//     * @param updateAircraftRequest param basic for aircraft
//     * @return aircraft or null
//     */
//    @Operation(summary = "Update aircraft", description = "Update aircraft")
////    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
//    @PutMapping("/{id}")
//    public ResponseEntity<ObjectResponse> updateAircraft(@PathVariable("id") UUID id, @RequestBody UpdateAircraftRequest updateAircraftRequest) {
//        try {
//            AircraftResponseDTO aircraft = aircraftService.updateAircraft(updateAircraftRequest, id);
//            if (aircraft != null) {
//                return ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Update aircrafts successfully", aircraft));
//            }
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Update aircrafts failed. aircrafts is null", null));
//        } catch (BadRequestException e) {
//            log.error("Error creating aircraft", e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", e.getMessage(), null));
//        } catch (ElementExistException e) {
//            log.error("Error while updating aircraft", e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", e.getMessage(), null));
//        } catch (ElementNotFoundException e) {
//            log.error("Error while updating aircraft", e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Update Aircraft failed. Aircraft not found", null));
//        } catch (Exception e) {
//            log.error("Error updating aircrafts", e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Update aircrafts failed", null));
//        }
//    }
//
//    /**
//     * Method restore aircraft by aircraft id set delete = true
//     *
//     * @param id idOfAircraft
//     * @return aircraft or null
//     */
//    @Operation(summary = "Delete aircraft", description = "Delete aircraft by aircraft id set delete = true")
////    @PreAuthorize("hasRole('ADMIN')")
//    @DeleteMapping("/{id}")
//    public ResponseEntity<ObjectResponse> deleteAircraftByID(@PathVariable("id") UUID id) {
//        try {
//            AircraftResponseDTO aircrafts = aircraftService.deleteAircraft(id);
//            if(aircrafts != null) {
//                return ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Delete aircrafts successfully", aircrafts));
//            }
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Delete aircrafts failed", null));
//        } catch (ElementNotFoundException e) {
//            log.error("Error while updating aircrafts", e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Update Aircraft failed" + e.getMessage(), null));
//        } catch (Exception e) {
//            log.error("Error deleting aircrafts", e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Delete aircrafts failed", null));
//        }
//    }
//
//    /**
//     * Method restore aircraft by aircraft id set delete = false
//     *
//     * @param id idOfAircraft
//     * @return aircraft or null
//     */
//    @Operation(summary = "Restore aircraft", description = "Restore aircraft by aircraft id set delete = false")
////    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
//    @PutMapping("/{id}/restore")
//    public ResponseEntity<ObjectResponse> unDeleteAircraftByID(@PathVariable("id") UUID id) {
//        try {
//            AircraftResponseDTO aircraft = aircraftService.unDeleteAircraft(id);
//            if(aircraft != null) {
//                return ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "UnDelete aircraft successfully", aircraft));
//            }
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Aircraft is null", null));
//        } catch (ElementNotFoundException e) {
//            log.error("Error while updating aircraft", e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Update Aircraft failed" + e.getMessage(), null));
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Undelete aircraft failed", null));
//        }
//    }

}
