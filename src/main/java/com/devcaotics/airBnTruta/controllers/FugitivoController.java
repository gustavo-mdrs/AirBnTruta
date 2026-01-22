package com.devcaotics.airBnTruta.controllers;

import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.devcaotics.airBnTruta.model.entities.Fugitivo;
import com.devcaotics.airBnTruta.model.entities.Hospedagem;
import com.devcaotics.airBnTruta.model.entities.Interesse;
import com.devcaotics.airBnTruta.model.repositories.Facade;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/fugitivo")
public class FugitivoController {

    @Autowired
    private Facade facade;
    
    private String msg = null;
    
    @Autowired
    private HttpSession session;

    @GetMapping({"", "/"})
    public String init(Model m) {
        // Se já estiver logado, redireciona para a página principal do fugitivo
        if(session.getAttribute("fugitivoLogado") != null){
            return "redirect:/fugitivo/home";
        }
        
        // Se não estiver logado, mostra a página de login/cadastro
        m.addAttribute("fugitivo", new Fugitivo());
        m.addAttribute("msg", this.msg);
        this.msg = null;
        return "fugitivo/login"; // Vamos criar este template
    }

    @GetMapping("/home")
    public String home(Model m) {
        // Verifica se está logado
        if(session.getAttribute("fugitivoLogado") == null){
            this.msg = "Você precisa fazer login primeiro!";
            return "redirect:/fugitivo";
        }
        
        try {
            // Busca todas as hospedagens disponíveis (onde fugitivo_id é null)
            List<Hospedagem> hospedagens = facade.filterHospedagemByAvailable();
            m.addAttribute("hospedagens", hospedagens);
        } catch (SQLException e) {
            e.printStackTrace();
            m.addAttribute("msg", "Não foi possível carregar as hospedagens disponíveis!");
        }
        
        return "fugitivo/home"; // Vamos criar este template
    }

    @PostMapping("/save")
    public String saveFugitivo(Model m, Fugitivo f) {
        try {
            facade.create(f);
            this.msg = "Cadastro realizado com sucesso! Faça login para continuar.";
        } catch (SQLException e) {
            e.printStackTrace();
            this.msg = "Erro ao cadastrar! Tente novamente.";
        }
        
        return "redirect:/fugitivo";
    }

    @PostMapping("/login")
    public String login(Model m, @RequestParam String vulgo, @RequestParam String senha) {
        try {
            Fugitivo logado = facade.loginFugitivo(vulgo, senha);
            if(logado == null) {
                this.msg = "Vulgo ou senha incorretos!";
                return "redirect:/fugitivo";
            }
            session.setAttribute("fugitivoLogado", logado);
            return "redirect:/fugitivo/home";
        } catch (SQLException e) {
            e.printStackTrace();
            this.msg = "Erro ao fazer login!";
            return "redirect:/fugitivo";
        }
    }

    @GetMapping("/logout")
    public String logout() {
        session.removeAttribute("fugitivoLogado");
        this.msg = "Logout realizado com sucesso!";
        return "redirect:/fugitivo";
    }

