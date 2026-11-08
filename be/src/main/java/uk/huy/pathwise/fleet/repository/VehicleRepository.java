package uk.huy.pathwise.fleet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.huy.pathwise.fleet.model.Vehicle;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByIdAndStoreId(long id, long storeId);

    List<Vehicle> findAllByStoreIdOrderByNameAscIdAsc(long storeId);

    List<Vehicle> findAllByStoreIdAndActiveOrderByNameAscIdAsc(long storeId, boolean active);

    List<Vehicle> findAllByStoreIdAndIdIn(long storeId, Collection<Long> ids);

    boolean existsByStoreIdAndName(long storeId, String name);

    boolean existsByStoreIdAndNameAndIdNot(long storeId, String name, long id);
}
