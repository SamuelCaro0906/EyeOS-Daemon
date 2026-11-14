package com.eyeos.daemon.dao;

import com.eyeos.daemon.model.Usuario;
import java.util.List;

public interface UsuarioDAO extends GenericDAO<Usuario, Integer> {
    // Aquí podrías agregar métodos específicos si los necesitas, ej:
    Usuario buscarPorCorreo(String correo);
}