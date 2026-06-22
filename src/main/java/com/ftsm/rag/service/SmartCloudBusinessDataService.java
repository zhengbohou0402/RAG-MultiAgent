package com.ftsm.rag.service;

import com.ftsm.rag.config.AppConfig;
import com.ftsm.rag.model.BillingSummary;
import com.ftsm.rag.model.InvoiceRecord;
import com.ftsm.rag.model.SmartCloudUserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SmartCloudBusinessDataService {

    private final AppConfig appConfig;
    private final Environment environment;

    public SmartCloudBusinessDataService(AppConfig appConfig, Environment environment) {
        this.appConfig = appConfig;
        this.environment = environment;
    }

    public Optional<SmartCloudUserContext> authenticate(String username, String password, String tenantId) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        String normalizedTenant = tenantId == null || tenantId.isBlank()
                ? appConfig.getSmartcloud().getDefaultTenantId()
                : tenantId.trim();
        if (appConfig.getSmartcloud().isMysqlEnabled()) {
            Optional<SmartCloudUserContext> sqlUser = tryAuthenticateFromMysql(username, password, normalizedTenant);
            if (sqlUser.isPresent()) {
                return sqlUser;
            }
        }
        if ("demo-admin".equals(username.trim()) && "demo123456".equals(password)) {
            return Optional.of(SmartCloudUserContext.demo());
        }
        return Optional.empty();
    }

    public BillingSummary billingSummary(SmartCloudUserContext user, String query) {
        SmartCloudUserContext effectiveUser = user == null ? SmartCloudUserContext.demo() : user;
        if (appConfig.getSmartcloud().isMysqlEnabled()) {
            BillingSummary sqlSummary = tryBillingFromMysql(effectiveUser);
            if (sqlSummary != null) {
                return sqlSummary;
            }
        }
        String topProduct = containsAny(query, "gpu", "GPU", "llm", "LLM", "ai", "AI")
                ? "GPU Cloud Instance"
                : "ECS Standard Instance";
        return new BillingSummary(
                "demo-enterprise-001",
                effectiveUser.tenantId(),
                "2026-06",
                "RM 1,286.40",
                "RM 286.40",
                topProduct,
                "Enable reserved-instance billing for stable workloads and keep auto-scaling for burst traffic."
        );
    }

    public List<InvoiceRecord> invoices(SmartCloudUserContext user) {
        SmartCloudUserContext effectiveUser = user == null ? SmartCloudUserContext.demo() : user;
        if (appConfig.getSmartcloud().isMysqlEnabled()) {
            List<InvoiceRecord> sqlInvoices = tryInvoicesFromMysql(effectiveUser);
            if (!sqlInvoices.isEmpty()) {
                return sqlInvoices;
            }
        }
        return List.of(
                new InvoiceRecord("inv-demo-001", "INV-202606-001", "RM 860.00", "PAID", "2026-06-03"),
                new InvoiceRecord("inv-demo-002", "INV-202606-002", "RM 426.40", "UNPAID", "2026-06-16")
        );
    }

    public void upsertConversationCase(String conversationId, SmartCloudUserContext user, String title, String route) {
        SmartCloudUserContext effectiveUser = user == null ? SmartCloudUserContext.demo() : user;
        if (!appConfig.getSmartcloud().isMysqlEnabled()) {
            return;
        }
        String sql = """
                INSERT INTO conversation_cases(id, tenant_id, user_id, title, route, status, updated_at)
                VALUES (?, ?, ?, ?, ?, 'OPEN', ?)
                ON DUPLICATE KEY UPDATE title = VALUES(title), route = VALUES(route), updated_at = VALUES(updated_at)
                """;
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, conversationId);
            statement.setString(2, effectiveUser.tenantId());
            statement.setString(3, effectiveUser.userId());
            statement.setString(4, title);
            statement.setString(5, route);
            statement.setLong(6, System.currentTimeMillis() / 1000);
            statement.executeUpdate();
        } catch (Exception error) {
            log.warn("MySQL conversation case write failed, using local conversation store only: {}", error.getMessage());
        }
    }

    public void rollbackConversationCase(String conversationId, SmartCloudUserContext user, String reason) {
        SmartCloudUserContext effectiveUser = user == null ? SmartCloudUserContext.demo() : user;
        if (!appConfig.getSmartcloud().isMysqlEnabled()) {
            return;
        }
        try (Connection connection = connection()) {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM conversation_cases WHERE id = ? AND tenant_id = ?")) {
                delete.setString(1, conversationId);
                delete.setString(2, effectiveUser.tenantId());
                delete.executeUpdate();
            }
            try (PreparedStatement logStatement = connection.prepareStatement(
                    "INSERT INTO saga_compensation_log(id, tenant_id, operation, detail) VALUES (?, ?, ?, ?)")) {
                logStatement.setString(1, java.util.UUID.randomUUID().toString());
                logStatement.setString(2, effectiveUser.tenantId());
                logStatement.setString(3, "conversation_case_rollback");
                logStatement.setString(4, reason);
                logStatement.executeUpdate();
            }
        } catch (Exception error) {
            log.warn("MySQL compensation log failed: {}", error.getMessage());
        }
    }

    private Optional<SmartCloudUserContext> tryAuthenticateFromMysql(String username, String password, String tenantId) {
        String sql = "SELECT id, tenant_id, username, display_name, role_name FROM users WHERE username = ? AND tenant_id = ? AND password_hash = ?";
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username.trim());
            statement.setString(2, tenantId);
            statement.setString(3, password);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new SmartCloudUserContext(
                            rs.getString("tenant_id"),
                            rs.getString("id"),
                            rs.getString("username"),
                            rs.getString("display_name"),
                            rs.getString("role_name")
                    ));
                }
            }
        } catch (Exception error) {
            log.warn("MySQL auth lookup failed, falling back to demo user: {}", error.getMessage());
        }
        return Optional.empty();
    }

    private BillingSummary tryBillingFromMysql(SmartCloudUserContext user) {
        String sql = "SELECT id, billing_month, total_cost, unpaid_amount, currency, top_product, recommendation FROM billing_accounts WHERE tenant_id = ? ORDER BY billing_month DESC LIMIT 1";
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.tenantId());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    String currency = rs.getString("currency");
                    return new BillingSummary(
                            rs.getString("id"),
                            user.tenantId(),
                            rs.getString("billing_month"),
                            money(currency, rs.getBigDecimal("total_cost")),
                            money(currency, rs.getBigDecimal("unpaid_amount")),
                            rs.getString("top_product"),
                            rs.getString("recommendation")
                    );
                }
            }
        } catch (Exception error) {
            log.warn("MySQL billing lookup failed, falling back to demo snapshot: {}", error.getMessage());
        }
        return null;
    }

    private List<InvoiceRecord> tryInvoicesFromMysql(SmartCloudUserContext user) {
        List<InvoiceRecord> invoices = new ArrayList<>();
        String sql = "SELECT id, invoice_no, amount, status, issued_at FROM invoices WHERE tenant_id = ? ORDER BY issued_at DESC";
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.tenantId());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    invoices.add(new InvoiceRecord(
                            rs.getString("id"),
                            rs.getString("invoice_no"),
                            "RM " + rs.getBigDecimal("amount"),
                            rs.getString("status"),
                            rs.getDate("issued_at").toString()
                    ));
                }
            }
        } catch (Exception error) {
            log.warn("MySQL invoice lookup failed, falling back to demo invoices: {}", error.getMessage());
        }
        return invoices;
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                environment.getProperty("spring.datasource.url"),
                environment.getProperty("spring.datasource.username"),
                environment.getProperty("spring.datasource.password")
        );
    }

    private String money(String currency, BigDecimal amount) {
        return currency + " " + amount.setScale(2).toPlainString();
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
