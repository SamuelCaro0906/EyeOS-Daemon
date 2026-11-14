package com.eyeos.daemon.dao;

import com.eyeos.daemon.model.Usuario;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAOArrayImpl implements UsuarioDAO {

    // Simulación de una base de datos en memoria
    private List<Usuario> baseDeDatosMemoria = new ArrayList<>();

    @Override
    public void guardar(Usuario usuario) {
        baseDeDatosMemoria.add(usuario);
        System.out.println("DAO: Usuario guardado exitosamente -> " + usuario.getNombre());
    }

    @Override
    public Usuario buscarPorId(int id) {
        for (Usuario u : baseDeDatosMemoria) {
            if (u.getId() == id) {
                return u;
            }
        }
        return null; // Retorna null si no lo encuentra
    }

    @Override
    public List<Usuario> listarTodos() {
        return new ArrayList<>(baseDeDatosMemoria);
    }

    @Override
    public void actualizar(Usuario usuario) {
        Usuario existente = buscarPorId(usuario.getId());
        if (existente != null) {
            existente.setNombre(usuario.getNombre());
            existente.setCorreo(usuario.getCorreo());
            existente.setActivo(usuario.isActivo());
            System.out.println("DAO: Usuario actualizado -> " + usuario.getId());
        }
    }

    @Override
    public void eliminar(Integer id) {
        Usuario existente = buscarPorId(id);
        if (existente != null) {
            baseDeDatosMemoria.remove(existente);
            System.out.println("DAO: Usuario eliminado con ID -> " + id);
        }
    }

    @Override
    public Usuario buscarPorCorreo(String correo) {
        for (Usuario u : baseDeDatosMemoria) {
            if (u.getCorreo().equalsIgnoreCase(correo)) {
                return u;
            }
        }
        return null;
    }
}