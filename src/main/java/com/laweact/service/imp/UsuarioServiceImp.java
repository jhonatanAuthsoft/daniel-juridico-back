package com.laweact.service.imp;

import com.laweact.config.JwtUtil;
import com.laweact.config.exception.CustomError;
import com.laweact.dto.usuario.CadastrarUsuarioInputDTO;
import com.laweact.dto.usuario.EditarUsuarioInputDTO;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.dto.usuario.LoginUsuarioResponseDTO;
import com.laweact.dto.usuario.RedefinirSenhaInputDTO;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    @Transactional
    public void redefinirSenha(RedefinirSenhaInputDTO redefinirSenhaInputDTO) {
        UsuarioEntity usuario = usuarioRepository
            .findByEmail(redefinirSenhaInputDTO.email().toLowerCase())
            .orElseThrow(() ->
                new CustomError("Usuário não encontrado", HttpStatus.BAD_REQUEST)
            );

        if (usuario.getStatus() != StatusUsuarioEnum.ATIVO) {
            throw new CustomError("Usuário inativo", HttpStatus.BAD_REQUEST);
        }

        String novaSenha = gerarSenhaAleatoria();
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        // Como não há EmailService no Daniel Jurídico, apenas logamos a nova senha de forma mockada.
        log.info("Senha redefinida para o e-mail: {}. Nova senha temporária gerada: {}", usuario.getEmail(), novaSenha);
    }

    private String gerarSenhaAleatoria() {
        String maiusculas = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String minusculas = "abcdefghijklmnopqrstuvwxyz";
        String numeros = "0123456789";
        
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder password = new StringBuilder();
        
        password.append(maiusculas.charAt(random.nextInt(maiusculas.length())));
        password.append(minusculas.charAt(random.nextInt(minusculas.length())));
        password.append(numeros.charAt(random.nextInt(numeros.length())));
        
        String todos = maiusculas + minusculas + numeros;
        for (int i = 0; i < 9; i++) {
            password.append(todos.charAt(random.nextInt(todos.length())));
        }
        
        List<Character> characters = new java.util.ArrayList<>();
        for (char c : password.toString().toCharArray()) {
            characters.add(c);
        }
        java.util.Collections.shuffle(characters, random);
        
        StringBuilder shuffledPassword = new StringBuilder();
        for (char c : characters) {
            shuffledPassword.append(c);
        }
        
        return shuffledPassword.toString();
    }

    @Override
    @Transactional
    public UsuarioResponseDTO editar(UUID id, EditarUsuarioInputDTO input) {
        UsuarioEntity usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new CustomError("Usuário não encontrado", HttpStatus.NOT_FOUND));

        Optional<UsuarioEntity> existingUser = usuarioRepository.findByEmail(input.email().toLowerCase());
        if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
            throw new CustomError("E-mail já cadastrado para outro usuário", HttpStatus.BAD_REQUEST);
        }

        usuario.setNomeCompleto(input.nomeCompleto());
        usuario.setEmail(input.email().toLowerCase());
        usuario.setPerfil(input.perfil());
        usuario.setStatus(input.status());
        usuario.setTelefone(input.telefone());

        if (input.senha() != null && !input.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(input.senha()));
        }

        UsuarioEntity saved = usuarioRepository.save(usuario);
        return usuarioMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public void excluir(UUID id) {
        UsuarioEntity usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new CustomError("Usuário não encontrado", HttpStatus.NOT_FOUND));

        // Neste projeto, como não há relacionamentos de chaves estrangeiras amarrados a UsuarioEntity na persistência atual, a deleção é direta.
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

