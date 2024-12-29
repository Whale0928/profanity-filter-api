package app.storage.rds;

import app.domain.client.Clients;
import app.domain.client.ClientsRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaClientsRepository extends ClientsRepository, JpaRepository<Clients, UUID> {
}
