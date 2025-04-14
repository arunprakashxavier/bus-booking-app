// src/main/java/com/guvi/busapp/controller/WebController.java
package com.guvi.busapp.controller;

import com.guvi.busapp.dto.*;
import com.guvi.busapp.service.BusService;
import com.guvi.busapp.service.RouteService;
import com.guvi.busapp.service.ScheduledTripService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // Import Value
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller // Dedicated controller for serving web pages using Thymeleaf
public class WebController {

    private static final Logger logger = LoggerFactory.getLogger(WebController.class);

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    @Autowired
    private BusService busService;
    @Autowired
    private RouteService routeService;
    @Autowired
    private ScheduledTripService scheduledTripService;

    // --- Login/Register Pages ---

    @GetMapping("/login")
    public String showLoginPage(Model model, @RequestParam(value = "registrationSuccess", required = false) String registrationSuccess) {
        logger.info("Displaying login page view.");
        if (!model.containsAttribute("loginDto")) {
            model.addAttribute("loginDto", new LoginDto());
        }
        if (registrationSuccess != null && registrationSuccess.equals("true")) {
            model.addAttribute("registrationSuccessMessage", "Registration successful! Please login.");
            logger.info("Registration success message added to login page model.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        logger.info("Displaying registration page view.");
        if (!model.containsAttribute("registerDto")) {
            model.addAttribute("registerDto", new RegisterDto());
        }
        return "register";
    }

    // --- User Pages ---

    @GetMapping("/dashboard")
    public String showUserDashboard(Model model) {
        logger.info("Displaying user dashboard view.");
        return "user-dashboard";
    }

    @GetMapping("/booking/trip/{tripId}/select-seats")
    public String showSeatSelectionPage(@PathVariable Long tripId, Model model) {
        logger.info("Displaying seat selection page for trip ID: {}", tripId);
        model.addAttribute("tripId", tripId);
        return "seat-selection";
    }

    @GetMapping("/booking/confirm")
    public String showBookingConfirmationPage(Model model) {
        logger.info("Displaying booking confirmation / passenger details page view.");
        return "booking-confirmation";
    }

    @GetMapping("/payment/{bookingId}")
    public String showPaymentPage(@PathVariable Long bookingId, Model model) {
        logger.info("Displaying payment page for booking ID: {}", bookingId);
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("stripePublishableKey", stripePublishableKey);
        return "payment";
    }

    @GetMapping("/booking-history")
    public String showBookingHistoryPage(Model model) {
        logger.info("Displaying booking history page view.");
        return "booking-history";
    }

    @GetMapping("/profile")
    public String showUserProfilePage(Model model) {
        logger.info("Displaying user profile page view.");
        return "user-profile";
    }

    // TODO: Add mappings for booking success/failure pages if needed


    // --- Admin Pages ---

    @GetMapping("/admin/dashboard")
    public String showAdminDashboard(Model model) {
        logger.info("Displaying admin dashboard view.");
        return "admin-dashboard";
    }

    @GetMapping("/admin/buses")
    public String showBusListPage(Model model, @RequestParam(value = "error", required = false) String error) {
        logger.info("Displaying admin bus list page view.");
        model.addAttribute("buses", new ArrayList<BusDto>());
        if (error != null) model.addAttribute("errorMessage", "Error: " + error);
        return "admin-buses";
    }
    @GetMapping("/admin/buses/add")
    public String showAddBusForm(Model model) {
        logger.info("Displaying admin add bus form view.");
        model.addAttribute("busDto", new BusDto());
        return "admin-bus-form";
    }
    @GetMapping("/admin/buses/edit/{id}")
    public String showEditBusForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Displaying admin edit bus form view for ID: {}", id);
        try {
            BusDto busDto = busService.getBusById(id);
            model.addAttribute("busDto", busDto);
            return "admin-bus-form";
        } catch (Exception e) {
            logger.error("Error fetching bus for edit with ID {}: {}", id, e.getMessage());
            redirectAttributes.addAttribute("error", "BusNotFound");
            return "redirect:/admin/buses";
        }
    }

    @GetMapping("/admin/routes")
    public String showRouteListPage(Model model, @RequestParam(value = "error", required = false) String error) {
        logger.info("Displaying admin route list page view.");
        model.addAttribute("routes", new ArrayList<RouteDto>());
        if (error != null) model.addAttribute("errorMessage", "Error: " + error);
        return "admin-routes";
    }
    @GetMapping("/admin/routes/add")
    public String showAddRouteForm(Model model) {
        logger.info("Displaying admin add route form view.");
        model.addAttribute("routeDto", new RouteDto());
        return "admin-route-form";
    }
    @GetMapping("/admin/routes/edit/{id}")
    public String showEditRouteForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Displaying admin edit route form view for ID: {}", id);
        try {
            RouteDto routeDto = routeService.getRouteById(id);
            model.addAttribute("routeDto", routeDto);
            return "admin-route-form";
        } catch (Exception e) {
            logger.error("Error fetching route for edit with ID {}: {}", id, e.getMessage());
            redirectAttributes.addAttribute("error", "RouteNotFound");
            return "redirect:/admin/routes";
        }
    }

