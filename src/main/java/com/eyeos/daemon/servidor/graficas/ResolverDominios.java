package com.eyeos.daemon.servidor.graficas;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ResolverDominios implements AutoCloseable {

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final ExecutorService ejecutor;
    private final Function<String, String> resolutor;
    private final long timeoutMs;

    public ResolverDominios() {
        this(null, 2500);
    }

    public ResolverDominios(Function<String, String> resolutor, long timeoutMs) {
        this.resolutor = resolutor;
        this.timeoutMs = timeoutMs;
        this.ejecutor = Executors.newFixedThreadPool(32, r -> {
            Thread hilo = new Thread(r, "eyeos-dns");
            hilo.setDaemon(true);
            return hilo;
        });
    }

    public String resolver(String ip) {
        if (ip == null || ip.isBlank()) {
            return "";
        }
        return cache.computeIfAbsent(ip, this::resolverSinCache);
    }

    public Map<String, String> resolverTodos(Collection<String> ips) {
        Map<String, String> resultado = new ConcurrentHashMap<>();
        List<String> pendientes = new ArrayList<>();
        for (String ip : ips) {
            if (ip == null || ip.isBlank()) {
                continue;
            }
            String cacheado = cache.get(ip);
            if (cacheado != null) {
                resultado.put(ip, cacheado);
            } else if (!pendientes.contains(ip)) {
                pendientes.add(ip);
            }
        }
        if (pendientes.isEmpty()) {
            return resultado;
        }
        if (resolutor != null) {
            for (String ip : pendientes) {
                String host = resolverDirecto(ip);
                cache.put(ip, host);
                resultado.put(ip, host);
            }
            return resultado;
        }
        List<Callable<String>> tareas = new ArrayList<>();
        for (String ip : pendientes) {
            tareas.add(() -> resolverDirecto(ip));
        }
        try {
            List<Future<String>> futuros = ejecutor.invokeAll(tareas, timeoutMs, TimeUnit.MILLISECONDS);
            for (int i = 0; i < pendientes.size(); i++) {
                String ip = pendientes.get(i);
                String host = ip;
                Future<String> futuro = futuros.get(i);
                if (!futuro.isCancelled()) {
                    try {
                        host = futuro.get();
                    } catch (Exception ignored) {
                    }
                }
                cache.put(ip, host);
                resultado.put(ip, host);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            for (String ip : pendientes) {
                resultado.putIfAbsent(ip, ip);
            }
        }
        return resultado;
    }

    private String resolverSinCache(String ip) {
        try {
            if (resolutor != null) {
                return resolverDirecto(ip);
            }
            Future<String> futuro = ejecutor.submit(() -> resolverDirecto(ip));
            try {
                return futuro.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                futuro.cancel(true);
                return ip;
            }
        } catch (Exception e) {
            return ip;
        }
    }

    private String resolverDirecto(String ip) {
        try {
            if (resolutor != null) {
                return limpiar(resolutor.apply(ip), ip);
            }
            return limpiar(InetAddress.getByName(ip).getHostName(), ip);
        } catch (Exception e) {
            return ip;
        }
    }

    private String limpiar(String host, String ip) {
        if (host == null || host.isBlank() || host.equals(ip)) {
            return ip;
        }
        String limpio = host.trim().toLowerCase();
        if (limpio.endsWith(".")) {
            limpio = limpio.substring(0, limpio.length() - 1);
        }
        if (limpio.startsWith("www.")) {
            limpio = limpio.substring(4);
        }
        return limpio.isBlank() ? ip : limpio;
    }

    @Override
    public void close() {
        ejecutor.shutdownNow();
    }
}
