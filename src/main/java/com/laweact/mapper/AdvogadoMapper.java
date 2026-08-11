package com.laweact.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.laweact.dto.advogado.AdvogadoDetalheResponseDTO;
import com.laweact.dto.advogado.AdvogadoPerfilResponseDTO;
import com.laweact.dto.advogado.AreaAtuacaoResponseDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoResponseDTO;
import com.laweact.dto.advogado.CatalogoItemResponseDTO;
import com.laweact.dto.advogado.EspecialidadeResponseDTO;
import com.laweact.dto.advogado.OabResponseDTO;
import com.laweact.dto.advogado.PosGraduacaoResponseDTO;
import com.laweact.dto.cliente.EnderecoResponseDTO;
import com.laweact.model.entity.AdvogadoEntity;
import com.laweact.model.entity.AdvogadoEspecialidadeEntity;
import com.laweact.model.entity.AdvogadoFormaCobrancaEntity;
import com.laweact.model.entity.AdvogadoModalidadeEntity;
import com.laweact.model.entity.AreaAtuacaoAdvogadoEntity;
import com.laweact.model.entity.EnderecoEntity;
import com.laweact.model.entity.OabEntity;
import com.laweact.model.entity.PosGraduacaoAdvogadoEntity;
import com.laweact.model.entity.UsuarioEntity;

@Component
public class AdvogadoMapper {

    private final UsuarioMapper usuarioMapper;
    private final ClienteMapper clienteMapper;

    public AdvogadoMapper(UsuarioMapper usuarioMapper, ClienteMapper clienteMapper) {
        this.usuarioMapper = usuarioMapper;
        this.clienteMapper = clienteMapper;
    }

    public AdvogadoPerfilResponseDTO toPerfilResponse(AdvogadoEntity advogado) {
        return AdvogadoPerfilResponseDTO.builder()
                .usuarioId(advogado.getUsuarioId())
                .nomeCompleto(advogado.getNomeCompleto())
                .nomeSocial(advogado.getNomeSocial())
                .rg(advogado.getRg())
                .rgOrgaoEmissor(advogado.getRgOrgaoEmissor())
                .rgUf(advogado.getRgUf())
                .cpf(advogado.getCpf())
                .nomePai(advogado.getNomePai())
                .nomeMae(advogado.getNomeMae())
                .pronomeTratamento(advogado.getPronomeTratamento())
                .fotoUrl(advogado.getFotoUrl())
                .universidade(advogado.getUniversidade())
                .curso(advogado.getCurso())
                .anoFormacao(advogado.getAnoFormacao())
                .atuacaoDesde(advogado.getAtuacaoDesde())
                .biografia(advogado.getBiografia())
                .disponibilidade(advogado.getDisponibilidade())
                .statusVerificacao(advogado.getStatusVerificacao())
                .mediaAvaliacoes(advogado.getMediaAvaliacoes())
                .totalAvaliacoes(advogado.getTotalAvaliacoes())
                .build();
    }

    public OabResponseDTO toOabResponse(OabEntity oab) {
        return OabResponseDTO.builder()
                .id(oab.getId())
                .numero(oab.getNumero())
                .uf(oab.getUf())
                .dataExpedicao(oab.getDataExpedicao())
                .principal(oab.getPrincipal())
                .fotoFrenteUrl(oab.getFotoFrenteUrl())
                .fotoVersoUrl(oab.getFotoVersoUrl())
                .statusValidacao(oab.getStatusValidacao())
                .build();
    }

    public AreaAtuacaoResponseDTO toAreaResponse(AreaAtuacaoAdvogadoEntity area) {
        return AreaAtuacaoResponseDTO.builder()
                .id(area.getId())
                .estado(area.getEstado())
                .cidade(area.getCidade())
                .build();
    }

    public CatalogoItemResponseDTO toModalidadeResponse(AdvogadoModalidadeEntity entity) {
        return CatalogoItemResponseDTO.builder()
                .codigo(entity.getModalidade().getCodigo())
                .nome(entity.getModalidade().getNome())
                .build();
    }

