package br.com.eventflow.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    public void logLoginSucesso(UUID userId, String email) {
        log.info("[AUDITORIA] acao=LOGIN_SUCESSO userId={} email={} timestamp={}",
                userId, email, Instant.now());
    }

    public void logLoginFalha(String email) {
        log.warn("[AUDITORIA] acao=LOGIN_FALHA email={} timestamp={}",
                email, Instant.now());
    }

    public void logAcessoNegado(UUID userId, String endpoint) {
        log.warn("[AUDITORIA] acao=ACESSO_NEGADO userId={} endpoint={} timestamp={}",
                userId, endpoint, Instant.now());
    }

    public void logAcao(String acao, UUID userId, String detalhe) {
        log.info("[AUDITORIA] acao={} userId={} detalhe={} timestamp={}",
                acao, userId, detalhe, Instant.now());
    }
}
