package com.eyeos.daemon.service;

import com.eyeos.daemon.dao.UsuarioDAO;
import com.eyeos.daemon.model.Usuario;
import java.util.List;

public class UsuarioServiceImpl implements UsuarioService {

    // Dependencia del DAO (inyectada por el constructor)
    private final UsuarioDAO usuarioDAO;

    // Constructor para inyección de dependencias
    public UsuarioServiceImpl(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    @Override
    public void registrarUsuario(Usuario usuario) {
        // Regla de negocio 1: Validar campos obligatorios
        if (usuario.getNombre() == null || usuario.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del usuario no puede estar vacío.");
        }

        // Regla de negocio 2: Validar que el correo no esté registrado previamente
        Usuario existente = usuarioDAO.buscarPorCorreo(usuario.getCorreo());
        if (existente != null) {
            throw new IllegalStateException("Ya existe un usuario registrado con el correo: " + usuario.getCorreo());
        }

        // Si pasa las validaciones, se envía al DAO para guardarlo
        usuarioDAO.guardar(usuario);
        System.out.println("SERVICE: Usuario validado y registrado con éxito.");
    }

    @Override
    public Usuario consultarUsuarioPorId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID de usuario debe ser mayor a cero.");
        }
        return usuarioDAO.buscarPorId(id);
    }

    @Override
    public List<Usuario> listarUsuarios() {
        return usuarioDAO.listarTodos();
    }

    @Override
    public void modificarUsuario(Usuario usuario) {
        if (usuario.getId() <= 0) {
            throw new IllegalArgumentException("No se puede actualizar un usuario sin un ID válido.");
        }
        usuarioDAO.actualizar(usuario);
    }

    @Override
    public void borrarUsuario(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID inválido para la eliminación.");
        }
        usuarioDAO.eliminar(id);
    }
}