    @GetMapping("/admin/scheduled-trips")
    public String showTripListPage(Model model, @RequestParam(value = "error", required = false) String error) {
        logger.info("Displaying admin scheduled trip list page view.");
        model.addAttribute("trips", new ArrayList<ScheduledTripResponseDto>());
        if (error != null) model.addAttribute("errorMessage", "Error: " + error);
        return "admin-scheduled-trips";
    }
    @GetMapping("/admin/scheduled-trips/add")
    public String showAddTripForm(Model model) {
        logger.info("Displaying admin add scheduled trip form view.");
        try {
            List<BusDto> buses = busService.getAllBuses();
            List<RouteDto> routes = routeService.getAllRoutes();
            model.addAttribute("scheduledTripRequestDto", new ScheduledTripRequestDto());
            model.addAttribute("buses", buses);
            model.addAttribute("routes", routes);
            return "admin-scheduled-trip-form";
        } catch (Exception e) {
            logger.error("Error loading data for add scheduled trip form: {}", e.getMessage());
            model.addAttribute("errorMessage", "Error loading buses or routes.");
            return "admin-scheduled-trips";
        }
    }
    @GetMapping("/admin/scheduled-trips/edit/{id}")
    public String showEditTripForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Displaying admin edit scheduled trip form view for ID: {}", id);
        try {
            ScheduledTripResponseDto tripDto = scheduledTripService.getScheduledTripById(id);
            List<BusDto> buses = busService.getAllBuses();
            List<RouteDto> routes = routeService.getAllRoutes();
            ScheduledTripRequestDto requestDto = new ScheduledTripRequestDto();
            requestDto.setBusId(tripDto.getBus() != null ? tripDto.getBus().getId() : null);
            requestDto.setRouteId(tripDto.getRoute() != null ? tripDto.getRoute().getId() : null);
            requestDto.setDepartureDate(tripDto.getDepartureDate());
            requestDto.setDepartureTime(tripDto.getDepartureTime());
            requestDto.setArrivalTime(tripDto.getArrivalTime());
            requestDto.setFare(tripDto.getFare());
            model.addAttribute("scheduledTripRequestDto", requestDto);
            model.addAttribute("tripId", id);
            model.addAttribute("buses", buses);
            model.addAttribute("routes", routes);
            return "admin-scheduled-trip-form";
        } catch (Exception e) {
            logger.error("Error fetching scheduled trip for edit with ID {}: {}", id, e.getMessage());
            redirectAttributes.addAttribute("error", "TripNotFound");
            return "redirect:/admin/scheduled-trips";
        }
    }

    // **** ADDED: Mapping for Admin Bookings Page ****
    @GetMapping("/admin/bookings")
    public String showAdminBookingListPage(Model model) {
        logger.info("Displaying admin all bookings list page view.");
        // No specific data needed from controller initially, JS will fetch history via API
        return "admin-bookings";
    }


    // --- Root Path ---
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

}