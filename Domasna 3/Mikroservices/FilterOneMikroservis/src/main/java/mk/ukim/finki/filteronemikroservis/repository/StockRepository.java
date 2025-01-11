package mk.ukim.finki.filteronemikroservis.repository;

import mk.ukim.finki.filteronemikroservis.model.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<StockEntity, Long> {
    Optional<StockEntity> findByCompanyCode(String companyCode);
}
