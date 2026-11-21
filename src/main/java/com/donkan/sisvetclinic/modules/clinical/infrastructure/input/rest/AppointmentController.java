package com.donkan.sisvetclinic.modules.clinical.infrastructure.input.rest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.donkan.sisvetclinic.modules.clinical.domain.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.*;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.*;

@RestController
@RequestMapping("/api/clinical/appointments")
public class AppointmentController {

    private final AppointmentUseCase appointments;
    private final FollowUpUseCase followUps;
    private final ClinicalQueryUseCase queries;

    public AppointmentController(
        AppointmentUseCase appointments,
        FollowUpUseCase followUps
    ) {
        this(appointments, followUps, null);
    }

    @Autowired
    public AppointmentController(
        AppointmentUseCase appointments,
        FollowUpUseCase followUps,
        ClinicalQueryUseCase queries
    ) {
        this.appointments = appointments;
        this.followUps = followUps;
        this.queries = queries;
    }

    @GetMapping("/professionals")
    public List<ProfessionalOption> professionals() {
        return queries.listActiveProfessionals();
    }

    @PostMapping("/professionals/{id}/specialties")
    public void addSpecialty(
        @PathVariable long id,
        @RequestBody Map<String, String> body
    ) {
        String specialty = body == null ? null : body.get("specialty");
        queries.assignSpecialty(id, specialty);
    }

    @DeleteMapping("/professionals/{id}/specialties")
    public void removeSpecialty(
        @PathVariable long id,
        @RequestParam String specialty
    ) {
        queries.removeSpecialty(id, specialty);
    }

    @PostMapping("/availability-views")
    public List<AppointmentView> availabilityViews(
        @RequestBody AvailabilityFilter filter
    ) {
        Long profId = filter != null && filter.professionalId() > 0 ? filter.professionalId() : null;
        return queries.listAppointmentViews(
            profId,
            filter == null ? null : filter.start(),
            filter == null ? null : filter.end()
        );
    }

    @PostMapping("/availability")
    public List<Appointment> availability(
        @RequestBody AvailabilityFilter filter
    ) {
        return appointments.getAvailability(filter);
    }

    @PostMapping
    public Appointment schedule(@RequestBody AppointmentRequest request) {
        return appointments.scheduleAppointment(request);
    }

    @PostMapping("/follow-up")
    public Appointment followUpAppointment(
        @RequestBody AppointmentRequest request
    ) {
        return appointments.scheduleFollowUp(request);
    }

    @PostMapping("/specialist")
    public Appointment specialist(
        @RequestBody AppointmentRequest request
    ) {
        return appointments.scheduleSpecialist(request);
    }

    @PutMapping("/{id}/reschedule")
    public void reschedule(
        @PathVariable long id,
        @RequestBody Map<String, LocalDateTime> body
    ) {
        if (body == null || body.get("date") == null) {
            throw new IllegalArgumentException("Debe indicar la nueva fecha y hora de la cita");
        }
        appointments.rescheduleAppointment(id, body.get("date"));
    }

    @PutMapping("/{id}/cancel")
    public void cancel(@PathVariable long id) {
        appointments.cancelAppointment(id);
    }

    @PostMapping("/pending-follow-ups")
    public FollowUp scheduleFollowUp(
        @RequestBody FollowUpRequest request
    ) {
        return followUps.scheduleFollowUp(request);
    }

    @GetMapping("/pending-follow-ups")
    public List<FollowUp> pending() {
        return followUps.getPendingFollowUps();
    }

    @GetMapping("/pending-follow-ups/details")
    public List<FollowUpView> pendingDetails() {
        return queries.listPendingFollowUpViews();
    }

    @PutMapping("/pending-follow-ups/{id}/complete")
    public void complete(@PathVariable long id) {
        followUps.completeFollowUp(id);
    }

    @PostMapping("/pending-follow-ups/{id}/notify")
    public Map<String, String> notifyFollowUp(@PathVariable long id) {
        return Map.of("message", followUps.notifyFollowUpNow(id));
    }
}
