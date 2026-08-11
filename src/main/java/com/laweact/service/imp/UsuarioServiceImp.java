package com.laweact.service.imp;

import com.laweact.config.JwtUtil;
import com.laweact.config.exception.CustomError;
import com.laweact.dto.advogado.AdvogadoDetalheResponseDTO;
import com.laweact.dto.cliente.ClienteDetalheResponseDTO;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.dto.usuario.LoginUsuarioResponseDTO;
import com.laweact.dto.usuario.MeResponseDTO;
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

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
                .orElseThrow(() -> new CustomError("E-mail ou senha inválidos", HttpStatus.BAD_REQUEST));

        try {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    email,
                    loginUsuarioDTO.senha()
            );
            authenticationManager.authenticate(token);
        } catch (Exception error) {
            log.debug("Falha no login para {}", email, error);
            throw new CustomError("E-mail ou senha inválidos", HttpStatus.BAD_REQUEST);
        }

        UserDetails userDetails = usuarioDetailsServiceImp.loadUserByUsername(email);
        String jwt = jwtUtil.generateToken(userDetails);

        ClienteDetalheResponseDTO cliente = null;
        AdvogadoDetalheResponseDTO advogado = null;
        if (usuario.getPerfil() == PerfilUsuarioEnum.CLIENTE) {
            cliente = clienteServiceImp.carregarDetalhe(usuario.getId());
        } else if (usuario.getPerfil() == PerfilUsuarioEnum.ADVOGADO) {
            advogado = advogadoServiceImp.carregarDetalhe(usuario.getId());
        }

        return usuarioMapper.toLoginResponse(usuario, cliente, advogado, jwt);
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
