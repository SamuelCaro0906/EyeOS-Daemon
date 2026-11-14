package com.eyeos.daemon.service;

import com.eyeos.daemon.model.Usuario;
import java.util.List;

public interface UsuarioService {
    void registrarUsuario(Usuario usuario);
    Usuario consultarUsuarioPorId(int id);
    List<Usuario> listarUsuarios();
    void modificarUsuario(Usuario usuario);
    void borrarUsuario(int id);
}
