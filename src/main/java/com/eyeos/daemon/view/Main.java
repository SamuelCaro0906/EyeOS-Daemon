package com.eyeos.daemon;

import com.eyeos.daemon.dao.UsuarioDAO;
import com.eyeos.daemon.dao.UsuarioDAOArrayImpl;
import com.eyeos.daemon.model.Usuario;
import com.eyeos.daemon.service.UsuarioService;
import com.eyeos.daemon.service.UsuarioServiceImpl;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== INICIANDO SISTEMA EYEOS DAEMON ===");

        // 1. Instanciar la capa DAO (Persistencia en memoria)
        UsuarioDAO usuarioDAO = new UsuarioDAOArrayImpl();

        // 2. Instanciar la capa Service inyectándole el DAO
        UsuarioService usuarioService = new UsuarioServiceImpl(usuarioDAO);

        try {
            // 3. Crear objetos de prueba (Model)
            Usuario u1 = new Usuario(1, "Samuel Caro", "samuel@email.com", true);
            Usuario u2 = new Usuario(2, "Ana Pérez", "ana@email.com", true);

            // 4. Registrar usuarios a través del Service
            System.out.println("\n--- Registrando Usuarios ---");
            usuarioService.registrarUsuario(u1);
            usuarioService.registrarUsuario(u2);

            // 5. Listar usuarios registrados
            System.out.println("\n--- Listado Actual de Usuarios ---");
            for (Usuario u : usuarioService.listarUsuarios()) {
                System.out.println(u);
            }

            // 6. Probar validación de negocio (Intentar registrar un correo duplicado)
            System.out.println("\n--- Probando Validación de Duplicados ---");
            Usuario uDuplicado = new Usuario(3, "Carlos Test", "samuel@email.com", true);
            usuarioService.registrarUsuario(uDuplicado);

        } catch (Exception e) {
            System.err.println("¡Excepción capturada por regla de negocio!: " + e.getMessage());
        }

        System.out.println("\n=== FIN DE LA PRUEBA DE ARQUITECTURA ===");
    }
}