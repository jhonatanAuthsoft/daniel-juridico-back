package com.laweact.service.imp;

import com.laweact.config.JwtUtil;
import com.laweact.config.exception.CustomError;
import com.laweact.dto.advogado.AdvogadoDetalheResponseDTO;
import com.laweact.dto.cliente.ClienteDetalheResponseDTO;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.dto.usuario.LoginUsuarioResponseDTO;
import com.laweact.dto.usuario.MeResponseDTO;
import com.laweact.dto.usuario.RefreshTokenInputDTO;
import com.laweact.dto.usuario.RefreshTokenResponseDTO;
import com.laweact.dto.usuario.UsuarioResponseDTO;
import com.laweact.mapper.UsuarioMapper;
import com.laweact.model.entity.TokenRevogadoEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusUsuarioEnum;
import com.laweact.repository.TokenRevogadoRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.UsuarioService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import io.jsonwebtoken.Claims;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class UsuarioServiceImp implements UsuarioService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final JwtUtil jwtUtil;
    private final UsuarioDetailsServiceImp usuarioDetailsServiceImp;
    private final TokenRevogadoRepository tokenRevogadoRepository;
    private final ClienteServiceImp clienteServiceImp;
    private final AdvogadoServiceImp advogadoServiceImp;

    @Override
    @Transactional
    public LoginUsuarioResponseDTO login(LoginUsuarioInputDTO loginUsuarioDTO) {
        String email = loginUsuarioDTO.email().toLowerCase();

        UsuarioEntity usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new CustomError("Usuário ou senha inválidos", HttpStatus.BAD_REQUEST));

        try {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    email,
                    loginUsuarioDTO.senha()
            );
            authenticationManager.authenticate(token);
        } catch (Exception error) {
            log.debug("Falha no login para {}", email, error);
            throw new CustomError("Usuário ou senha inválidos", HttpStatus.BAD_REQUEST);
        }

        UserDetails userDetails = usuarioDetailsServiceImp.loadUserByUsername(email);
        String jwt = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        ClienteDetalheResponseDTO cliente = null;
        AdvogadoDetalheResponseDTO advogado = null;
        if (usuario.getPerfil() == PerfilUsuarioEnum.CLIENTE) {
            cliente = clienteServiceImp.carregarDetalhe(usuario.getId());
        } else if (usuario.getPerfil() == PerfilUsuarioEnum.ADVOGADO) {
            advogado = advogadoServiceImp.carregarDetalhe(usuario.getId());
        }

        return usuarioMapper.toLoginResponse(usuario, cliente, advogado, jwt, refreshToken);
    }

    @Override
    @Transactional
    public RefreshTokenResponseDTO refresh(RefreshTokenInputDTO input) {
        Claims refreshClaims;
        try {
            refreshClaims = jwtUtil.extractAllClaimsAllowExpired(input.refreshToken());
        } catch (Exception e) {
            throw new CustomError("Refresh token inválido", HttpStatus.UNAUTHORIZED);
        }

        Date refreshExp = refreshClaims.getExpiration();
        if (refreshExp == null || refreshExp.before(new Date())) {
            throw new CustomError("Refresh token expirado. Faça login novamente.", HttpStatus.UNAUTHORIZED);
        }

        if (!JwtUtil.TYPE_REFRESH.equals(refreshClaims.get(JwtUtil.CLAIM_TOKEN_TYPE, String.class))) {
            throw new CustomError("Refresh token inválido", HttpStatus.UNAUTHORIZED);
        }

        Claims accessClaims;
        try {
            accessClaims = jwtUtil.extractAllClaimsAllowExpired(input.token());
        } catch (Exception e) {
            throw new CustomError("Token inválido", HttpStatus.UNAUTHORIZED);
        }

        String accessTyp = accessClaims.get(JwtUtil.CLAIM_TOKEN_TYPE, String.class);
        if (accessTyp != null && !JwtUtil.TYPE_ACCESS.equals(accessTyp)) {
            throw new CustomError("Token inválido", HttpStatus.UNAUTHORIZED);
        }

        String emailRefresh = refreshClaims.getSubject();
        String emailAccess = accessClaims.getSubject();
        if (emailRefresh == null || emailAccess == null || !emailRefresh.equalsIgnoreCase(emailAccess)) {
            throw new CustomError("Token e refreshToken não correspondem", HttpStatus.UNAUTHORIZED);
        }

        if (tokenRevogadoRepository.existsByToken(input.refreshToken())
                || tokenRevogadoRepository.existsByToken(input.token())) {
            throw new CustomError("Sessão inválida. Faça login novamente.", HttpStatus.UNAUTHORIZED);
        }

        String email = emailRefresh.toLowerCase();
        UsuarioEntity usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new CustomError("Usuário não encontrado", HttpStatus.UNAUTHORIZED));

        if (usuario.getStatus() != StatusUsuarioEnum.ATIVO) {
            throw new CustomError("Usuário inativo", HttpStatus.UNAUTHORIZED);
        }

        if (tokenInvalidadoPorResetSenha(usuario, refreshClaims.getIssuedAt())) {
            throw new CustomError("Sessão inválida. Faça login novamente.", HttpStatus.UNAUTHORIZED);
        }

        UserDetails userDetails = usuarioDetailsServiceImp.loadUserByUsername(email);
        return RefreshTokenResponseDTO.builder()
                .token(jwtUtil.generateAccessToken(userDetails))
                .refreshToken(jwtUtil.generateRefreshToken(userDetails))
                .build();
    }

    private boolean tokenInvalidadoPorResetSenha(UsuarioEntity usuario, Date issuedAt) {
        if (usuario.getTokensInvalidosAntes() == null || issuedAt == null) {
            return false;
        }
        LocalDateTime iat = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(issuedAt.getTime()),
                ZoneId.systemDefault()
        );
        return !iat.isAfter(usuario.getTokensInvalidosAntes());
    }

    @Override
    public void logout(String token) {
        TokenRevogadoEntity tokenRevogado = TokenRevogadoEntity.builder()
                .token(token)
                .revogadoEm(LocalDateTime.now())
                .build();
        tokenRevogadoRepository.save(tokenRevogado);
    }

    @Override
    @Transactional
    public MeResponseDTO obterUsuarioAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            throw new CustomError("Usuário não autenticado", HttpStatus.UNAUTHORIZED);
        }

        UsuarioEntity usuario = usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomError("Usuário não encontrado", HttpStatus.NOT_FOUND));

        ClienteDetalheResponseDTO cliente = null;
        AdvogadoDetalheResponseDTO advogado = null;
        if (usuario.getPerfil() == PerfilUsuarioEnum.CLIENTE) {
            cliente = clienteServiceImp.carregarDetalhe(usuario.getId());
        } else if (usuario.getPerfil() == PerfilUsuarioEnum.ADVOGADO) {
            advogado = advogadoServiceImp.carregarDetalhe(usuario.getId());
        }

        return usuarioMapper.toMeResponse(usuario, cliente, advogado);
    }

    @Override
    @Transactional
    public void excluir(UUID id) {
        UsuarioEntity usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new CustomError("Usuário não encontrado", HttpStatus.NOT_FOUND));
        usuarioRepository.delete(usuario);
    }

    @Override
    public UsuarioResponseDTO obterUsuarioPorId(UUID id) {
        UsuarioEntity usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new CustomError("Usuário não encontrado", HttpStatus.NOT_FOUND));
        return usuarioMapper.toResponseDTO(usuario);
    }

    @Override
    public List<UsuarioResponseDTO> obterTodosUsuarios(
        int limit, int offset, String searchText, PerfilUsuarioEnum perfil, StatusUsuarioEnum status
    ) {
        String perfilStr = perfil != null ? perfil.name() : null;
        String statusStr = status != null ? status.name() : null;

        List<UsuarioEntity> usuarios = usuarioRepository.findPaginated(
            searchText, perfilStr, statusStr, limit, offset
        );

        return usuarios.stream()
            .map(usuarioMapper::toResponseDTO)
            .toList();
    }

    @Override
    public long contarTodosUsuarios(String searchText, PerfilUsuarioEnum perfil, StatusUsuarioEnum status) {
        String perfilStr = perfil != null ? perfil.name() : null;
        String statusStr = status != null ? status.name() : null;
        return usuarioRepository.countWithFilter(searchText, perfilStr, statusStr);
    }
}
