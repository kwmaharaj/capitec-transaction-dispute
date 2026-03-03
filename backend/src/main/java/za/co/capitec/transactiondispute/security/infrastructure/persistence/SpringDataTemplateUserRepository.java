package za.co.capitec.transactiondispute.security.infrastructure.persistence;

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.security.application.port.out.UserRepositoryPort;
import za.co.capitec.transactiondispute.security.domain.model.UserAccount;

import java.util.Arrays;
import java.util.List;

public class SpringDataTemplateUserRepository implements UserRepositoryPort {

    private final R2dbcEntityTemplate template;

    private static final String USERNAME = "username";
    private static final String PASSWORD_HASH = "password_hash";
    private static final String ROLES = "roles";
    private static final String USER_ID = "user_id";
    private static final String INSERT_QUERY ="""
                        INSERT INTO security.users (user_id, username, password_hash, roles, created_at, updated_at)
                        VALUES (:user_id, :username, :password_hash, :roles, NOW(), NOW())
                        """;
    private static final String UPSERT_QUERY = """
                                INSERT INTO security.users (user_id, username, password_hash, roles, created_at, updated_at)
                                VALUES (:user_id, :username, :password_hash, :roles, NOW(), NOW())
                                ON CONFLICT (user_id)
                                DO UPDATE SET
                                    username = EXCLUDED.username,
                                    password_hash = EXCLUDED.password_hash,
                                    roles = EXCLUDED.roles,
                                    updated_at = NOW()
                                """;

    public SpringDataTemplateUserRepository(R2dbcEntityTemplate template) {
        this.template = template;
    }

    @Override
    public Mono<UserAccount> findByUsername(String username) {
        var q = Query.query(Criteria.where(USERNAME).is(username));
        return template.selectOne(q, UserEntity.class)
                .map(SpringDataTemplateUserRepository::toDomain);
    }

    @Override
    public Mono<Void> insert(UserAccount user) {
        var db = template.getDatabaseClient();
        return db.sql(INSERT_QUERY)
                .bind(USER_ID, user.userId())
                .bind(USERNAME, user.username())
                .bind(PASSWORD_HASH, user.passwordHash())
                .bind(ROLES, user.roles().toArray(new String[0]))
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public Mono<Void> upsertAll(Flux<UserAccount> users) {
        var db = template.getDatabaseClient();

        return users
                .flatMap(u -> db.sql(UPSERT_QUERY)
                        .bind(USER_ID, u.userId())
                        .bind(USERNAME, u.username())
                        .bind(PASSWORD_HASH, u.passwordHash())
                        .bind(ROLES, u.roles().toArray(new String[0]))
                        .fetch()
                        .rowsUpdated()
                )
                .then();
    }

    private static UserAccount toDomain(UserEntity e) {
        String[] rolesArray = e.getRoles();
        List<String> roles = rolesArray == null ? List.of() : Arrays.asList(rolesArray);
        return new UserAccount(e.getUserId(), e.getUsername(), e.getPasswordHash(), roles);
    }
}