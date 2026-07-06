package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.enums.School;
import edu.ucsb.cs.scaffold.model.SystemInfo;
import edu.ucsb.cs.scaffold.services.SystemInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "System Information")
@RequestMapping("/api/systemInfo")
@RestController
public class SystemInfoController {

  @Autowired private SystemInfoService systemInfoService;

  @Operation(summary = "Get global information about the application")
  @GetMapping("")
  public SystemInfo getSystemInfo() {
    return systemInfoService.getSystemInfo();
  }

  @Operation(summary = "Get available schools")
  @GetMapping("/schools")
  public List<School> getSchools() {
    return List.of(School.values());
  }
}
