package com.unicheck.security;

import com.unicheck.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** O Security usa o RA como username, mantendo email apenas como contato. */
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String ra) {
        // A mensagem continua generica mesmo nesta camada para nunca confirmar que um RA existe.
        return usuarioRepository.findByRa(ra)
                .orElseThrow(() -> new UsernameNotFoundException("RA ou senha incorretos"));
    }
}
