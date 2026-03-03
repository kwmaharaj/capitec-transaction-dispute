package za.co.capitec.transactiondispute.disputes.infrastructure.persistence;

import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeHistoryItem;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeHistoryRepositoryPort;

import java.util.UUID;

import static org.springframework.data.domain.Sort.Order.asc;

public class DisputeHistoryTemplateRepository implements DisputeHistoryRepositoryPort {

    private final R2dbcEntityTemplate template;

    public DisputeHistoryTemplateRepository(R2dbcEntityTemplate template) {
        this.template = template;
    }

    @Override
    public Mono<Void> append(DisputeHistoryItem item) {
        return template.insert(DisputeHistoryEntity.from(item)).then();
    }

    @Override
    public Flux<DisputeHistoryItem> findByDisputeId(UUID disputeId) {
        var q = Query.query(Criteria.where("dispute_id").is(disputeId))
                .sort(Sort.by(asc("changed_at")));

        return template.select(q, DisputeHistoryEntity.class)
                .map(DisputeHistoryEntity::toItem);
    }
}
