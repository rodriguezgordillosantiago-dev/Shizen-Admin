package com.shizen.Admin.repository;

import com.shizen.Admin.model.Usuario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface UsuariosRepositoryCustom {

    List<Usuario> buscarConFiltros(
            String buscar,
            String rol,
            String baneado,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta);

    Map<Integer, String> obtenerFotosPorUsuario();
}
