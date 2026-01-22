package com.devcaotics.airBnTruta.controllers;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.devcaotics.airBnTruta.model.entities.Hospedagem;
import com.devcaotics.airBnTruta.model.entities.Hospedeiro;
import com.devcaotics.airBnTruta.model.entities.Interesse;
import com.devcaotics.airBnTruta.model.repositories.Facade;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/hospedeiro")
public class HospedeiroController {

    @Autowired
    private Facade facade;
    private String msg = null;
    @Autowired
    private HttpSession session;

    @GetMapping({"/",""})
    public String init(Model m) {

        if(session.getAttribute("hospedeiroLogado") != null){
            Hospedeiro logado = (Hospedeiro)this.session.getAttribute("hospedeiroLogado");
            try {
               List<Hospedagem> hospedagens = this.facade.filterHospedagemByHospedeiro(logado.getCodigo());
                
                Map<Integer, Integer> interessesPorHospedagem = new HashMap<>();
                for (Hospedagem h : hospedagens) {
                    int count = facade.countInteressesByHospedagem(h.getCodigo());
                    interessesPorHospedagem.put(h.getCodigo(), count);
                }
                
                m.addAttribute("hospedagens", hospedagens);
                m.addAttribute("interessesMap", interessesPorHospedagem);
            } catch (SQLException e) {
                e.printStackTrace();
                m.addAttribute("msg", "não foi possível carregar suas hospedagens! Contate o desenvolvedor!");
            }

            return "hospedeiro/index";
        }

        m.addAttribute("hospedeiro", new Hospedeiro());
        m.addAttribute("msg", this.msg);
        this.msg=null;
        return "hospedeiro/login";
    }

    @PostMapping("/save")
    public String newHospedeiro(Model m, Hospedeiro h) {
        //TODO: process POST request
        
        try {
            facade.create(h);
            this.msg="Parabéns! Seu cadastro foi realizado com sucesso! Agora faça o login, por favor, meu querido hospedeiro de minha vida!";

        } catch (SQLException e) {
            this.msg="Chorou! Não foi possível criar seu cadastro. Rapa daqui, fi da peste!";
        }

        return "redirect:/hospedeiro";
    }

    @PostMapping("/login")
    public String login(Model m,@RequestParam String vulgo,
        @RequestParam String senha
    ) {
        //TODO: process POST request
        
        try {
            Hospedeiro logado = facade.loginHospedeiro(vulgo, senha);
            if(logado == null){
                this.msg = "Erro ao Logar";
                return "redirect:/hospedeiro";
            }
            session.setAttribute("hospedeiroLogado", logado);
            return "redirect:/hospedeiro";
            
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            this.msg = "Erro ao logar!";
            return "redirect:/hospedeiro";
        }

        
    }
    
    
    @GetMapping("/logout")
    public String logout(Model m) {

        session.removeAttribute("hospedeiroLogado");;

        return "redirect:/hospedeiro";
    }
    
    @GetMapping("/interesses/{hospedagemId}")
public String verInteressesHospedagem(@PathVariable int hospedagemId, Model m) {
    
    if(session.getAttribute("hospedeiroLogado") == null){
        return "redirect:/hospedeiro";
    }
    
    Hospedeiro hospedeiroLogado = (Hospedeiro) session.getAttribute("hospedeiroLogado");
    
    try {
        // Verifica se a hospedagem pertence ao hospedeiro
        Hospedagem hospedagem = facade.readHospedagem(hospedagemId);
        if (hospedagem == null || hospedagem.getHospedeiro().getCodigo() != hospedeiroLogado.getCodigo()) {
            m.addAttribute("msg", "Hospedagem não encontrada ou não pertence a você!");
            return "redirect:/hospedeiro";
        }
        
        // Busca interesses na hospedagem
        List<Interesse> interesses = facade.findInteressesByHospedagem(hospedagemId);
        
        m.addAttribute("hospedagem", hospedagem);
        m.addAttribute("interesses", interesses);
        
    } catch (SQLException e) {
        e.printStackTrace();
        m.addAttribute("msg", "Erro ao carregar interesses!");
        return "redirect:/hospedeiro";
    }
    
    return "hospedeiro/interesses";
}

@PostMapping("/aceitar-interesse")
public String aceitarInteresse(@RequestParam int interesseId, Model m) {
    
    if(session.getAttribute("hospedeiroLogado") == null){
        return "redirect:/hospedeiro";
    }
    
    try {
        // Busca o interesse
        Interesse interesse = facade.readInteresse(interesseId);
        if (interesse == null) {
            m.addAttribute("msg", "Interesse não encontrado!");
            return "redirect:/hospedeiro";
        }
        
        Hospedagem hospedagem = interesse.getInteresse();
        Hospedeiro hospedeiroLogado = (Hospedeiro) session.getAttribute("hospedeiroLogado");
        
        // Verifica se a hospedagem pertence ao hospedeiro
        if (hospedagem.getHospedeiro().getCodigo() != hospedeiroLogado.getCodigo()) {
            m.addAttribute("msg", "Esta hospedagem não pertence a você!");
            return "redirect:/hospedeiro";
        }
        
        // Verifica se a hospedagem já está ocupada
        if (hospedagem.getFugitivo() != null) {
            m.addAttribute("msg", "Esta hospedagem já está ocupada!");
            return "redirect:/hospedeiro";
        }
        
        // Aceita o interesse (associa fugitivo à hospedagem)
        facade.aceitarInteresse(hospedagem.getCodigo(), interesse.getInteressado().getCodigo());
        
        m.addAttribute("msg", "Interesse aceito! A hospedagem agora está ocupada por " + 
                        interesse.getInteressado().getVulgo());
        
    } catch (SQLException e) {
        e.printStackTrace();
        m.addAttribute("msg", "Erro ao aceitar interesse!");
    }
    
    return "redirect:/hospedeiro";
}

}
