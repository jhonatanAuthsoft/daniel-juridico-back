package com.laweact.service.imp;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.laweact.config.exception.CustomError;
import com.laweact.dto.usuario.RedefinirSenhaInputDTO;
import com.laweact.dto.usuario.RedefinirSenhaResponseDTO;
import com.laweact.dto.usuario.SolicitarRecuperacaoSenhaInputDTO;
import com.laweact.dto.usuario.SolicitarRecuperacaoSenhaResponseDTO;
import com.laweact.dto.usuario.ValidarCodigoRecuperacaoInputDTO;
import com.laweact.dto.usuario.ValidarCodigoRecuperacaoResponseDTO;
import com.laweact.email.EmailTemplates;
import com.laweact.model.entity.TokenRecuperacaoSenhaEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.StatusUsuarioEnum;
import com.laweact.repository.TokenRecuperacaoSenhaRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.AuditoriaService;
import com.laweact.service.EmailService;
import com.laweact.service.RecuperacaoSenhaService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class RecuperacaoSenhaServiceImp implements RecuperacaoSenhaService {

    public static final String EVENTO_RECUPERACAO_SENHA = "recuperacao_senha";
    public static final String MENSAGEM_GENERICA =
            "Se existir uma conta com este e-mail, enviaremos um código de redefinição.";

    private static final int EXPIRACAO_MINUTOS = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacaoSenhaRepository tokenRecuperacaoSenhaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditoriaService auditoriaService;

    @Override
    @Transactional
    public SolicitarRecuperacaoSenhaResponseDTO solicitarCodigo(SolicitarRecuperacaoSenhaInputDTO input) {
        String email = input.email().toLowerCase().trim();
        Map<String, Object> inputAudit = Map.of("email", mascararEmail(email));

        Optional<UsuarioEntity> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getStatus() != StatusUsuarioEnum.ATIVO) {
            SolicitarRecuperacaoSenhaResponseDTO response = SolicitarRecuperacaoSenhaResponseDTO.builder()
                    .mensagem(MENSAGEM_GENERICA)
                    .aguardarSegundos(null)
                    .build();
            auditoriaService.registrar(
                    EVENTO_RECUPERACAO_SENHA,
                    null,
                    inputAudit,
                    response,
                    Map.of("acao", "SOLICITADO", "resultado", "EMAIL_INEXISTENTE_OU_INATIVO")
            );
            return response;
        }

        UsuarioEntity usuario = usuarioOpt.get();
        long enviosAnteriores = tokenRecuperacaoSenhaRepository.countByUsuario_Id(usuario.getId());
        Optional<TokenRecuperacaoSenhaEntity> ultimo =
                tokenRecuperacaoSenhaRepository.findFirstByUsuario_IdOrderByCreatedAtDesc(usuario.getId());

        int cooldownSegundos = calcularCooldownSegundos(enviosAnteriores);
        if (ultimo.isPresent() && cooldownSegundos > 0) {
            long decorrido = ChronoUnit.SECONDS.between(ultimo.get().getCreatedAt(), LocalDateTime.now());
            long restante = cooldownSegundos - decorrido;
            if (restante > 0) {
                SolicitarRecuperacaoSenhaResponseDTO response = SolicitarRecuperacaoSenhaResponseDTO.builder()
                        .mensagem(MENSAGEM_GENERICA)
                        .aguardarSegundos((int) restante)
                        .build();
                auditoriaService.registrar(
                        EVENTO_RECUPERACAO_SENHA,
                        usuario.getId(),
                        inputAudit,
                        response,
                        Map.of("acao", "SOLICITADO", "resultado", "COOLDOWN", "aguardarSegundos", restante)
                );
                return response;
            }
        }

        tokenRecuperacaoSenhaRepository.invalidarAtivosPorUsuario(usuario.getId());

        String codigo = gerarCodigoQuatroDigitos();
        TokenRecuperacaoSenhaEntity token = TokenRecuperacaoSenhaEntity.builder()
                .usuario(usuario)
                .codigoHash(passwordEncoder.encode(codigo))
                .expiraEm(LocalDateTime.now().plusMinutes(EXPIRACAO_MINUTOS))
                .build();
        tokenRecuperacaoSenhaRepository.save(token);

        boolean emailEnviado = true;
        String erroEmail = null;
        try {
            emailService.enviarHtml(
                    email,
                    "Laweact — recuperação de senha",
                    EmailTemplates.recuperacaoSenhaHtml(codigo),
                    EmailTemplates.recuperacaoSenhaTexto(codigo)
            );
        } catch (Exception ex) {
            emailEnviado = false;
            erroEmail = ex.getMessage();
            log.warn("Falha ao enviar código de recuperação para {}: {}", email, ex.getMessage());
        }

        int proximoCooldown = calcularCooldownSegundos(enviosAnteriores + 1);
        SolicitarRecuperacaoSenhaResponseDTO response = SolicitarRecuperacaoSenhaResponseDTO.builder()
                .mensagem(MENSAGEM_GENERICA)
                .aguardarSegundos(proximoCooldown)
                .build();

        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("acao", "SOLICITADO");
        detalhes.put("resultado", emailEnviado ? "CODIGO_ENVIADO" : "FALHA_ENVIO_EMAIL");
        detalhes.put("expiraEmMinutos", EXPIRACAO_MINUTOS);
        if (erroEmail != null) {
            detalhes.put("erroEmail", erroEmail);
        }

        auditoriaService.registrar(EVENTO_RECUPERACAO_SENHA, usuario.getId(), inputAudit, response, detalhes);
        return response;
    }

    @Override
    @Transactional
    public ValidarCodigoRecuperacaoResponseDTO validarCodigo(ValidarCodigoRecuperacaoInputDTO input) {
        String email = input.email().toLowerCase().trim();
        Map<String, Object> inputAudit = Map.of(
                "email", mascararEmail(email),
                "codigoInformado", true
        );

        Optional<UsuarioEntity> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isEmpty()) {
            ValidarCodigoRecuperacaoResponseDTO response = ValidarCodigoRecuperacaoResponseDTO.builder()
                    .valido(false)
                    .mensagem("Código inválido ou expirado. Solicite um novo código.")
                    .build();
            auditoriaService.registrar(
                    EVENTO_RECUPERACAO_SENHA,
                    null,
                    inputAudit,
                    response,
                    Map.of("acao", "CODIGO_VALIDADO", "resultado", "USUARIO_NAO_ENCONTRADO")
            );
            return response;
        }

        UsuarioEntity usuario = usuarioOpt.get();
        Optional<TokenRecuperacaoSenhaEntity> tokenOpt = encontrarTokenAtivoCompativel(usuario, input.codigo());

        if (tokenOpt.isEmpty()) {
            ValidarCodigoRecuperacaoResponseDTO response = ValidarCodigoRecuperacaoResponseDTO.builder()
                    .valido(false)
                    .mensagem("Código inválido ou expirado. Solicite um novo código.")
                    .build();
            auditoriaService.registrar(
                    EVENTO_RECUPERACAO_SENHA,
                    usuario.getId(),
                    inputAudit,
                    response,
                    Map.of("acao", "CODIGO_VALIDADO", "resultado", "INVALIDO_OU_EXPIRADO")
            );
            return response;
        }

        ValidarCodigoRecuperacaoResponseDTO response = ValidarCodigoRecuperacaoResponseDTO.builder()
                .valido(true)
                .mensagem("Código válido.")
                .build();
        auditoriaService.registrar(
                EVENTO_RECUPERACAO_SENHA,
                usuario.getId(),
                inputAudit,
                response,
                Map.of("acao", "CODIGO_VALIDADO", "resultado", "OK")
        );
        return response;
    }

    @Override
    @Transactional
    public RedefinirSenhaResponseDTO redefinirSenha(RedefinirSenhaInputDTO input) {
        String email = input.email().toLowerCase().trim();
        Map<String, Object> inputAudit = Map.of(
                "email", mascararEmail(email),
                "codigoInformado", true
        );

        if (!input.novaSenha().equals(input.confirmarSenha())) {
            throw new CustomError("A confirmação de senha não confere", HttpStatus.BAD_REQUEST);
        }

        UsuarioEntity usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new CustomError(
                        "Código inválido ou expirado. Solicite um novo código.",
                        HttpStatus.BAD_REQUEST
                ));

        TokenRecuperacaoSenhaEntity token = encontrarTokenAtivoCompativel(usuario, input.codigo())
                .orElseThrow(() -> new CustomError(
                        "Código inválido ou expirado. Solicite um novo código.",
                        HttpStatus.BAD_REQUEST
                ));

        usuario.setSenha(passwordEncoder.encode(input.novaSenha()));
        usuario.setTokensInvalidosAntes(LocalDateTime.now());
        usuarioRepository.save(usuario);

        token.setUsadoEm(LocalDateTime.now());
        tokenRecuperacaoSenhaRepository.save(token);
        tokenRecuperacaoSenhaRepository.invalidarAtivosPorUsuario(usuario.getId());

        RedefinirSenhaResponseDTO response = RedefinirSenhaResponseDTO.builder()
                .mensagem("Senha alterada com sucesso")
                .build();

        auditoriaService.registrar(
                EVENTO_RECUPERACAO_SENHA,
                usuario.getId(),
                inputAudit,
                response,
                Map.of("acao", "SENHA_REDEFINIDA", "resultado", "OK", "sessoesInvalidadas", true)
        );

        return response;
    }

    private Optional<TokenRecuperacaoSenhaEntity> encontrarTokenAtivoCompativel(
            UsuarioEntity usuario,
            String codigo
    ) {
        LocalDateTime agora = LocalDateTime.now();
        return tokenRecuperacaoSenhaRepository
                .findByUsuario_IdAndUsadoEmIsNullAndInvalidadoEmIsNull(usuario.getId())
                .stream()
                .filter(token -> token.isAtivo(agora))
                .filter(token -> passwordEncoder.matches(codigo, token.getCodigoHash()))
                .findFirst();
    }

    /**
     * Cooldown A: 5s nos dois primeiros reenvios; depois +1 min por nova requisição.
     * enviosAnteriores = quantos códigos já foram gerados antes deste.
     */
    static int calcularCooldownSegundos(long enviosAnteriores) {
        if (enviosAnteriores <= 0) {
            return 0;
        }
        if (enviosAnteriores <= 2) {
            return 5;
        }
        return (int) (60L * (enviosAnteriores - 2));
    }

    private String gerarCodigoQuatroDigitos() {
        int valor = RANDOM.nextInt(10_000);
        return String.format("%04d", valor);
    }

    private String mascararEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(at, 0));
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
