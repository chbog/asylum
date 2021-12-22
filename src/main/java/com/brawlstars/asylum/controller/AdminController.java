package com.brawlstars.asylum.controller;

import com.brawlstars.asylum.dto.*;
import com.brawlstars.asylum.model.*;
import com.brawlstars.asylum.service.*;
import com.brawlstars.asylum.util.ObjectMapperUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@Log4j2
@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    AppointmentService appointmentService;
    @Autowired
    DoctorService doctorService;
    @Autowired
    UserService userService;
    @Autowired
    RequestAppointmentService requestAppointmentService;
    @Autowired
    TreatmentService treatmentService;

    @GetMapping("/appointmentRequest")
    public String requestsView(Model model){
        List<RequestAppointment> requestAppointmentList = requestAppointmentService.getAllRequestAppointments();
        List<AppointmentRequestDto> appointmentRequestDtos = ObjectMapperUtils.mapAll(requestAppointmentList, AppointmentRequestDto.class);
        model.addAttribute("appointments", appointmentRequestDtos);
        AppointmentCreationDto appointmentCreationDto = new AppointmentCreationDto();
        model.addAttribute("appointment", appointmentCreationDto);
        return "appointmentRequestsAdmin";
    }

    @GetMapping("/appointment")
    public String appointmentsView(Model model) {
        List<Appointment> appointmentModelList = appointmentService.getAllAppointments();
        List<AppointmentDto> appointmentDtoList = ObjectMapperUtils.mapAll(appointmentModelList, AppointmentDto.class);
        model.addAttribute("appointments", appointmentDtoList);
        return "appointment";
    }

    @PostMapping("/appointment/delete/{appointmentId}")
    public String deleteAppointment(@PathVariable int appointmentId) {
        appointmentService.deleteAppointmentById(appointmentId);
        return "redirect:/admin/appointment";
    }

    @PostMapping("/requestAppointment/delete/{requestAppointmentId}")
    public String deleteRequestAppointment(@PathVariable int requestAppointmentId) {
        requestAppointmentService.deleteRequestAppointmentById(requestAppointmentId);
        return "redirect:/admin/appointmentRequest";
    }

    @GetMapping("/requestAppointment/accept/{requestAppointmentId}")
    public String acceptRequestAppointment(@PathVariable int requestAppointmentId, Model model, HttpServletRequest request) {

        HttpSession session = request.getSession();

        AppointmentCreationDto appointmentCreationDto = new AppointmentCreationDto();

        RequestAppointment requestAppointment = requestAppointmentService.getById(requestAppointmentId);


        List<Doctor> doctorList = doctorService.getAllDoctors();
        List<DoctorDto> doctorDtos = ObjectMapperUtils.mapAll(doctorList, DoctorDto.class);
        appointmentCreationDto.setDoctors(doctorDtos);

        appointmentCreationDto.setDoctorPosition(requestAppointment.getDoctorPosition());
        appointmentCreationDto.setPatientEmail(requestAppointment.getUser().getEmail());
        appointmentCreationDto.setId(requestAppointment.getId());


        session.setAttribute("appointmentAttribute", appointmentCreationDto);

        model.addAttribute("appointmentCreation", appointmentCreationDto);
        return "appointmentCreation";
    }

    @PostMapping("/appointment/create")
    public String confirmAppointmentCreation(@Valid @ModelAttribute("appointmentCreation") AppointmentCreationDto appointment,
                                             BindingResult bindingResult, HttpServletRequest request) {
        AppointmentCreationDto appointmentCreationDto = (AppointmentCreationDto) request.getSession().getAttribute("appointmentAttribute");

        Optional<Doctor> doctorExist = Optional
                .ofNullable(doctorService
                        .getDoctorByUserEmail(appointment.getDoctorEmail()));
        if (doctorExist.isEmpty()) {
            bindingResult.rejectValue("doctorEmail", "error.appointment",
                    "*This doctor doesn't work in our hospital");
        }
        Optional<User> userExists =
                userService.findUserByEmail(appointment.getPatientEmail());
        if (userExists.isEmpty()) {
            bindingResult.rejectValue("patientEmail", "error.appointment",
                    "*This patient hasn't been registered in our hospital");
        }
        Optional<Appointment> appointmentExist = appointmentService
                .getAppointmentByDate(
                        appointment.getDoctorEmail(),
                        appointment.getDateOfAppointment());
        if (appointmentExist.isPresent()) {
            bindingResult
                    .rejectValue("dateOfAppointment", "error.appointment",
                            "*This date for this doctor isn't available");
        }
        if (bindingResult.hasErrors()) {
            log.error("Errors: " + bindingResult.getAllErrors());
            return "appointmentCreation";
        }

        Appointment appointmentModel = new Appointment();
        appointmentModel.setDoctor(doctorExist.get());
        appointmentModel.setPatient(userExists.get());
        appointmentModel.setDate(appointment.getDateOfAppointment());
        appointmentService.save(appointmentModel);
        requestAppointmentService.deleteRequestAppointmentById(appointmentCreationDto.getId());
        return "redirect:/admin/appointment";
    }

    @GetMapping("/createEpicrisis")
    public String createEpicrisis(Model model){
        EpicrisisCreationDto epicrisisCreationDto = new EpicrisisCreationDto();
        model.addAttribute(epicrisisCreationDto);
        return "epicrisisCreation";
    }

    @PostMapping("/createEpicrisis")
    public String confirmCreation(@Valid @ModelAttribute("epicrisis") EpicrisisCreationDto epicrisisCreationDto,
                                  BindingResult bindingResult, Model model) {
        Optional<User> userExists =
                userService.findUserByEmail(epicrisisCreationDto.getPatientEmail());
        if (userExists.isEmpty()) {
            bindingResult.rejectValue("patientEmail", "error.appointment",
                    "*This patient hasn't been registered in our hospital");
        }
        Treatment treatment = treatmentService.getLastTreatmentForPatient(userExists.get().getEmail());
        model.addAttribute(treatment);
        return "epicrisisResult";
    }

}