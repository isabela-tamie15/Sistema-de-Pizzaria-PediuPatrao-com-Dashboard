package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.exception.RegraDeNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    // PARTE 2: tratamento central das regras de negócio
    // Na Parte 1, quando o PedidoService recusava algo (ex: desconto acima de 20%),
    // o usuário via um erro 500 genérico. Agora ele recebe a mensagem da regra com status 400
    // - Rotas /api/** recebem JSON: {"erro": "mensagem"}
    // - Telas recebem a página error.html com a mensagem
    @ExceptionHandler(RegraDeNegocioException.class)
    public ModelAndView tratarRegraDeNegocio(RegraDeNegocioException e, HttpServletRequest request) {
        return respostaDeErro(e.getMessage(), request);
    }

    // DASHBOARD: parâmetro em formato errado na URL
    // Exemplo: ?dataInicio=09/11/2026 (o certo é 2026-11-09) ou ?limite=abc (número inválido)
    // Sem este tratamento, o Spring devolveria um erro 400 genérico, sem explicar o que estava errado
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView tratarParametroInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String mensagem = "Valor inválido para o parâmetro '" + e.getName() + "'.";
        if (e.getName().startsWith("data")) {
            mensagem += " Use o formato AAAA-MM-DD (ex: 2026-11-09).";
        }
        return respostaDeErro(mensagem, request);
    }

    // Monta a resposta de erro 400: JSON para a API, página error.html para as telas
    private ModelAndView respostaDeErro(String mensagem, HttpServletRequest request) {
        ModelAndView resposta;

        if (request.getRequestURI().startsWith("/api/")) {
            resposta = new ModelAndView(new MappingJackson2JsonView());
            resposta.addObject("erro", mensagem);
        } else {
            resposta = new ModelAndView("error");
            resposta.addObject("status", 400);
            resposta.addObject("message", mensagem);
        }

        resposta.setStatus(HttpStatus.BAD_REQUEST);
        return resposta;
    }
}
