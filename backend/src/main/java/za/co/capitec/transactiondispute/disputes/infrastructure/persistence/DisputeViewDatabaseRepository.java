package za.co.capitec.transactiondispute.disputes.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeSearchCriteria;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeTransactionViewRow;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeViewRepositoryPort;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;
import za.co.capitec.transactiondispute.disputes.domain.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class DisputeViewDatabaseRepository implements DisputeViewRepositoryPort {

    private final DatabaseClient db;

    private static final String AND = " AND ";

    public DisputeViewDatabaseRepository(DatabaseClient db) {
        this.db = db;
    }

    @Override
    public Mono<Page<DisputeTransactionViewRow>> searchForUser(UUID userId, DisputeSearchCriteria criteria, Pageable pageable) {
        return search(Optional.of(userId), criteria, pageable);
    }

    @Override
    public Mono<Page<DisputeTransactionViewRow>> searchForSupport(DisputeSearchCriteria criteria, Pageable pageable) {
        return search(Optional.empty(), criteria, pageable);
    }

    private Mono<Page<DisputeTransactionViewRow>> search(Optional<UUID> userIdOpt, DisputeSearchCriteria c, Pageable pageable) {

        DisputeSearchCriteria criteria = (c == null) ? DisputeSearchCriteria.empty() : c;

        var where = new StringBuilder(" WHERE 1=1 ");
        var binds = new HashMap<String, Object>();

        userIdOpt.ifPresent(userId -> {
            where.append(" AND d.user_id = :user_id ");
            binds.put("user_id", userId);
        });
        if (criteria.disputeId() != null) {
            where.append(" AND d.dispute_id = :dispute_id ");
            binds.put("dispute_id", criteria.disputeId());
        }

        addLike(where, binds, "merchant", "t.merchant", criteria.merchant());
        addLike(where, binds, "rrn", "t.rrn", criteria.rrn());
        addEqEnum(where, binds, "dispute_status", "d.dispute_status", criteria.disputeStatus());
        addEqEnum(where, binds, "tx_status", "t.transaction_status", criteria.transactionStatus());
        addAmountRange(where, binds, criteria.minAmount(), criteria.maxAmount());
        addDateRange(where, binds, criteria.fromDate(), criteria.toDate());

        String baseSelect = """
                SELECT
                  d.dispute_id,
                  d.transaction_id,
                  d.user_id,
                  d.dispute_status,
                  d.reason_code,
                  d.note AS dispute_note,
                  d.created_at,
                  t.posted_at,
                  t.amount,
                  t.currency,
                  t.merchant,
                  t.transaction_status,
                  t.rrn
                FROM disputes.disputes d
                JOIN transactions.transactions t ON t.transaction_id = d.transaction_id
                """;

        String order = " ORDER BY d.created_at DESC ";
        String paging = " LIMIT :limit OFFSET :offset ";

        String sql = baseSelect + where + order + paging;
        String countSql = "SELECT COUNT(*) AS total FROM disputes.disputes d JOIN transactions.transactions t ON t.transaction_id = d.transaction_id " + where;

        var offset = pageable.getOffset();
        var limit = pageable.getPageSize();

        Mono<Long> totalMono = bindAll(db.sql(countSql), binds)
                .map((row, meta) -> row.get("total", Long.class))
                .one()
                .defaultIfEmpty(0L);

        Mono<List<DisputeTransactionViewRow>> contentMono = bindAll(db.sql(sql), binds)
                .bind("limit", limit)
                .bind("offset", offset)
                .map((row, meta) -> new DisputeTransactionViewRow(
                        row.get("dispute_id", UUID.class),
                        row.get("transaction_id", UUID.class),
                        row.get("user_id", UUID.class),
                        DisputeStatus.valueOf(row.get("dispute_status", String.class)),
                        row.get("reason_code", String.class),
                        row.get("dispute_note", String.class),
                        row.get("created_at", Instant.class),
                        row.get("posted_at", Instant.class),
                        row.get("amount", BigDecimal.class),
                        row.get("currency", String.class),
                        row.get("merchant", String.class),
                        TransactionStatus.valueOf(row.get("transaction_status", String.class)),
                        row.get("rrn", String.class)
                ))
                .all()
                .collectList();

        return Mono.zip(contentMono, totalMono)
                .map(t -> new PageImpl<>(t.getT1(), pageable, t.getT2()));
    }

    private static DatabaseClient.GenericExecuteSpec bindAll(DatabaseClient.GenericExecuteSpec spec, Map<String, Object> binds) {
        DatabaseClient.GenericExecuteSpec s = spec;
        for (var e : binds.entrySet()) {
            s = s.bind(e.getKey(), e.getValue());
        }
        return s;
    }

    private static void addLike(StringBuilder where, Map<String, Object> binds, String bindName, String col, String value) {
        if (value == null || value.isBlank()) return;
        where.append(AND).append(col).append(" ILIKE :").append(bindName).append(' ');
        binds.put(bindName, "%" + value.trim() + "%");
    }

    private static void addAmountRange(StringBuilder where, Map<String, Object> binds, BigDecimal min, BigDecimal max) {
        if (min != null) {
            where.append(" AND t.amount >= :min_amount ");
            binds.put("min_amount", min);
        }
        if (max != null) {
            where.append(" AND t.amount <= :max_amount ");
            binds.put("max_amount", max);
        }
    }

    private static void addDateRange(StringBuilder where, Map<String, Object> binds, Instant from, Instant to) {
        if (from != null) {
            where.append(" AND t.posted_at >= :from_date ");
            binds.put("from_date", from);
        }
        if (to != null) {
            where.append(" AND t.posted_at <= :to_date ");
            binds.put("to_date", to);
        }
    }

    private static <E extends Enum<E>> void addEqEnum(
            StringBuilder where,
            Map<String, Object> binds,
            String bindName,
            String col,
            E enumValue
    ) {
        if (enumValue == null) return;
        where.append(AND).append(col).append(" = :").append(bindName).append(' ');
        binds.put(bindName, enumValue.name());
    }
}
