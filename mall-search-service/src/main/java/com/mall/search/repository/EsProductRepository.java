package com.mall.search.repository;

import com.mall.search.entity.EsProduct;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface EsProductRepository extends ElasticsearchRepository<EsProduct, Long> {

    // Spring Data ES 会自动根据方法名生成查询
    List<EsProduct> findByNameOrDescription(String name, String description);

    List<EsProduct> findByCategoryId(Long categoryId);

    List<EsProduct> findByBrand(String brand);
}