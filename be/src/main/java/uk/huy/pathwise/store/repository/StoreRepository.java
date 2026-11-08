package uk.huy.pathwise.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.huy.pathwise.store.model.Store;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {}
