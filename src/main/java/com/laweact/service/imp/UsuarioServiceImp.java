package com.laweact.service.imp;

import com.laweact.config.JwtUtil;
import com.laweact.config.exception.CustomError;
import com.laweact.dto.usuario.CadastrarUsuarioInputDTO;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.dto.usuario.LoginUsuarioResponseDTO;
import com.laweact.dto.usuario.UsuarioResponseDTO;
import com.laweact.mapper.UsuarioMapper;
import com.laweact.model.entity.TokenRevogadoEntity;
import com.laweact.model.entity.UsuarioEntity;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Log4j2
@RequiredArgsConstructor
public class UsuarioServiceImp implements UsuarioService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioDetailsServiceImp usuarioDetailsServiceImp;
    private final TokenRevogadoRepository tokenRevogadoRepository;

    @Override
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

        return usuarioMapper.loginUserResponseToDTO(usuario, jwt);
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
    public UsuarioResponseDTO cadastrar(CadastrarUsuarioInputDTO input) {
        String email = input.email().toLowerCase();
        Optional<UsuarioEntity> existingUser = usuarioRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            throw new CustomError("E-mail já cadastrado", HttpStatus.BAD_REQUEST);
        }

        UsuarioEntity usuario = UsuarioEntity.builder()
                .nomeCompleto(input.nomeCompleto())
                .email(email)
                .senha(passwordEncoder.encode(input.senha()))
                .perfil(input.perfil())
                .status(input.status() != null ? input.status() : StatusUsuarioEnum.ATIVO)
                .telefone(input.telefone())
                .build();

        UsuarioEntity saved = usuarioRepository.save(usuario);
        return usuarioMapper.toResponseDTO(saved);
    }

    @Override
    public UsuarioResponseDTO obterUsuarioAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            throw new CustomError("Usuário não autenticado", HttpStatus.UNAUTHORIZED);
        }

        UsuarioEntity usuario = usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomError("Usuário não encontrado", HttpStatus.NOT_FOUND));

        return usuarioMapper.toResponseDTO(usuario);
    }
}
