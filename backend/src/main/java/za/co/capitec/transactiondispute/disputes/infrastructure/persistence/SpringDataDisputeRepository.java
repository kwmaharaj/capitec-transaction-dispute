package za.co.capitec.transactiondispute.disputes.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeRepositoryPort;
import za.co.capitec.transactiondispute.disputes.domain.model.AuditMetadata;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;
import za.co.capitec.transactiondispute.disputes.domain.model.ReasonCode;

import java.util.List;
import java.util.UUID;

import static org.springframework.data.domain.Sort.Order.desc;

public class SpringDataDisputeRepository implements DisputeRepositoryPort {

    private final R2dbcEntityTemplate template;

    public SpringDataDisputeRepository(R2dbcEntityTemplate template) {
        this.template = template;
    }

    @Override
    public Mono<Dispute> save(Dispute dispute) {
        var entity = toEntity(dispute);
        var byId = Query.query(Criteria.where("dispute_id").is(dispute.disputeId()));
        //only save decides if true or not
        return template.selectOne(byId, DisputeEntity.class)
                .flatMap(existing -> template.update(entity.markNew(false)))
                .switchIfEmpty(template.insert(entity.markNew(true)))
                .thenReturn(dispute);
    }

    @Override
    public Mono<Boolean> existsForTransactionAndUser(UUID transactionId, UUID userId) {
        var q = Query.query(
                Criteria.where("transaction_id").is(transactionId)
                        .and("user_id").is(userId)
        );
        return template.exists(q, DisputeEntity.class).defaultIfEmpty(false);
    }

    @Override
    public Flux<Dispute> findByUserId(UUID userId) {
        var q = Query.query(Criteria.where("user_id").is(userId))
                .sort(Sort.by(desc("createdAt")));

        return template.select(q, DisputeEntity.class)
                .map(SpringDataDisputeRepository::toDomain);
    }

    @Override
    public Mono<Page<Dispute>> findByDisputeStatus(DisputeStatus disputeStatus, Pageable pageable) {
        var sort = Sort.by(desc("createdAt"));

        Query contentQuery;
        Query countQuery;

        if (disputeStatus == null) {
            contentQuery = Query.empty();
            countQuery = Query.empty();
        } else {
            var criteria = Criteria.where("dispute_status").is(disputeStatus);
            contentQuery = Query.query(criteria);
            countQuery = Query.query(criteria);
        }

        contentQuery = contentQuery
                .sort(sort)
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset());

        Mono<Long> totalMono = template.count(countQuery, DisputeEntity.class);

        Mono<List<Dispute>> contentMono = template.select(contentQuery, DisputeEntity.class)
                .map(SpringDataDisputeRepository::toDomain)
                .collectList();

        return Mono.zip(contentMono, totalMono)
                .map(t -> new PageImpl<>(t.getT1(), pageable, t.getT2()));
    }

    @Override
    public Mono<Dispute> findById(UUID disputeId) {
        var q = Query.query(Criteria.where("dispute_id").is(disputeId));
        return template.selectOne(q, DisputeEntity.class)
                .map(SpringDataDisputeRepository::toDomain);
    }

    private static DisputeEntity toEntity(Dispute d) {
        return new DisputeEntity(
                d.disputeId(),
                d.transactionId(),
                d.userId(),
                d.reasonCode().name(),
                d.note(),
                d.disputeStatus(),
                new DisputeAudit(d.createdAt(), d.updatedAt())
        );
    }

    private static Dispute toDomain(DisputeEntity e) {
        return Dispute.rehydrate(
                e.getId(),
                e.getTransactionId(),
                e.getUserId(),
                ReasonCode.valueOf(e.getReasonCode()),
                e.getNote(),
                e.getDisputeStatus(),
                new AuditMetadata(e.getCreatedAt(),e.getUpdatedAt())
        );
    }
}