   @GetMapping("/filtrar")
public String filtrarHospedagens(
    @RequestParam(required = false) String localidade,
    @RequestParam(required = false) Double precoMin,
    @RequestParam(required = false) Double precoMax,
    Model m) {
    
    // Verifica se está logado
    if(session.getAttribute("fugitivoLogado") == null){
        return "redirect:/fugitivo";
    }
    
    try {
        List<Hospedagem> hospedagens;
        
        if ((localidade == null || localidade.trim().isEmpty()) && precoMin == null && precoMax == null) {
            // Se não há filtros, mostra tudo
            hospedagens = facade.filterHospedagemByAvailable();
        } else {
            // Aplica filtros
            hospedagens = facade.filterHospedagemByLocalidadeEPreco(
                localidade != null ? localidade.trim() : null, 
                precoMin, 
                precoMax
            );
        }
        
        m.addAttribute("hospedagens", hospedagens);
        // Mantém os valores dos filtros para mostrar no formulário
        m.addAttribute("localidadeFiltro", localidade);
        m.addAttribute("precoMinFiltro", precoMin);
        m.addAttribute("precoMaxFiltro", precoMax);
        
    } catch (SQLException e) {
        e.printStackTrace();
        m.addAttribute("msg", "Erro ao filtrar hospedagens!");
    }
    
    return "fugitivo/home";
}
@GetMapping("/hospedagem/{id}")
public String verDetalhesHospedagem(@PathVariable int id, Model m) {
    // Verifica se está logado
    if(session.getAttribute("fugitivoLogado") == null){
        return "redirect:/fugitivo";
    }
    
    Fugitivo fugitivoLogado = (Fugitivo) session.getAttribute("fugitivoLogado");
    
    try {
        Hospedagem hospedagem = facade.readHospedagem(id);
        if (hospedagem == null) {
            m.addAttribute("msg", "Hospedagem não encontrada!");
            return "redirect:/fugitivo/home";
        }
        
        // Verifica se a hospedagem está disponível
        if (hospedagem.getFugitivo() != null) {
            // Se está ocupada, verifica se é pelo fugitivo logado
            if (hospedagem.getFugitivo().getCodigo() == fugitivoLogado.getCodigo()) {
                // É o fugitivo que ocupa a hospedagem - mostra detalhes com mensagem
                m.addAttribute("hospedagem", hospedagem);
                m.addAttribute("msg", "Você está ocupando esta hospedagem!");
                m.addAttribute("interesse", new Interesse());
                return "fugitivo/detalhes";
            } else {
                // Outro fugitivo ocupa - redireciona
                m.addAttribute("msg", "Esta hospedagem já está ocupada por outro fugitivo!");
                return "redirect:/fugitivo/home";
            }
        }
        
        // Hospedagem disponível - mostra normalmente
        m.addAttribute("hospedagem", hospedagem);
        m.addAttribute("interesse", new Interesse());
        
    } catch (SQLException e) {
        e.printStackTrace();
        m.addAttribute("msg", "Erro ao carregar detalhes da hospedagem!");
        return "redirect:/fugitivo/home";
    }
    
    return "fugitivo/detalhes";
}

@PostMapping("/interesse/{hospedagemId}")
public String demonstrarInteresse(@PathVariable int hospedagemId,
                                  @RequestParam String proposta,
                                  @RequestParam int tempoPermanencia,
                                  Model m) {
    
    // Verifica se está logado
    Fugitivo fugitivoLogado = (Fugitivo) session.getAttribute("fugitivoLogado");
    if (fugitivoLogado == null) {
        return "redirect:/fugitivo";
    }
    
    try {
        // Verifica se a hospedagem existe e está disponível
        Hospedagem hospedagem = facade.readHospedagem(hospedagemId);
        if (hospedagem == null || hospedagem.getFugitivo() != null) {
            m.addAttribute("msg", "Hospedagem não disponível!");
            return "redirect:/fugitivo/home";
        }
        
        // Cria o interesse
        Interesse interesse = new Interesse();
        interesse.setProposta(proposta);
        interesse.setTempoPermanencia(tempoPermanencia);
        interesse.setInteressado(fugitivoLogado);
        interesse.setInteresse(hospedagem);
        
        // Salva no banco
        facade.create(interesse);
        
        m.addAttribute("msg", "Interesse registrado com sucesso! O hospedeiro será notificado.");
        
    } catch (SQLException e) {
        e.printStackTrace();
        m.addAttribute("msg", "Erro ao registrar interesse!");
    }
    
    return "redirect:/fugitivo/hospedagem/" + hospedagemId;
}

@GetMapping("/interesses")
public String meusInteresses(Model m) {
    // Verifica se está logado
    Fugitivo fugitivoLogado = (Fugitivo) session.getAttribute("fugitivoLogado");
    if (fugitivoLogado == null) {
        return "redirect:/fugitivo";
    }
    
    try {
        List<Interesse> interesses = facade.findInteressesByFugitivo(fugitivoLogado.getCodigo());
        m.addAttribute("interesses", interesses);
        
    } catch (SQLException e) {
        e.printStackTrace();
        m.addAttribute("msg", "Erro ao carregar seus interesses!");
    }
    
    return "fugitivo/interesses";
}
}