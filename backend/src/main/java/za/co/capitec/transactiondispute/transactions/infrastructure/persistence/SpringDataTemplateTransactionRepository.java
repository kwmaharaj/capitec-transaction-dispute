package za.co.capitec.transactiondispute.transactions.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.transactions.application.model.TransactionSearchCriteria;
import za.co.capitec.transactiondispute.transactions.application.port.out.TransactionRepositoryPort;
import za.co.capitec.transactiondispute.transactions.domain.model.Transaction;

import java.util.List;
import java.util.UUID;

import static org.springframework.data.domain.Sort.Order.desc;

public class SpringDataTemplateTransactionRepository implements TransactionRepositoryPort {

    private final R2dbcEntityTemplate template;

    private static final String POSTED_AT = "posted_at";
    private static final String AMOUNT = "amount";
    private static final String CURRENCY = "currency";
    private static final String MERCHANT = "merchant";
    private static final String TRANSACTION_STATUS = "transaction_status";
    private static final String BIN = "bin";
    private static final String LAST4DIGITS = "last4digits";
    private static final String PAN_HASH = "pan_hash";
    private static final String RRN = "rrn";
    private static final String USER_ID = "user_id";
    private static final String TRANSACTION_ID = "transaction_id";
    private static final String UPSERT_QUERY = """
                                INSERT INTO transactions.transactions (
                                    transaction_id, user_id, posted_at, amount, currency, merchant, transaction_status,
                                    bin, last4digits, pan_hash, rrn, created_at, updated_at
                                ) VALUES (
                                    :transaction_id, :user_id, :posted_at, :amount, :currency, :merchant, :transaction_status,
                                    :bin, :last4digits, :pan_hash, :rrn, NOW(), NOW()
                                )
                                ON CONFLICT (transaction_id)
                                DO UPDATE SET
                                    user_id = EXCLUDED.user_id,
                                    posted_at = EXCLUDED.posted_at,
                                    amount = EXCLUDED.amount,
                                    currency = EXCLUDED.currency,
                                    merchant = EXCLUDED.merchant,
                                    transaction_status = EXCLUDED.transaction_status,
                                    bin = EXCLUDED.bin,
                                    last4digits = EXCLUDED.last4digits,
                                    pan_hash = EXCLUDED.pan_hash,
                                    rrn = EXCLUDED.rrn,
                                    updated_at = NOW()
                                """;

    public SpringDataTemplateTransactionRepository(R2dbcEntityTemplate template) {
        this.template = template;
    }

    @Override
    public Flux<Transaction> findByUserId(UUID userId) {
        var query = Query.query(Criteria.where(USER_ID).is(userId))
                .sort(Sort.by(desc(POSTED_AT)));

        return template.select(query, TransactionEntity.class)
                .map(SpringDataTemplateTransactionRepository::toDomain);
    }

    @Override
    public Flux<Transaction> findAll() {
        var query = Query.empty()
                .sort(Sort.by(desc(POSTED_AT)));

        return template.select(query, TransactionEntity.class)
                .map(SpringDataTemplateTransactionRepository::toDomain);
    }

    @Override
    public Mono<Transaction> findById(UUID transactionId) {
        var query = Query.query(Criteria.where(TRANSACTION_ID).is(transactionId));
        return template.selectOne(query, TransactionEntity.class)
                .map(SpringDataTemplateTransactionRepository::toDomain);
    }

    @Override
    public Mono<Page<Transaction>> searchForUser(UUID userId, TransactionSearchCriteria c, Pageable pageable) {
        // Always constrain by user_id to avoid NPEs and to ensure user-scoped queries.
        var criteria = buildCriteria(c);
        criteria = (criteria == null) ? Criteria.where(USER_ID).is(userId) : criteria.and(USER_ID).is(userId);
        return search(criteria, pageable);
    }

