package com.brawlstars.asylum.controller;

import com.brawlstars.asylum.dto.AppointmentDto;
import com.brawlstars.asylum.dto.DiagnoseDto;
import com.brawlstars.asylum.dto.TreatmentDto;
import com.brawlstars.asylum.model.Appointment;
import com.brawlstars.asylum.model.Diagnose;
import com.brawlstars.asylum.model.Treatment;
import com.brawlstars.asylum.service.AppointmentService;
import com.brawlstars.asylum.service.TreatmentService;
import com.brawlstars.asylum.util.GeneratePDFInfo;
import com.brawlstars.asylum.util.ObjectMapperUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.util.List;

@Log4j2
@Controller
@RequestMapping("/doctor")
public class DoctorController {
    @Autowired
    AppointmentService appointmentService;
    @Autowired
    TreatmentService treatmentService;


    @GetMapping("/appointment")
    public String appointmentsView(Principal principal, Model model) {
        List<Appointment> appointmentModelList = appointmentService.getAllAppointmentsByDoctorEmail(principal.getName());
        List<AppointmentDto> appointmentDtoList = ObjectMapperUtils.mapAll(appointmentModelList, AppointmentDto.class);
        model.addAttribute("appointments", appointmentDtoList);
        return "appointment";
    }

    @PostMapping( "/appointment/delete/{appointmentId}")
    public String deleteAppointment(@PathVariable int appointmentId){
        appointmentService.deleteAppointmentById(appointmentId);
        return "redirect:/doctor/appointment";
    }

    @PostMapping("/treatment/create/{appointmentId}")
    public String createTreatment(@PathVariable int appointmentId){
        Treatment newTreatment = treatmentService.createTreatment(appointmentId);
        return "redirect:/doctor/treatment/edit/" + newTreatment.getId();
    }

    @GetMapping("treatment/edit/{treatmentId}")
    public String editTreatmentView(@PathVariable int treatmentId, Model model){
        Treatment treatmentModel = treatmentService.getTreatmentById(treatmentId);
        TreatmentDto newTreatmentDto = ObjectMapperUtils.map(treatmentModel, TreatmentDto.class);
        if (model.containsAttribute("treatmentEditErrors")) {
            model.addAttribute("org.springframework.validation.BindingResult.treatment",
                    model.getAttribute("treatmentEditErrors"));
            TreatmentDto treatmentDto = (TreatmentDto)model.getAttribute("treatment");
            treatmentDto.setId(newTreatmentDto.getId());
            treatmentDto.setDoctor(newTreatmentDto.getDoctor());
            treatmentDto.setPatient(newTreatmentDto.getPatient());
            treatmentDto.setDiagnoses(newTreatmentDto.getDiagnoses());
            treatmentDto.setStartOfTreatment(newTreatmentDto.getStartOfTreatment());
        }
        else{
            model.addAttribute("treatment", newTreatmentDto);
        }
        return "treatmentEditing";
    }

    @PostMapping("treatment/edit/{treatmentId}")
    public String editTreatment(@PathVariable int treatmentId, @Valid @ModelAttribute("treatment") TreatmentDto treatmentDto,
                                BindingResult bindingResult, RedirectAttributes redirectAttributes){
        Treatment treatmentModel = treatmentService.getTreatmentById(treatmentId);
        if(treatmentDto.getEndOfTreatment() != null && treatmentModel.getStartOfTreatment().compareTo(treatmentDto.getEndOfTreatment()) > 0){
            bindingResult.rejectValue("endOfTreatment", "",
                    "*End of treatment date must not be before start date");
        }
        if (bindingResult.hasErrors()) {
            log.error("Errors: " + bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("treatmentEditErrors", bindingResult);
            redirectAttributes.addFlashAttribute("treatment", treatmentDto);
            return "redirect:/doctor/treatment/edit/" + treatmentId;
        }
        treatmentModel.setEndOfTreatment(treatmentDto.getEndOfTreatment());
        treatmentModel.setMethodsOfTreatment(treatmentDto.getMethodsOfTreatment());
        treatmentModel.setChamber(treatmentDto.getChamber());
        treatmentService.saveTreatment(treatmentModel);
        return "redirect:/doctor/treatment";
    }

    @GetMapping("/treatment")
    public String adminTreatmentView(Principal principal, Model model) {
        List<Treatment> treatmentList = treatmentService.getAllTreatmentsForDoctor(principal.getName());
        List<TreatmentDto> treatmentDtoList = ObjectMapperUtils.mapAll(treatmentList, TreatmentDto.class);
        model.addAttribute("treatments", treatmentDtoList);
        return "treatment";
    }

    @PostMapping("/treatment/delete/{treatmentId}")
    public String deleteTreatment(@PathVariable int treatmentId){
        treatmentService.deleteTreatmentById(treatmentId);
        return "redirect:/doctor/treatment";
    }

    @GetMapping("/treatment/diagnose/{treatmentId}")
    public String doctorDiagnoseView(@PathVariable int treatmentId, Model model){
        Treatment treatmentModel = treatmentService.getTreatmentById(treatmentId);
        List<DiagnoseDto> diagnoseDtoList = ObjectMapperUtils.mapAll(treatmentModel.getDiagnoses(), DiagnoseDto.class);
        DiagnoseDto addDiagnose = new DiagnoseDto();
        model.addAttribute("diagnoses", diagnoseDtoList);
        if (model.containsAttribute("addDiagnoseErrors")) {
            model.addAttribute("org.springframework.validation.BindingResult.addDiagnose",
                    model.getAttribute("addDiagnoseErrors"));
        }
        else{
            model.addAttribute("addDiagnose", addDiagnose);
        }
        return "diagnose";
    }

    @PostMapping("/treatment/diagnose/delete/{treatmentId}/{diagnoseId}")
    public String deleteDiagnose(@PathVariable int treatmentId, @PathVariable int diagnoseId) {
        treatmentService.deleteDiagnoseForTreatment(treatmentId, diagnoseId);
        return "redirect:/doctor/treatment/diagnose/" + treatmentId;
    }

    @PostMapping("/treatment/diagnose/add/{treatmentId}")
    public String addDiagnose(@PathVariable int treatmentId, @Valid @ModelAttribute("addDiagnose") DiagnoseDto diagnoseDto, BindingResult bindingResult,  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            log.error("Errors: " + bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("addDiagnoseErrors", bindingResult);
            redirectAttributes.addFlashAttribute("addDiagnose", diagnoseDto);
            return "redirect:/doctor/treatment/diagnose/" + treatmentId;
        }
        Diagnose diagnose = ObjectMapperUtils.map(diagnoseDto, Diagnose.class);
        treatmentService.addDiagnose(diagnose, treatmentId);
        return "redirect:/doctor/treatment/diagnose/" + treatmentId;
    }

    @RequestMapping(value = "treatment/createEpicriz/{treatmentId}", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<InputStreamResource> treatmentReport(@PathVariable int treatmentId){
        Treatment treatment = treatmentService.getTreatmentById(treatmentId);

        HttpHeaders headers  = new HttpHeaders();

        ByteArrayInputStream bis = GeneratePDFInfo.citiesReport(treatment);
        headers.add("Content-Disposition", "inline; filename = epicrisis.pdf");


        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }
}
