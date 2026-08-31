package com.farmtohome.api.category;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, String> {
  List<CategoryEntity> findByActiveTrueOrderByDisplayOrderAsc();
}
