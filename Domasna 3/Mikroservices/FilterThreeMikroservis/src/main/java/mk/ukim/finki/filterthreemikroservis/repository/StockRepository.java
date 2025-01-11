package mk.ukim.finki.filterthreemikroservis.repository;

import mk.ukim.finki.filterthreemikroservis.model.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<StockEntity, Long> {
    Optional<StockEntity> findByCompanyCode(String companyCode);
}