    @Override
    public Mono<Page<Transaction>> searchAll(TransactionSearchCriteria c, Pageable pageable) {
        var criteria = buildCriteria(c);
        return search(criteria, pageable);
    }

    @Override
    public Mono<Void> upsertAll(Flux<Transaction> transactions) {
        DatabaseClient db = template.getDatabaseClient();

        return transactions
                .flatMap(t -> db.sql(UPSERT_QUERY)
                        .bind(TRANSACTION_ID, t.transactionId())
                        .bind(USER_ID, t.userId())
                        .bind(POSTED_AT, t.postedAt())
                        .bind(AMOUNT, t.amount())
                        .bind(CURRENCY, t.currency())
                        .bind(MERCHANT, t.merchant())
                        .bind(TRANSACTION_STATUS, t.transactionStatus().name()) //name else java.lang.IllegalArgumentException: Cannot encode parameter of type io.r2dbc.spi.Parameters$InParameter
                        .bind(BIN, t.bin())
                        .bind(LAST4DIGITS, t.last4digits())
                        .bind(PAN_HASH, t.panHash())
                        .bind(RRN, t.rrn())
                        .fetch()
                        .rowsUpdated())
                .then();
    }

    private static Transaction toDomain(TransactionEntity e) {
        return new Transaction(
                e.getTransactionId(),
                e.getUserId(),
                e.getPostedAt(),
                e.getAmount(),
                e.getCurrency(),
                e.getMerchant(),
                e.getTransactionStatus(),
                e.getBin(),
                e.getLast4digits(),
                e.getPanHash(),
                e.getRrn()
        );
    }

    private Mono<Page<Transaction>> search(Criteria criteria, Pageable pageable) {
        var sort = Sort.by(desc(POSTED_AT));

        Query contentQuery;
        Query countQuery;

        if (criteria == null) {
            contentQuery = Query.empty();
            countQuery = Query.empty();
        } else {
            contentQuery = Query.query(criteria);
            countQuery = Query.query(criteria);
        }

        contentQuery = contentQuery
                .sort(sort)
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset());

        Mono<Long> totalMono = template.count(countQuery, TransactionEntity.class);

        Mono<List<Transaction>> contentMono = template.select(contentQuery, TransactionEntity.class)
                .map(SpringDataTemplateTransactionRepository::toDomain)
                .collectList();

        return Mono.zip(contentMono, totalMono)
                .map(t -> new PageImpl<>(t.getT1(), pageable, t.getT2()));
    }

    private static Criteria buildCriteria(TransactionSearchCriteria c) {
        if (c == null) {
            return null;
        }

        Criteria criteria = null;

        if (c.minAmount() != null) {
            criteria = and(criteria, Criteria.where(AMOUNT).greaterThanOrEquals(c.minAmount()));
        }
        if (c.maxAmount() != null) {
            criteria = and(criteria, Criteria.where(AMOUNT).lessThanOrEquals(c.maxAmount()));
        }
        if (c.fromDate() != null) {
            criteria = and(criteria, Criteria.where(POSTED_AT).greaterThanOrEquals(c.fromDate()));
        }
        if (c.toDate() != null) {
            criteria = and(criteria, Criteria.where(POSTED_AT).lessThanOrEquals(c.toDate()));
        }
        if (c.rrn() != null && !c.rrn().isBlank()) {
            criteria = and(criteria, Criteria.where(RRN).like("%" + c.rrn().trim() + "%"));
        }
        if (c.merchant() != null && !c.merchant().isBlank()) {
            criteria = and(criteria, Criteria.where(MERCHANT).like("%" + c.merchant().trim() + "%"));
        }
        if (c.transactionStatus() != null) {
            criteria = and(criteria, Criteria.where(TRANSACTION_STATUS).is(c.transactionStatus()));
        }

        return criteria;
    }

    private static Criteria and(Criteria existing, Criteria next) {
        if (existing == null) return next;
        return existing.and(next);
    }

}