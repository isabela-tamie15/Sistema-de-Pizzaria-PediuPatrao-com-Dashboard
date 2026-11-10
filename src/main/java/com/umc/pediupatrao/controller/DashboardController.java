package com.umc.pediupatrao.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// Tela do Dashboard Administrativo
// Esta classe só abre a página: os números são buscados depois pelo JavaScript,
// que chama a API /api/dashboard/** (DashboardApiController) - RF06
@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Mesmo padrão das outras telas: o layout.html carrega o fragmento indicado em "content"
        model.addAttribute("content", "dashboard/painel :: content");
        return "layout";
    }
}
