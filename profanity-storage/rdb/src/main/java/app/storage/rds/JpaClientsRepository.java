package app.storage.rds;

import app.domain.client.Clients;
import app.domain.client.ClientsRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaClientsRepository extends ClientsRepository, JpaRepository<Clients, UUID> {
    @Override
    @Modifying
    @Query("""
            update clients
            set requestCount = (select count(r.id)
                                 from records r
                                 where r.apiKey = clients.api_key)
            where expiredAt is null
            """)
    void updateClientRequestCount();
}