    public CatalogoItemResponseDTO toFormaCobrancaResponse(AdvogadoFormaCobrancaEntity entity) {
        return CatalogoItemResponseDTO.builder()
                .codigo(entity.getFormaCobranca().getCodigo())
                .nome(entity.getFormaCobranca().getNome())
                .build();
    }

    public EspecialidadeResponseDTO toEspecialidadeResponse(AdvogadoEspecialidadeEntity entity) {
        return EspecialidadeResponseDTO.builder()
                .id(entity.getId())
                .especialidadeCodigo(entity.getEspecialidade() != null ? entity.getEspecialidade().getCodigo() : null)
                .especialidadeNome(entity.getEspecialidade() != null ? entity.getEspecialidade().getNome() : null)
                .especialidadeLivre(entity.getEspecialidadeLivre())
                .subespecialidadeCodigo(entity.getSubespecialidade() != null ? entity.getSubespecialidade().getCodigo() : null)
                .subespecialidadeNome(entity.getSubespecialidade() != null ? entity.getSubespecialidade().getNome() : null)
                .subespecialidadeLivre(entity.getSubespecialidadeLivre())
                .build();
    }

    public PosGraduacaoResponseDTO toPosGraduacaoResponse(PosGraduacaoAdvogadoEntity entity) {
        return PosGraduacaoResponseDTO.builder()
                .id(entity.getId())
                .nomeCurso(entity.getNomeCurso())
                .instituicao(entity.getInstituicao())
                .anoFormacao(entity.getAnoFormacao())
                .build();
    }

    public AdvogadoDetalheResponseDTO toDetalheResponse(
            AdvogadoEntity advogado,
            EnderecoEntity endereco,
            List<OabEntity> oabs,
            List<AreaAtuacaoAdvogadoEntity> areas,
            List<AdvogadoModalidadeEntity> modalidades,
            List<AdvogadoEspecialidadeEntity> especialidades,
            List<AdvogadoFormaCobrancaEntity> formasCobranca,
            List<PosGraduacaoAdvogadoEntity> posGraduacoes
    ) {
        return AdvogadoDetalheResponseDTO.builder()
                .perfil(toPerfilResponse(advogado))
                .endereco(endereco != null ? clienteMapper.toEnderecoResponse(endereco) : null)
                .oabs(oabs.stream().map(this::toOabResponse).toList())
                .areasAtuacao(areas.stream().map(this::toAreaResponse).toList())
                .modalidades(modalidades.stream().map(this::toModalidadeResponse).toList())
                .especialidades(especialidades.stream().map(this::toEspecialidadeResponse).toList())
                .formasCobranca(formasCobranca.stream().map(this::toFormaCobrancaResponse).toList())
                .posGraduacoes(posGraduacoes.stream().map(this::toPosGraduacaoResponse).toList())
                .build();
    }

    public CadastrarAdvogadoResponseDTO toCadastrarResponse(
            UsuarioEntity usuario,
            AdvogadoEntity advogado,
            EnderecoEntity endereco,
            List<OabEntity> oabs,
            List<AreaAtuacaoAdvogadoEntity> areas,
            List<AdvogadoModalidadeEntity> modalidades,
            List<AdvogadoEspecialidadeEntity> especialidades,
            List<AdvogadoFormaCobrancaEntity> formasCobranca,
            List<PosGraduacaoAdvogadoEntity> posGraduacoes,
            String token
    ) {
        EnderecoResponseDTO enderecoResponse = clienteMapper.toEnderecoResponse(endereco);
        return CadastrarAdvogadoResponseDTO.builder()
                .usuario(usuarioMapper.toResponseDTO(usuario))
                .advogado(toPerfilResponse(advogado))
                .endereco(enderecoResponse)
                .oabs(oabs.stream().map(this::toOabResponse).toList())
                .areasAtuacao(areas.stream().map(this::toAreaResponse).toList())
                .modalidades(modalidades.stream().map(this::toModalidadeResponse).toList())
                .especialidades(especialidades.stream().map(this::toEspecialidadeResponse).toList())
                .formasCobranca(formasCobranca.stream().map(this::toFormaCobrancaResponse).toList())
                .posGraduacoes(posGraduacoes.stream().map(this::toPosGraduacaoResponse).toList())
                .token(token)
                .build();
    }
}
