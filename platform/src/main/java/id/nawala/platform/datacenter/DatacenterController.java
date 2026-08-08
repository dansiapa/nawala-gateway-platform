package id.nawala.platform.datacenter;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/datacenters")
@RequiredArgsConstructor
public class DatacenterController {

    private final DatacenterService datacenterService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("datacenters", datacenterService.getAllDatacenters());
        model.addAttribute("newDc", new DatacenterConfig());
        return "admin/datacenters";
    }

    @PostMapping
    public String create(@ModelAttribute DatacenterConfig dc, RedirectAttributes ra) {
        datacenterService.createDatacenter(dc);
        ra.addFlashAttribute("success", "Datacenter '" + dc.getName() + "' created!");
        return "redirect:/admin/datacenters";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        datacenterService.getAllDatacenters().stream()
            .filter(dc -> dc.getId().equals(id))
            .findFirst()
            .ifPresent(dc -> {
                dc.setEnabled(!dc.isEnabled());
                datacenterService.createDatacenter(dc);
                ra.addFlashAttribute("success", "Datacenter toggled!");
            });
        return "redirect:/admin/datacenters";
    }

    @PostMapping("/{id}/primary")
    public String setPrimary(@PathVariable Long id, RedirectAttributes ra) {
        datacenterService.getAllDatacenters().stream()
            .filter(dc -> dc.getId().equals(id))
            .findFirst()
            .ifPresent(dc -> {
                dc.setPrimary(true);
                datacenterService.createDatacenter(dc);
                ra.addFlashAttribute("success", "Primary datacenter updated!");
            });
        return "redirect:/admin/datacenters";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        datacenterService.deleteDatacenter(id);
        ra.addFlashAttribute("success", "Datacenter deleted!");
        return "redirect:/admin/datacenters";
    }
}
