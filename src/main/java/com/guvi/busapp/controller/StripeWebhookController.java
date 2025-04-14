// src/main/java/com/guvi/busapp/controller/StripeWebhookController.java
package com.guvi.busapp.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import com.guvi.busapp.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment; // Keep Environment for loading secret
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

    @Autowired
    private PaymentService paymentService;

    // Use Environment to load secret as @Value wasn't working reliably before
    @Autowired
    private Environment env;

    // Gson instance for manual parsing
    private static final Gson gson = new Gson();

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload, // Raw request body
            @RequestHeader("Stripe-Signature") String sigHeader)
    {
        String endpointSecret = env.getProperty("stripe.webhook.secret");

        if (sigHeader == null) {
            logger.warn("Webhook received without Stripe-Signature header.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing Stripe-Signature header");
        }
        if (endpointSecret == null || endpointSecret.isBlank()) {
            logger.error("Stripe Webhook Secret ('stripe.webhook.secret') could not be read from environment/properties!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook secret not configured");
        }

        Event event;
        try {
            // Step 1: Verify signature first (still uses Stripe library)
            logger.info("Attempting to verify webhook signature using secret (from Environment) ending with: ...{}",
                    endpointSecret.length() > 4 ? endpointSecret.substring(endpointSecret.length() - 4) : endpointSecret);
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            logger.info("Webhook signature VERIFIED for event ID: {}", event.getId());

        } catch (SignatureVerificationException e) {
            logger.warn("Webhook Error: Invalid Stripe signature. Message: [{}]. Check secrets.", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (JsonSyntaxException e) { // Catch JsonSyntaxException during constructEvent
            logger.warn("Webhook Error: Invalid JSON payload during signature verification. {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        } catch (Exception e) { // Catch other potential errors during event construction
            logger.error("Webhook Error: Could not construct base event object. {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }

        // Step 2: Manually Parse Necessary Data (since deserializer failed)
        String eventType = event.getType();
        String paymentIntentId = null;
        Long bookingId = null;
        Long amount = null;
        String currency = null;

        try {
            // Parse the raw payload string into a JsonObject
            JsonObject jsonObject = JsonParser.parseString(payload).getAsJsonObject();
            JsonObject dataObject = jsonObject.getAsJsonObject("data").getAsJsonObject("object");

            // Extract required fields
            if (dataObject.has("id")) {
                paymentIntentId = dataObject.get("id").getAsString();
            }
            if (dataObject.has("metadata")) {
                JsonObject metadata = dataObject.getAsJsonObject("metadata");
                if (metadata.has("booking_id")) {
                    bookingId = metadata.get("booking_id").getAsLong();
                }
            }
            // Extract optional fields for success handler
            if (eventType.equals("payment_intent.succeeded")) {
                if (dataObject.has("amount")) {
                    amount = dataObject.get("amount").getAsLong();
                }
                if (dataObject.has("currency")) {
                    currency = dataObject.get("currency").getAsString();
                }
            }


            // Validate essential extracted data
            if (paymentIntentId == null || bookingId == null) {
                logger.error("Webhook Error: Failed to extract paymentIntentId or bookingId from webhook payload for event type {}. Payload: {}", eventType, payload);
                // Acknowledge receipt but indicate processing failure
                return ResponseEntity.status(HttpStatus.ACCEPTED).body("Webhook received but missing essential data in payload.");
            }

            logger.info("Manually Parsed Data: Type='{}', PI_ID='{}', Booking_ID={}", eventType, paymentIntentId, bookingId);


        } catch (JsonSyntaxException | IllegalStateException | NullPointerException | NumberFormatException e) {
            logger.error("Webhook Error: Failed to manually parse JSON payload or extract required fields for event ID: {}. Error: {}", event.getId(), e.getMessage(), e);
            // Acknowledge receipt but indicate processing failure
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("Webhook received but failed to parse essential data.");
        }


        // Step 3: Handle the event using extracted data
        switch (eventType) {
            case "payment_intent.succeeded":
                logger.info("Webhook: Calling handlePaymentSuccess for PI_ID={}, Booking_ID={}", paymentIntentId, bookingId);
                // Pass extracted data to the service method
                paymentService.handlePaymentSuccess(paymentIntentId, bookingId, amount, currency);
                break;
            case "payment_intent.payment_failed":
                logger.warn("Webhook: Calling handlePaymentFailure for PI_ID={}, Booking_ID={}", paymentIntentId, bookingId);
                // Pass extracted data to the service method
                paymentService.handlePaymentFailure(paymentIntentId, bookingId);
                break;
            default:
                logger.debug("Webhook: Unhandled event type: {}", eventType);
        }

        return ResponseEntity.ok().body("Webhook Handled");
    }
}