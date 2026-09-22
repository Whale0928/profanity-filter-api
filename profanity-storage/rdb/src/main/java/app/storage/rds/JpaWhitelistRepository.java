package app.storage.rds;

import app.domain.whitelist.Whitelist;
import app.domain.whitelist.WhitelistRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaWhitelistRepository
    extends WhitelistRepository, JpaRepository<Whitelist, UUID> {}
