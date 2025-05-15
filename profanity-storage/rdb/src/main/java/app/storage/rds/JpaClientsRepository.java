package app.storage.rds;

import app.domain.client.Clients;
import app.domain.client.ClientsRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface JpaClientsRepository extends ClientsRepository, JpaRepository<Clients, UUID> {
    @Override
    @Modifying
    @Query("""
            update clients c
            set c.requestCount = (select count(r.id)
                                  from records r
                                  where r.apiKey = c.apiKey)
            where c.expiredAt is null
            """)
    void updateClientRequestCount();
}
